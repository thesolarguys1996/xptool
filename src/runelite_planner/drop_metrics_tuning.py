from __future__ import annotations

import os
import sqlite3
from dataclasses import dataclass
from pathlib import Path
from statistics import median
from typing import Any, Mapping, Sequence

DEFAULT_MOUSE_ANALYTICS_DB_PATH = Path(
    os.getenv(
        "XPT_MOUSE_ANALYTICS_DB_PATH",
        r"C:\Users\ericb\source\repos\mouse-analytics\mouse_analytics.db",
    )
)
_GLOBAL_DROP_LABEL_TOKENS: tuple[str, ...] = ("droptest", "manualdrop")
_ACTIVITY_DROP_LABEL_TOKENS: dict[str, tuple[str, ...]] = {
    "woodcutting": ("woodcuttinglooptest", "woodcutting_loop_test"),
    "fishing": ("fishinglooptest", "fishing_loop_test"),
}


@dataclass(frozen=True)
class DropCycleSample:
    avg_clicks_per_cycle: float
    avg_cycle_duration_ms: float

    @property
    def interclick_ms(self) -> float:
        clicks = max(1.0, float(self.avg_clicks_per_cycle))
        duration = max(1.0, float(self.avg_cycle_duration_ms))
        return duration / clicks


def resolve_drop_cadence_tuning_payload(
    *,
    activity_key: str = "fishing",
    user_key: str = "default_user",
    db_path: Path | str = DEFAULT_MOUSE_ANALYTICS_DB_PATH,
) -> dict[str, int] | None:
    path = Path(db_path)
    if not path.exists():
        return None
    samples = _load_manual_drop_cycle_samples(path=path, activity_key=activity_key, user_key=user_key)
    return derive_drop_cadence_tuning_from_samples(samples)


def derive_drop_cadence_tuning_from_samples(samples: Sequence[DropCycleSample]) -> dict[str, int] | None:
    filtered: list[DropCycleSample] = []
    for sample in samples:
        clicks = float(sample.avg_clicks_per_cycle)
        duration = float(sample.avg_cycle_duration_ms)
        if clicks < 20.0 or clicks > 34.0:
            continue
        if duration < 8_000.0 or duration > 18_000.0:
            continue
        filtered.append(sample)
    if len(filtered) < 3:
        return None

    clicks_med = float(median([s.avg_clicks_per_cycle for s in filtered]))
    duration_med = float(median([s.avg_cycle_duration_ms for s in filtered]))
    interclick_med = max(180.0, float(median([s.interclick_ms for s in filtered])))

    # One dispatch per 600ms tick is baseline; extra throughput comes from second dispatches.
    base_interclick_ms = 600.0
    extra_dispatch_ratio = max(0.0, (base_interclick_ms / interclick_med) - 1.0)
    # Slight speed-up pass: preserve manual cadence shape, but trim pauses a bit.
    second_dispatch_chance = _clamp_int(round((extra_dispatch_ratio * 100.0) + 2.0), 18, 58)

    local_cooldown_min = _clamp_int(round(interclick_med * 0.045), 10, 72)
    local_cooldown_max = _clamp_int(round(interclick_med * 0.145), local_cooldown_min + 8, 160)

    rhythm_pause_min = 0
    rhythm_pause_max = 4
    rhythm_pause_ramp = _clamp_int(round(clicks_med * 0.34), 7, 14)

    session_tick_skip_chance = 0 if interclick_med <= 500.0 else 1
    session_burst_pause_threshold = _clamp_int(round(clicks_med * 0.32), 7, 14)
    session_burst_pause_chance = 4
    session_cooldown_bias_ms = _clamp_int(round((interclick_med - 430.0) * 0.05), -3, 12)

    return {
        "localCooldownMinMs": local_cooldown_min,
        "localCooldownMaxMs": local_cooldown_max,
        "secondDispatchChancePercent": second_dispatch_chance,
        "rhythmPauseChanceMinPercent": rhythm_pause_min,
        "rhythmPauseChanceMaxPercent": rhythm_pause_max,
        "rhythmPauseRampStartDispatches": rhythm_pause_ramp,
        "sessionTickSkipChancePercent": session_tick_skip_chance,
        "sessionBurstPauseThreshold": session_burst_pause_threshold,
        "sessionBurstPauseChancePercent": session_burst_pause_chance,
        "sessionCooldownBiasMs": session_cooldown_bias_ms,
        "targetCycleClicksMedian": _clamp_int(round(clicks_med), 1, 99),
        "targetCycleDurationMsMedian": _clamp_int(round(duration_med), 1_000, 60_000),
    }


def _load_manual_drop_cycle_samples(
    *,
    path: Path,
    activity_key: str,
    user_key: str,
) -> list[DropCycleSample]:
    query = """
        SELECT
            s.id AS session_id,
            s.label AS label,
            AVG(dc.click_count) AS avg_clicks_per_cycle,
            AVG(dc.duration_ms) AS avg_cycle_duration_ms,
            COUNT(*) AS cycle_count
        FROM sessions s
        JOIN drop_cycles dc ON dc.session_id = s.id
        WHERE lower(COALESCE(s.activity_key, '')) = lower(?)
          AND lower(COALESCE(s.user_key, '')) = lower(?)
          AND dc.click_count > 0
          AND dc.duration_ms > 0
        GROUP BY s.id, s.label
        ORDER BY s.id DESC
        LIMIT 80
    """
    out: list[DropCycleSample] = []
    try:
        with sqlite3.connect(path) as con:
            con.row_factory = sqlite3.Row
            rows = list(con.execute(query, (str(activity_key), str(user_key))))
    except sqlite3.Error:
        return out

    for row in rows:
        label = str(row["label"] or "").strip().lower()
        if label and not _looks_like_manual_drop_label(label, activity_key=activity_key):
            continue
        avg_clicks = _as_float(row, "avg_clicks_per_cycle")
        avg_duration = _as_float(row, "avg_cycle_duration_ms")
        cycle_count = _as_int(row, "cycle_count")
        if avg_clicks <= 0.0 or avg_duration <= 0.0 or cycle_count <= 0:
            continue
        out.append(
            DropCycleSample(
                avg_clicks_per_cycle=avg_clicks,
                avg_cycle_duration_ms=avg_duration,
            )
        )
    return out


def _looks_like_manual_drop_label(label: str, *, activity_key: str) -> bool:
    lowered = str(label or "").strip().lower()
    if not lowered:
        return False
    activity_tokens = _ACTIVITY_DROP_LABEL_TOKENS.get(str(activity_key or "").strip().lower(), ())
    for token in activity_tokens:
        if token in lowered:
            return True
    for token in _GLOBAL_DROP_LABEL_TOKENS:
        if token in lowered:
            return True
    return False


def _as_float(row: Mapping[str, Any], key: str) -> float:
    try:
        return float(row[key])
    except (TypeError, ValueError, KeyError):
        return 0.0


def _as_int(row: Mapping[str, Any], key: str) -> int:
    try:
        return int(row[key])
    except (TypeError, ValueError, KeyError):
        return 0


def _clamp_int(value: int, minimum: int, maximum: int) -> int:
    lo = int(min(minimum, maximum))
    hi = int(max(minimum, maximum))
    return max(lo, min(hi, int(value)))
