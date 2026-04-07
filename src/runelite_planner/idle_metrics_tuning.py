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
_MIN_SAMPLE_DENSITY_EVENTS_PER_S_BY_ACTIVITY: dict[str, float] = {
    "fishing": 12.0,
    "woodcutting": 4.0,
}


@dataclass(frozen=True)
class IdleSessionSample:
    avg_clicks_per_cycle: float
    avg_cycle_duration_ms: float
    movement_count: float
    hover_count: float
    pause_before_burst_mean_ms: float
    pause_after_burst_mean_ms: float
    burst_count_per_min: float
    quality_sample_density_events_per_s: float

    @property
    def movement_share(self) -> float:
        denom = max(1.0, self.movement_count + self.hover_count)
        return max(0.0, min(1.0, float(self.movement_count) / denom))


def resolve_idle_cadence_tuning_payload(
    *,
    activity_key: str = "fishing",
    user_key: str = "default_user",
    db_path: Path | str = DEFAULT_MOUSE_ANALYTICS_DB_PATH,
) -> dict[str, int] | None:
    path = Path(db_path)
    if not path.exists():
        return None
    samples = _load_manual_drop_idle_samples(path=path, activity_key=activity_key, user_key=user_key)
    return derive_idle_cadence_tuning_from_samples(samples, activity_key=activity_key)


def derive_idle_cadence_tuning_from_samples(
    samples: Sequence[IdleSessionSample],
    *,
    activity_key: str = "fishing",
) -> dict[str, int] | None:
    normalized_activity_key = str(activity_key or "").strip().lower()
    min_density = float(
        _MIN_SAMPLE_DENSITY_EVENTS_PER_S_BY_ACTIVITY.get(
            normalized_activity_key,
            _MIN_SAMPLE_DENSITY_EVENTS_PER_S_BY_ACTIVITY["fishing"],
        )
    )
    filtered: list[IdleSessionSample] = []
    for sample in samples:
        clicks = float(sample.avg_clicks_per_cycle)
        duration = float(sample.avg_cycle_duration_ms)
        density = float(sample.quality_sample_density_events_per_s)
        move_hover_total = float(sample.movement_count + sample.hover_count)
        if clicks < 20.0 or clicks > 34.0:
            continue
        if duration < 8_000.0 or duration > 18_000.0:
            continue
        if density < min_density:
            continue
        if move_hover_total < 1.0:
            continue
        filtered.append(sample)
    if len(filtered) < 3:
        return None

    movement_share_med = float(median([s.movement_share for s in filtered]))
    pause_after_med = _median_positive_or_default(
        [float(s.pause_after_burst_mean_ms) for s in filtered],
        default=1_000.0,
    )
    pause_before_med = _median_positive_or_default(
        [float(s.pause_before_burst_mean_ms) for s in filtered],
        default=6_500.0,
    )
    burst_rate_med = _median_positive_or_default(
        [float(s.burst_count_per_min) for s in filtered],
        default=1.7,
    )

    fishing_idle_min = _clamp_int(round((pause_after_med / 350.0) + 1.0), 3, 10)
    fishing_idle_max = _clamp_int(
        fishing_idle_min + round(max(3.0, pause_before_med / 1800.0)),
        fishing_idle_min + 2,
        16,
    )
    fishing_retry_min = _clamp_int(fishing_idle_min - 2, 2, fishing_idle_min)
    fishing_retry_max = _clamp_int(fishing_idle_max - 3, fishing_retry_min + 1, fishing_idle_max)

    db_idle_min = _clamp_int(fishing_idle_min - 1, 2, 8)
    db_idle_max = _clamp_int(fishing_idle_max - 1, db_idle_min + 2, 14)
    db_retry_min = _clamp_int(fishing_retry_min - 1, 1, db_idle_min)
    db_retry_max = _clamp_int(fishing_retry_max - 1, db_retry_min + 1, db_idle_max)

    post_drop_min = _clamp_int(round(pause_after_med / 280.0), 2, 10)
    post_drop_max = _clamp_int(
        post_drop_min + round(max(2.0, pause_before_med / 3200.0)),
        post_drop_min + 1,
        18,
    )
    post_drop_db_min = _clamp_int(post_drop_min - 2, 1, 10)
    post_drop_db_max = _clamp_int(post_drop_max - 2, post_drop_db_min + 1, 14)

    offscreen_margin_min_px = _clamp_int(
        round(14.0 + ((1.0 - movement_share_med) * 20.0)),
        10,
        42,
    )
    offscreen_margin_max_px = _clamp_int(
        offscreen_margin_min_px + round(max(10.0, pause_after_med / 160.0)),
        offscreen_margin_min_px + 6,
        120,
    )
    offscreen_far_min_gap_px = _clamp_int(
        round(max(offscreen_margin_min_px + 20.0, 56.0) + (pause_after_med / 24.0)),
        offscreen_margin_min_px + 12,
        260,
    )
    offscreen_near_max_gap_px = _clamp_int(
        round(offscreen_far_min_gap_px + max(40.0, pause_before_med / 220.0)),
        offscreen_far_min_gap_px + 20,
        420,
    )
    offscreen_far_max_gap_px = _clamp_int(
        round(offscreen_near_max_gap_px + max(70.0, pause_before_med / 140.0)),
        offscreen_near_max_gap_px + 30,
        900,
    )

    profile_drift = _clamp_int(round(18.0 + (movement_share_med * 38.0)), 18, 60)
    profile_hover = _clamp_int(round(4.0 + ((1.0 - movement_share_med) * 30.0)), 4, 28)
    profile_camera = 10
    profile_noop = _clamp_int(100 - profile_drift - profile_hover - profile_camera, 18, 62)
    profile_park_min_actions = _clamp_int(round(2.0 + (pause_before_med / 7000.0)), 2, 6)
    profile_park_chance = _clamp_int(round(18.0 + (burst_rate_med * 8.0)), 15, 55)

    return {
        "fishingIdleMinIntervalTicks": fishing_idle_min,
        "fishingIdleMaxIntervalTicks": fishing_idle_max,
        "fishingIdleRetryMinIntervalTicks": fishing_retry_min,
        "fishingIdleRetryMaxIntervalTicks": fishing_retry_max,
        "fishingDbParityIdleMinIntervalTicks": db_idle_min,
        "fishingDbParityIdleMaxIntervalTicks": db_idle_max,
        "fishingDbParityIdleRetryMinIntervalTicks": db_retry_min,
        "fishingDbParityIdleRetryMaxIntervalTicks": db_retry_max,
        "postDropIdleCooldownMinTicks": post_drop_min,
        "postDropIdleCooldownMaxTicks": post_drop_max,
        "postDropIdleDbParityCooldownMinTicks": post_drop_db_min,
        "postDropIdleDbParityCooldownMaxTicks": post_drop_db_max,
        "offscreenWindowMarginMinPx": offscreen_margin_min_px,
        "offscreenWindowMarginMaxPx": offscreen_margin_max_px,
        "offscreenNearTargetMaxGapPx": offscreen_near_max_gap_px,
        "offscreenFarTargetMinGapPx": offscreen_far_min_gap_px,
        "offscreenFarTargetMaxGapPx": offscreen_far_max_gap_px,
        "profileHoverChancePercent": profile_hover,
        "profileDriftChancePercent": profile_drift,
        "profileCameraChancePercent": profile_camera,
        "profileNoopChancePercent": profile_noop,
        "profileParkAfterBurstMinActions": profile_park_min_actions,
        "profileParkAfterBurstChancePercent": profile_park_chance,
    }


def _load_manual_drop_idle_samples(
    *,
    path: Path,
    activity_key: str,
    user_key: str,
) -> list[IdleSessionSample]:
    query = """
        WITH cycle_agg AS (
            SELECT
                dc.session_id AS session_id,
                AVG(dc.click_count) AS avg_clicks_per_cycle,
                AVG(dc.duration_ms) AS avg_cycle_duration_ms,
                COUNT(*) AS cycle_count
            FROM drop_cycles dc
            WHERE dc.click_count > 0
              AND dc.duration_ms > 0
            GROUP BY dc.session_id
        )
        SELECT
            s.id AS session_id,
            s.label AS label,
            ca.avg_clicks_per_cycle AS avg_clicks_per_cycle,
            ca.avg_cycle_duration_ms AS avg_cycle_duration_ms,
            ca.cycle_count AS cycle_count,
            MAX(CASE WHEN sm.metric_name = 'movement_count' THEN sm.metric_value END) AS movement_count,
            MAX(CASE WHEN sm.metric_name = 'hover_count' THEN sm.metric_value END) AS hover_count,
            MAX(CASE WHEN sm.metric_name = 'pause_before_burst_mean_ms' THEN sm.metric_value END) AS pause_before_burst_mean_ms,
            MAX(CASE WHEN sm.metric_name = 'pause_after_burst_mean_ms' THEN sm.metric_value END) AS pause_after_burst_mean_ms,
            MAX(CASE WHEN sm.metric_name = 'burst_count_per_min' THEN sm.metric_value END) AS burst_count_per_min,
            MAX(CASE WHEN sm.metric_name = 'quality_sample_density_events_per_s' THEN sm.metric_value END) AS quality_sample_density_events_per_s
        FROM sessions s
        JOIN cycle_agg ca ON ca.session_id = s.id
        LEFT JOIN session_metrics sm ON sm.session_id = s.id
        WHERE lower(COALESCE(s.activity_key, '')) = lower(?)
          AND lower(COALESCE(s.user_key, '')) = lower(?)
        GROUP BY
            s.id,
            s.label,
            ca.avg_clicks_per_cycle,
            ca.avg_cycle_duration_ms,
            ca.cycle_count
        ORDER BY s.id DESC
        LIMIT 120
    """
    out: list[IdleSessionSample] = []
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
        cycle_count = _as_int(row, "cycle_count")
        if cycle_count < 2:
            continue
        avg_clicks = _as_float(row, "avg_clicks_per_cycle")
        avg_duration = _as_float(row, "avg_cycle_duration_ms")
        if avg_clicks <= 0.0 or avg_duration <= 0.0:
            continue
        out.append(
            IdleSessionSample(
                avg_clicks_per_cycle=avg_clicks,
                avg_cycle_duration_ms=avg_duration,
                movement_count=_as_float(row, "movement_count"),
                hover_count=_as_float(row, "hover_count"),
                pause_before_burst_mean_ms=_as_float(row, "pause_before_burst_mean_ms"),
                pause_after_burst_mean_ms=_as_float(row, "pause_after_burst_mean_ms"),
                burst_count_per_min=_as_float(row, "burst_count_per_min"),
                quality_sample_density_events_per_s=_as_float(row, "quality_sample_density_events_per_s"),
            )
        )
    return out


def _looks_like_manual_drop_label(label: str, *, activity_key: str = "") -> bool:
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


def _median_positive_or_default(values: Sequence[float], *, default: float) -> float:
    positives = [float(value) for value in values if float(value) > 0.0]
    if not positives:
        return float(default)
    return float(median(positives))


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
