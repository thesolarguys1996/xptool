import sqlite3
import tempfile
import unittest
from pathlib import Path

from runelite_planner.project_scorecard import build_project_scorecard


BASE_METRICS: dict[str, float] = {
    "mean_interclick_interval_ms": 5450.0,
    "median_interclick_interval_ms": 420.0,
    "interclick_entropy": 0.58,
    "interclick_periodicity_score": 0.21,
    "pause_before_burst_mean_ms": 6200.0,
    "pause_after_burst_mean_ms": 1100.0,
    "pause_count": 52.0,
    "session_traveled_distance_norm_screen_diag": 21.0,
    "quality_move_density_events_per_s": 24.0,
    "total_move_count": 36000.0,
    "speed_early_mean_px_per_s": 1300.0,
    "speed_mid_mean_px_per_s": 1400.0,
    "speed_late_mean_px_per_s": 1260.0,
    "speed_drift_late_minus_early_px_per_s": -40.0,
    "correction_early_mean": 0.02,
    "correction_mid_mean": 0.05,
    "correction_late_mean": 0.03,
    "correction_drift_late_minus_early": 0.01,
    "overshoot_frequency": 0.012,
    "overshoot_click_episode_count": 2.0,
    "overshoot_aim_return_count": 2.0,
    "primary_window_event_ratio": 0.96,
    "window_switch_rate_per_min": 0.11,
    "active_window_unique_count": 2.0,
    "total_idle_time_ms": 1_080_000.0,
    "total_duration_ms": 1_350_000.0,
    "burst_count_per_min": 1.8,
    "avg_burst_size": 5.1,
    "hover_count": 9.0,
    "total_click_count": 260.0,
    "movement_count": 210.0,
    "click_episode_count": 175.0,
    "quality_sample_density_events_per_s": 25.0,
    "quality_tiny_dt_ratio": 0.30,
    "quality_score_0_100": 35.0,
    "quality_large_timestamp_gap_flag": 1.0,
    "quality_abnormal_event_batching_flag": 1.0,
    "quality_replay_interpolation_risk_flag": 1.0,
}


class ProjectScorecardTests(unittest.TestCase):
    def test_insufficient_manual_baseline_returns_status(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            db_path = Path(tmpdir) / "mouse_analytics.db"
            _create_schema(db_path)
            _insert_session(db_path, session_id=1, label="FishingDropTEST1", metric_offset=0.0)
            _insert_session(db_path, session_id=2, label="FishingDropTEST2", metric_offset=1.0)
            _insert_session(db_path, session_id=10, label="FishingLOOPtest1", metric_offset=8.0)

            result = build_project_scorecard(db_path=db_path, min_manual_sessions=3)
            self.assertEqual("insufficient_manual_data", result["status"])
            self.assertEqual(2, result["manualSessionCount"])

    def test_scorecard_shape_and_range(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            db_path = Path(tmpdir) / "mouse_analytics.db"
            _create_schema(db_path)
            for idx in range(1, 7):
                _insert_session(
                    db_path,
                    session_id=idx,
                    label=f"FishingDropTEST{idx}",
                    metric_offset=float(idx),
                )
            _insert_session(db_path, session_id=30, label="FishingLOOPtest5", metric_offset=12.0)

            result = build_project_scorecard(db_path=db_path, min_manual_sessions=5)
            self.assertEqual("ok", result["status"])
            self.assertEqual(12, len(result["scores"]["componentScores"]))
            self.assertEqual(30, result["targetSession"]["id"])
            for key in ("overallHuman", "overallConcealment", "overallScore"):
                value = float(result["scores"][key])
                self.assertGreaterEqual(value, 0.0)
                self.assertLessEqual(value, 100.0)
            for component in result["scores"]["componentScores"]:
                self.assertGreaterEqual(float(component["overallScore"]), 0.0)
                self.assertLessEqual(float(component["overallScore"]), 100.0)

    def test_explicit_target_session_selection(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            db_path = Path(tmpdir) / "mouse_analytics.db"
            _create_schema(db_path)
            for idx in range(1, 7):
                _insert_session(
                    db_path,
                    session_id=idx,
                    label=f"FishingDropTEST{idx}",
                    metric_offset=float(idx),
                )
            _insert_session(db_path, session_id=40, label="FishingLOOPtestA", metric_offset=10.0)
            _insert_session(db_path, session_id=41, label="FishingLOOPtestB", metric_offset=18.0)

            result = build_project_scorecard(
                db_path=db_path,
                min_manual_sessions=5,
                target_session_id=40,
            )
            self.assertEqual("ok", result["status"])
            self.assertEqual(40, result["targetSession"]["id"])


def _create_schema(db_path: Path) -> None:
    con = sqlite3.connect(db_path)
    try:
        con.execute(
            """
            CREATE TABLE sessions (
                id INTEGER PRIMARY KEY,
                started_at TEXT NOT NULL,
                ended_at TEXT,
                label TEXT,
                notes TEXT,
                activity_key TEXT NOT NULL DEFAULT 'general',
                user_key TEXT NOT NULL DEFAULT 'default_user',
                screen_width_px INTEGER,
                screen_height_px INTEGER,
                dpi_scale REAL
            )
            """
        )
        con.execute(
            """
            CREATE TABLE session_metrics (
                id INTEGER PRIMARY KEY,
                session_id INTEGER NOT NULL,
                metric_name TEXT NOT NULL,
                metric_value REAL NOT NULL
            )
            """
        )
        con.execute(
            """
            CREATE TABLE drop_cycles (
                id INTEGER PRIMARY KEY,
                session_id INTEGER NOT NULL,
                start_ts REAL NOT NULL,
                end_ts REAL NOT NULL,
                start_click_event_id INTEGER,
                end_click_event_id INTEGER,
                click_count INTEGER NOT NULL,
                duration_ms REAL NOT NULL,
                mean_interclick_ms REAL NOT NULL DEFAULT 0,
                median_interclick_ms REAL NOT NULL DEFAULT 0,
                inside_target_ratio REAL,
                created_at TEXT NOT NULL
            )
            """
        )
        con.commit()
    finally:
        con.close()


def _insert_session(db_path: Path, *, session_id: int, label: str, metric_offset: float) -> None:
    metrics = dict(BASE_METRICS)
    metrics["mean_interclick_interval_ms"] += metric_offset * 12.0
    metrics["median_interclick_interval_ms"] += metric_offset * 2.0
    metrics["pause_before_burst_mean_ms"] += metric_offset * 75.0
    metrics["pause_after_burst_mean_ms"] += metric_offset * 9.0
    metrics["quality_move_density_events_per_s"] += metric_offset * 0.2
    metrics["quality_sample_density_events_per_s"] += metric_offset * 0.2
    metrics["session_traveled_distance_norm_screen_diag"] += metric_offset * 0.3
    metrics["drop_cycle_hint_clicks"] = 27.0 + (metric_offset * 0.2)
    metrics["drop_cycle_hint_duration"] = 11_900.0 + (metric_offset * 80.0)

    con = sqlite3.connect(db_path)
    try:
        con.execute(
            """
            INSERT INTO sessions (
                id, started_at, ended_at, label, activity_key, user_key,
                screen_width_px, screen_height_px, dpi_scale
            )
            VALUES (?, ?, ?, ?, 'fishing', 'default_user', 2560, 1440, 1.0)
            """,
            (
                int(session_id),
                "2026-03-25T00:00:00+00:00",
                "2026-03-25T00:20:00+00:00",
                str(label),
            ),
        )
        metric_rows = [
            (int(session_id), str(metric_name), float(metric_value))
            for metric_name, metric_value in metrics.items()
            if metric_name not in {"drop_cycle_hint_clicks", "drop_cycle_hint_duration"}
        ]
        con.executemany(
            """
            INSERT INTO session_metrics (session_id, metric_name, metric_value)
            VALUES (?, ?, ?)
            """,
            metric_rows,
        )

        drop_clicks = int(round(float(metrics["drop_cycle_hint_clicks"])))
        drop_duration = float(metrics["drop_cycle_hint_duration"])
        for cycle_index in range(4):
            con.execute(
                """
                INSERT INTO drop_cycles (
                    session_id, start_ts, end_ts, click_count, duration_ms, created_at
                )
                VALUES (?, ?, ?, ?, ?, '2026-03-25T00:00:00+00:00')
                """,
                (
                    int(session_id),
                    float(cycle_index),
                    float(cycle_index) + 12.0,
                    int(drop_clicks + (cycle_index % 2)),
                    float(drop_duration + (cycle_index * 45.0)),
                ),
            )
        con.commit()
    finally:
        con.close()


if __name__ == "__main__":
    unittest.main()
