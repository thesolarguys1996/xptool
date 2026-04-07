from __future__ import annotations

import argparse
import json
import math
import sqlite3
import time
from dataclasses import dataclass
from pathlib import Path
from statistics import mean, median
from typing import Any, Optional, Sequence

from .drop_metrics_tuning import DEFAULT_MOUSE_ANALYTICS_DB_PATH

DEFAULT_MANUAL_LABEL_TOKENS: tuple[str, ...] = ("droptest", "manualdrop")
DEFAULT_AUTOMATION_LABEL_TOKENS: tuple[str, ...] = ("looptest", "loop", "automation", "auto")


@dataclass(frozen=True)
class ComponentSpec:
    key: str
    label: str
    metrics: tuple[str, ...]
    concealment_sensitive_metrics: tuple[str, ...] = ()


@dataclass(frozen=True)
class MetricReference:
    median: float
    iqr: float
    spread: float


@dataclass(frozen=True)
class SessionSnapshot:
    session_id: int
    label: str
    started_at: str
    ended_at: str
    is_manual: bool
    source_metric_count: int
    metrics: dict[str, float]


COMPONENT_SPECS: tuple[ComponentSpec, ...] = (
    ComponentSpec(
        key="drop_click_shape",
        label="Drop Click Shape",
        metrics=("drop_avg_clicks_per_cycle", "drop_cycle_count_per_min"),
        concealment_sensitive_metrics=("drop_avg_clicks_per_cycle",),
    ),
    ComponentSpec(
        key="drop_duration_shape",
        label="Drop Duration Shape",
        metrics=("drop_avg_cycle_duration_ms", "mean_interclick_interval_ms"),
    ),
    ComponentSpec(
        key="click_rhythm",
        label="Click Rhythm",
        metrics=("median_interclick_interval_ms", "interclick_entropy", "interclick_periodicity_score"),
        concealment_sensitive_metrics=("interclick_entropy", "interclick_periodicity_score"),
    ),
    ComponentSpec(
        key="pause_rhythm",
        label="Pause Rhythm",
        metrics=("pause_before_burst_mean_ms", "pause_after_burst_mean_ms", "pause_count_per_min"),
    ),
    ComponentSpec(
        key="movement_coverage",
        label="Movement Coverage",
        metrics=("session_traveled_distance_norm_screen_diag", "quality_move_density_events_per_s", "total_move_count_per_s"),
    ),
    ComponentSpec(
        key="speed_profile",
        label="Speed Profile",
        metrics=(
            "speed_early_mean_px_per_s",
            "speed_mid_mean_px_per_s",
            "speed_late_mean_px_per_s",
            "speed_drift_late_minus_early_px_per_s",
        ),
        concealment_sensitive_metrics=("speed_drift_late_minus_early_px_per_s",),
    ),
    ComponentSpec(
        key="correction_profile",
        label="Correction Profile",
        metrics=(
            "correction_early_mean",
            "correction_mid_mean",
            "correction_late_mean",
            "correction_drift_late_minus_early",
        ),
    ),
    ComponentSpec(
        key="overshoot_profile",
        label="Overshoot Profile",
        metrics=("overshoot_frequency", "overshoot_click_episode_count", "overshoot_aim_return_count"),
        concealment_sensitive_metrics=("overshoot_frequency",),
    ),
    ComponentSpec(
        key="window_discipline",
        label="Window Discipline",
        metrics=("primary_window_event_ratio", "window_switch_rate_per_min", "active_window_unique_count"),
        concealment_sensitive_metrics=("window_switch_rate_per_min",),
    ),
    ComponentSpec(
        key="idle_mix",
        label="Idle Mix",
        metrics=("total_idle_ratio", "burst_count_per_min", "avg_burst_size", "hover_count"),
    ),
    ComponentSpec(
        key="throughput_balance",
        label="Throughput Balance",
        metrics=("total_click_count_per_min", "click_episode_count_per_min", "movement_count"),
    ),
    ComponentSpec(
        key="quality_risk",
        label="Data Quality Risk",
        metrics=(
            "quality_sample_density_events_per_s",
            "quality_tiny_dt_ratio",
            "quality_score_0_100",
            "quality_large_timestamp_gap_flag",
            "quality_abnormal_event_batching_flag",
            "quality_replay_interpolation_risk_flag",
        ),
        concealment_sensitive_metrics=(
            "quality_tiny_dt_ratio",
            "quality_large_timestamp_gap_flag",
            "quality_abnormal_event_batching_flag",
            "quality_replay_interpolation_risk_flag",
        ),
    ),
)


def build_project_scorecard(
    *,
    activity_key: str = "fishing",
    user_key: str = "default_user",
    db_path: Path | str = DEFAULT_MOUSE_ANALYTICS_DB_PATH,
    min_manual_sessions: int = 5,
    min_target_metric_count: int = 20,
    target_session_id: Optional[int] = None,
    manual_label_tokens: Sequence[str] = DEFAULT_MANUAL_LABEL_TOKENS,
    automation_label_tokens: Sequence[str] = DEFAULT_AUTOMATION_LABEL_TOKENS,
) -> dict[str, Any]:
    path = Path(db_path)
    now_ms = int(time.time() * 1000)
    if not path.exists():
        return {
            "status": "db_not_found",
            "generatedAtUnixMillis": now_ms,
            "dbPath": str(path),
            "activityKey": str(activity_key),
            "userKey": str(user_key),
        }

    snapshots = _load_sessions(
        path=path,
        activity_key=activity_key,
        user_key=user_key,
        manual_label_tokens=manual_label_tokens,
    )
    manual_snapshots = [snapshot for snapshot in snapshots if snapshot.is_manual]
    if len(manual_snapshots) < int(min_manual_sessions):
        return {
            "status": "insufficient_manual_data",
            "generatedAtUnixMillis": now_ms,
            "dbPath": str(path),
            "activityKey": str(activity_key),
            "userKey": str(user_key),
            "minimumManualSessions": int(min_manual_sessions),
            "manualSessionCount": len(manual_snapshots),
            "candidateSessionCount": len(snapshots),
            "manualSessionIds": [snapshot.session_id for snapshot in manual_snapshots],
        }

    target_snapshot = _select_target_session(
        snapshots=snapshots,
        target_session_id=target_session_id,
        automation_label_tokens=automation_label_tokens,
        min_target_metric_count=min_target_metric_count,
    )
    if target_snapshot is None:
        return {
            "status": "target_session_not_found",
            "generatedAtUnixMillis": now_ms,
            "dbPath": str(path),
            "activityKey": str(activity_key),
            "userKey": str(user_key),
            "targetSessionId": target_session_id,
            "candidateSessionCount": len(snapshots),
        }
    if target_snapshot.source_metric_count < int(min_target_metric_count):
        return {
            "status": "insufficient_target_metrics",
            "generatedAtUnixMillis": now_ms,
            "dbPath": str(path),
            "activityKey": str(activity_key),
            "userKey": str(user_key),
            "targetSessionId": target_snapshot.session_id,
            "targetLabel": target_snapshot.label,
            "targetMetricCount": target_snapshot.source_metric_count,
            "minimumTargetMetricCount": int(min_target_metric_count),
        }

    references = _build_metric_references(manual_snapshots)
    component_scores = _score_components(target_snapshot, references)
    human_scores = [float(component["humanScore"]) for component in component_scores]
    concealment_scores = [float(component["concealmentScore"]) for component in component_scores]
    overall_scores = [float(component["overallScore"]) for component in component_scores]

    overall_human = _round2(mean(human_scores)) if human_scores else 0.0
    overall_concealment = _round2(mean(concealment_scores)) if concealment_scores else 0.0
    overall_score = _round2((overall_human * 0.55) + (overall_concealment * 0.45))

    top_gaps = sorted(component_scores, key=lambda row: float(row["overallScore"]))[:5]
    return {
        "status": "ok",
        "generatedAtUnixMillis": now_ms,
        "dbPath": str(path),
        "activityKey": str(activity_key),
        "userKey": str(user_key),
        "manualSessionCount": len(manual_snapshots),
        "candidateSessionCount": len(snapshots),
        "targetSession": {
            "id": target_snapshot.session_id,
            "label": target_snapshot.label,
            "startedAt": target_snapshot.started_at,
            "endedAt": target_snapshot.ended_at,
            "isManualLabel": target_snapshot.is_manual,
            "sourceMetricCount": target_snapshot.source_metric_count,
        },
        "scores": {
            "overallHuman": overall_human,
            "overallConcealment": overall_concealment,
            "overallScore": overall_score,
            "componentScores": component_scores,
            "topGaps": [
                {
                    "key": gap["key"],
                    "label": gap["label"],
                    "overallScore": gap["overallScore"],
                }
                for gap in top_gaps
            ],
        },
    }


def _load_sessions(
    *,
    path: Path,
    activity_key: str,
    user_key: str,
    manual_label_tokens: Sequence[str],
) -> list[SessionSnapshot]:
    query_sessions = """
        SELECT
            s.id AS session_id,
            COALESCE(s.label, '') AS label,
            COALESCE(s.started_at, '') AS started_at,
            COALESCE(s.ended_at, '') AS ended_at
        FROM sessions s
        WHERE lower(COALESCE(s.activity_key, '')) = lower(?)
          AND lower(COALESCE(s.user_key, '')) = lower(?)
        ORDER BY s.id DESC
        LIMIT 300
    """

    con: sqlite3.Connection | None = None
    try:
        con = sqlite3.connect(path)
        con.row_factory = sqlite3.Row
        session_rows = list(con.execute(query_sessions, (str(activity_key), str(user_key))))
        if not session_rows:
            return []

        session_ids = [int(row["session_id"]) for row in session_rows]
        metric_map = _load_metric_map(con, session_ids)
        drop_cycle_map = _load_drop_cycle_map(con, session_ids)
    except sqlite3.Error:
        return []
    finally:
        if con is not None:
            con.close()

    snapshots: list[SessionSnapshot] = []
    for row in session_rows:
        session_id = int(row["session_id"])
        label = str(row["label"] or "")
        metrics = dict(metric_map.get(session_id, {}))
        for drop_key, drop_value in drop_cycle_map.get(session_id, {}).items():
            metrics[drop_key] = float(drop_value)
        _derive_metrics(metrics)

        snapshots.append(
            SessionSnapshot(
                session_id=session_id,
                label=label,
                started_at=str(row["started_at"] or ""),
                ended_at=str(row["ended_at"] or ""),
                is_manual=_label_contains_any(label, manual_label_tokens),
                source_metric_count=len(metrics),
                metrics=metrics,
            )
        )
    return snapshots


def _load_metric_map(con: sqlite3.Connection, session_ids: Sequence[int]) -> dict[int, dict[str, float]]:
    out: dict[int, dict[str, float]] = {}
    if not session_ids:
        return out
    placeholders = ",".join("?" for _ in session_ids)
    query = f"""
        SELECT
            sm.session_id AS session_id,
            sm.metric_name AS metric_name,
            sm.metric_value AS metric_value
        FROM session_metrics sm
        WHERE sm.session_id IN ({placeholders})
    """
    for row in con.execute(query, tuple(int(value) for value in session_ids)):
        session_id = int(row["session_id"])
        metric_name = str(row["metric_name"] or "")
        metric_value = _to_float(row["metric_value"])
        if not metric_name:
            continue
        out.setdefault(session_id, {})[metric_name] = metric_value
    return out


def _load_drop_cycle_map(con: sqlite3.Connection, session_ids: Sequence[int]) -> dict[int, dict[str, float]]:
    out: dict[int, dict[str, float]] = {}
    if not session_ids:
        return out
    placeholders = ",".join("?" for _ in session_ids)
    query = f"""
        SELECT
            dc.session_id AS session_id,
            AVG(dc.click_count) AS drop_avg_clicks_per_cycle,
            AVG(dc.duration_ms) AS drop_avg_cycle_duration_ms,
            COUNT(*) AS drop_cycle_count
        FROM drop_cycles dc
        WHERE dc.session_id IN ({placeholders})
          AND dc.click_count > 0
          AND dc.duration_ms > 0
        GROUP BY dc.session_id
    """
    for row in con.execute(query, tuple(int(value) for value in session_ids)):
        session_id = int(row["session_id"])
        out[session_id] = {
            "drop_avg_clicks_per_cycle": _to_float(row["drop_avg_clicks_per_cycle"]),
            "drop_avg_cycle_duration_ms": _to_float(row["drop_avg_cycle_duration_ms"]),
            "drop_cycle_count": _to_float(row["drop_cycle_count"]),
        }
    return out


def _derive_metrics(metrics: dict[str, float]) -> None:
    total_duration_ms = max(0.0, float(metrics.get("total_duration_ms", 0.0)))
    duration_minutes = total_duration_ms / 60_000.0 if total_duration_ms > 0.0 else 0.0
    duration_seconds = total_duration_ms / 1_000.0 if total_duration_ms > 0.0 else 0.0

    total_idle_time_ms = max(0.0, float(metrics.get("total_idle_time_ms", 0.0)))
    pause_count = max(0.0, float(metrics.get("pause_count", 0.0)))
    drop_cycle_count = max(0.0, float(metrics.get("drop_cycle_count", 0.0)))
    total_click_count = max(0.0, float(metrics.get("total_click_count", 0.0)))
    total_move_count = max(0.0, float(metrics.get("total_move_count", 0.0)))
    click_episode_count = max(0.0, float(metrics.get("click_episode_count", 0.0)))

    metrics["total_idle_ratio"] = (total_idle_time_ms / total_duration_ms) if total_duration_ms > 0.0 else 0.0
    metrics["pause_count_per_min"] = (pause_count / duration_minutes) if duration_minutes > 0.0 else 0.0
    metrics["drop_cycle_count_per_min"] = (drop_cycle_count / duration_minutes) if duration_minutes > 0.0 else 0.0
    metrics["total_click_count_per_min"] = (total_click_count / duration_minutes) if duration_minutes > 0.0 else 0.0
    metrics["total_move_count_per_s"] = (total_move_count / duration_seconds) if duration_seconds > 0.0 else 0.0
    metrics["click_episode_count_per_min"] = (
        click_episode_count / duration_minutes if duration_minutes > 0.0 else 0.0
    )


def _select_target_session(
    *,
    snapshots: Sequence[SessionSnapshot],
    target_session_id: Optional[int],
    automation_label_tokens: Sequence[str],
    min_target_metric_count: int,
) -> Optional[SessionSnapshot]:
    if not snapshots:
        return None
    if target_session_id is not None:
        for snapshot in snapshots:
            if snapshot.session_id == int(target_session_id):
                return snapshot
        return None

    automation_matches = [
        snapshot
        for snapshot in snapshots
        if (not snapshot.is_manual) and _label_contains_any(snapshot.label, automation_label_tokens)
    ]
    if automation_matches:
        filtered = [
            snapshot
            for snapshot in automation_matches
            if snapshot.source_metric_count >= int(min_target_metric_count)
        ]
        if filtered:
            return filtered[0]
        return automation_matches[0]

    non_manual = [snapshot for snapshot in snapshots if not snapshot.is_manual]
    if non_manual:
        filtered = [snapshot for snapshot in non_manual if snapshot.source_metric_count >= int(min_target_metric_count)]
        if filtered:
            return filtered[0]
        return non_manual[0]
    return snapshots[0]


def _build_metric_references(manual_snapshots: Sequence[SessionSnapshot]) -> dict[str, MetricReference]:
    values_by_metric: dict[str, list[float]] = {}
    metric_keys: set[str] = set()
    for spec in COMPONENT_SPECS:
        metric_keys.update(spec.metrics)

    for snapshot in manual_snapshots:
        for key in metric_keys:
            value = snapshot.metrics.get(key)
            if value is None or not math.isfinite(float(value)):
                continue
            values_by_metric.setdefault(key, []).append(float(value))

    references: dict[str, MetricReference] = {}
    for key, values in values_by_metric.items():
        if not values:
            continue
        med = float(median(values))
        q1 = _percentile(values, 25.0)
        q3 = _percentile(values, 75.0)
        iqr = max(0.0, float(q3 - q1))
        scale_floor = max(0.05, abs(med) * 0.08)
        spread = max(iqr, scale_floor)
        references[key] = MetricReference(median=med, iqr=iqr, spread=spread)
    return references


def _score_components(
    target_snapshot: SessionSnapshot,
    references: dict[str, MetricReference],
) -> list[dict[str, Any]]:
    component_rows: list[dict[str, Any]] = []

    for spec in COMPONENT_SPECS:
        human_metric_scores: list[float] = []
        concealment_metric_scores: list[float] = []
        metric_breakdown: list[dict[str, float | str]] = []

        for metric_name in spec.metrics:
            candidate = target_snapshot.metrics.get(metric_name)
            reference = references.get(metric_name)
            if candidate is None or reference is None or not math.isfinite(float(candidate)):
                continue

            concealment_sensitive = metric_name in spec.concealment_sensitive_metrics
            human_score, concealment_score, distance = _score_metric(
                value=float(candidate),
                reference=reference,
                concealment_sensitive=concealment_sensitive,
            )
            human_metric_scores.append(human_score)
            concealment_metric_scores.append(concealment_score)
            metric_breakdown.append(
                {
                    "metric": metric_name,
                    "candidateValue": _round4(float(candidate)),
                    "baselineMedian": _round4(reference.median),
                    "baselineSpread": _round4(reference.spread),
                    "distance": _round4(distance),
                }
            )

        coverage = float(len(metric_breakdown)) / float(len(spec.metrics)) if spec.metrics else 1.0
        if human_metric_scores:
            human_score = _clamp_score(mean(human_metric_scores) * coverage)
            concealment_score = _clamp_score(mean(concealment_metric_scores) * coverage)
        else:
            human_score = 0.0
            concealment_score = 0.0
        overall_score = _clamp_score((human_score * 0.55) + (concealment_score * 0.45))

        component_rows.append(
            {
                "key": spec.key,
                "label": spec.label,
                "humanScore": _round2(human_score),
                "concealmentScore": _round2(concealment_score),
                "overallScore": _round2(overall_score),
                "coverage": _round4(coverage),
                "metricCountUsed": len(metric_breakdown),
                "metricCountTotal": len(spec.metrics),
                "metricBreakdown": metric_breakdown,
            }
        )

    return component_rows


def _score_metric(
    *,
    value: float,
    reference: MetricReference,
    concealment_sensitive: bool,
) -> tuple[float, float, float]:
    spread = max(0.0001, float(reference.spread))
    distance = abs(float(value) - float(reference.median)) / spread
    human_score = _clamp_score(100.0 - (22.0 * distance))

    concealment_multiplier = 1.25 if concealment_sensitive else 1.0
    concealment_score = _clamp_score(100.0 - (30.0 * distance * concealment_multiplier))
    return human_score, concealment_score, float(distance)


def _label_contains_any(label: str, tokens: Sequence[str]) -> bool:
    normalized = str(label or "").strip().lower()
    if not normalized:
        return False
    for token in tokens:
        if str(token or "").strip().lower() in normalized:
            return True
    return False


def _to_float(value: Any) -> float:
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return 0.0
    if not math.isfinite(parsed):
        return 0.0
    return parsed


def _clamp_score(value: float) -> float:
    return max(0.0, min(100.0, float(value)))


def _percentile(values: Sequence[float], percentile: float) -> float:
    if not values:
        return 0.0
    if len(values) == 1:
        return float(values[0])
    sorted_values = sorted(float(value) for value in values)
    rank = (len(sorted_values) - 1) * (float(percentile) / 100.0)
    lo = int(rank)
    hi = min(lo + 1, len(sorted_values) - 1)
    frac = rank - lo
    return sorted_values[lo] + ((sorted_values[hi] - sorted_values[lo]) * frac)


def _round2(value: float) -> float:
    return round(float(value), 2)


def _round4(value: float) -> float:
    return round(float(value), 4)


def parse_args(argv: Optional[list[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="xptool-scorecard",
        description="Build the Phase-1 project human/concealment scorecard from SQLite metrics.",
    )
    parser.add_argument(
        "--db-path",
        default=str(DEFAULT_MOUSE_ANALYTICS_DB_PATH),
        help="Path to mouse analytics SQLite DB",
    )
    parser.add_argument("--activity-key", default="fishing", help="Session activity key filter")
    parser.add_argument("--user-key", default="default_user", help="Session user key filter")
    parser.add_argument(
        "--target-session-id",
        type=int,
        default=None,
        help="Optional explicit target session id to score",
    )
    parser.add_argument(
        "--min-manual-sessions",
        type=int,
        default=5,
        help="Minimum manual sessions needed for baseline references",
    )
    parser.add_argument(
        "--min-target-metrics",
        type=int,
        default=20,
        help="Minimum raw session metric count required for target score confidence",
    )
    parser.add_argument(
        "--manual-label-token",
        action="append",
        default=None,
        help="Manual session label token (repeatable). Defaults: droptest, manualdrop",
    )
    parser.add_argument(
        "--automation-label-token",
        action="append",
        default=None,
        help="Automation session label token (repeatable). Defaults: looptest, loop, automation, auto",
    )
    parser.add_argument("--json-out", default="", help="Optional output JSON file path")
    parser.add_argument("--compact", action="store_true", help="Emit compact JSON")
    return parser.parse_args(argv)


def main(argv: Optional[list[str]] = None) -> int:
    args = parse_args(argv)
    manual_tokens = (
        tuple(str(token) for token in args.manual_label_token)
        if args.manual_label_token
        else DEFAULT_MANUAL_LABEL_TOKENS
    )
    automation_tokens = (
        tuple(str(token) for token in args.automation_label_token)
        if args.automation_label_token
        else DEFAULT_AUTOMATION_LABEL_TOKENS
    )

    scorecard = build_project_scorecard(
        activity_key=str(args.activity_key),
        user_key=str(args.user_key),
        db_path=str(args.db_path),
        min_manual_sessions=int(args.min_manual_sessions),
        min_target_metric_count=int(args.min_target_metrics),
        target_session_id=args.target_session_id,
        manual_label_tokens=manual_tokens,
        automation_label_tokens=automation_tokens,
    )

    if args.json_out:
        out_path = Path(str(args.json_out))
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, "w", encoding="utf-8") as handle:
            json.dump(scorecard, handle, indent=2, sort_keys=True)
            handle.write("\n")
    if args.compact:
        print(json.dumps(scorecard, separators=(",", ":")))
    else:
        print(json.dumps(scorecard, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
