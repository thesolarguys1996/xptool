import json
import tempfile
import unittest

from runelite_planner.kpi_dashboard import compute_kpi_summary, load_audit_events


def _event(event_type: str, captured_ms: int, details: dict | None = None) -> dict:
    return {
        "type": "CONTROL_PLANE_AUDIT",
        "eventType": event_type,
        "capturedAtUnixMillis": captured_ms,
        "details": details or {},
    }


class KpiDashboardTests(unittest.TestCase):
    def test_compute_kpi_summary_basic_metrics(self) -> None:
        events = [
            _event("session_started", 1000),
            _event("policy_refreshed", 1100),
            _event("decision_latency_sample", 1200, {"latencyMs": 10}),
            _event("decision_latency_sample", 1300, {"latencyMs": 20}),
            _event("decision_latency_sample", 1400, {"latencyMs": 30}),
            _event("dispatch_sent", 1500),
            _event("dispatch_rejected_unsupported", 1600),
            _event("policy_refresh_failed", 1700, {"error": "control_plane_response_replay_rejected"}),
            _event("policy_refreshed", 2000),
        ]
        summary = compute_kpi_summary(events)
        kpis = summary["kpis"]
        self.assertEqual(3, kpis["decisionLatencyMs"]["samples"])
        self.assertGreaterEqual(kpis["decisionLatencyMs"]["p95"], 20.0)
        self.assertEqual(1, kpis["commandValidation"]["rejectedUnsupported"])
        self.assertGreater(kpis["commandValidation"]["rejectRate"], 0.0)
        self.assertEqual(1, kpis["security"]["count"])
        self.assertEqual(1, kpis["operations"]["incidentsClosed"])

    def test_load_audit_events_window_filter(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            path = f"{tmpdir}/audit.ndjson"
            rows = [
                _event("decision_latency_sample", 1000, {"latencyMs": 5}),
                _event("decision_latency_sample", 2000, {"latencyMs": 15}),
                {"type": "OTHER", "eventType": "ignored", "capturedAtUnixMillis": 2500},
            ]
            with open(path, "w", encoding="utf-8") as fh:
                for row in rows:
                    fh.write(json.dumps(row) + "\n")
            loaded = load_audit_events(audit_path=path, from_unix_ms=1500, to_unix_ms=3000)
            self.assertEqual(1, len(loaded))
            self.assertEqual("decision_latency_sample", loaded[0]["eventType"])


if __name__ == "__main__":
    unittest.main()
