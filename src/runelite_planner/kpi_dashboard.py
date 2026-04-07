from __future__ import annotations

import argparse
import json
from pathlib import Path
from statistics import mean
import time
from typing import Any, Mapping, Optional

from .paths import PROJECT_ROOT, default_control_plane_audit_path


def _as_mapping(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _to_int(value: Any, fallback: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return fallback


def _to_float(value: Any, fallback: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return fallback


def _percentile(values: list[float], p: float) -> float:
    if not values:
        return 0.0
    if len(values) == 1:
        return float(values[0])
    sorted_values = sorted(float(v) for v in values)
    rank = (len(sorted_values) - 1) * (p / 100.0)
    lo = int(rank)
    hi = min(lo + 1, len(sorted_values) - 1)
    frac = rank - lo
    return sorted_values[lo] + ((sorted_values[hi] - sorted_values[lo]) * frac)


def _count_lines(path: Path) -> int:
    if not path.exists():
        return 0
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            return sum(1 for _ in fh)
    except OSError:
        return 0


def load_audit_events(
    *,
    audit_path: str,
    from_unix_ms: Optional[int],
    to_unix_ms: Optional[int],
) -> list[dict[str, Any]]:
    path = Path(audit_path)
    if not path.exists():
        return []
    events: list[dict[str, Any]] = []
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            for raw_line in fh:
                line = raw_line.strip()
                if not line:
                    continue
                try:
                    payload = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if not isinstance(payload, dict):
                    continue
                captured_ms = _to_int(payload.get("capturedAtUnixMillis"), 0)
                if from_unix_ms is not None and captured_ms < from_unix_ms:
                    continue
                if to_unix_ms is not None and captured_ms > to_unix_ms:
                    continue
                if str(payload.get("type", "")) != "CONTROL_PLANE_AUDIT":
                    continue
                events.append(payload)
    except OSError:
        return []
    return events


def _count_security_event(event_type: str, error_text: str) -> bool:
    security_event_types = {
        "session_start_failed",
        "policy_refresh_failed",
        "session_close_failed",
    }
    if event_type in security_event_types:
        normalized = str(error_text or "").lower()
        if ("replay" in normalized) or ("signature" in normalized) or ("auth" in normalized):
            return True
    return False


def compute_kpi_summary(events: list[dict[str, Any]]) -> dict[str, Any]:
    now_ms = int(time.time() * 1000)
    event_types: dict[str, int] = {}
    latency_samples: list[float] = []
    fallback_reasons: dict[str, int] = {}
    security_reasons: dict[str, int] = {}

    dispatch_sent = 0
    dispatch_dry_run = 0
    dispatch_failed = 0
    dispatch_rejected_unsupported = 0

    refresh_ok = 0
    refresh_failed = 0
    session_start_ok = 0
    session_start_failed = 0

    incident_open_at_ms: Optional[int] = None
    incident_durations_ms: list[int] = []
    security_events_total = 0

    for event in events:
        event_type = str(event.get("eventType", "") or "")
        details = _as_mapping(event.get("details"))
        event_types[event_type] = event_types.get(event_type, 0) + 1

        if event_type == "decision_latency_sample":
            latency_ms = _to_float(details.get("latencyMs"), -1.0)
            if latency_ms >= 0.0:
                latency_samples.append(latency_ms)

        if event_type == "dispatch_sent":
            dispatch_sent += 1
        elif event_type == "dispatch_dry_run":
            dispatch_dry_run += 1
        elif event_type == "dispatch_failed":
            dispatch_failed += 1
        elif event_type == "dispatch_rejected_unsupported":
            dispatch_rejected_unsupported += 1

        if event_type == "policy_refreshed":
            refresh_ok += 1
            if incident_open_at_ms is not None:
                ended_at = _to_int(event.get("capturedAtUnixMillis"), now_ms)
                if ended_at >= incident_open_at_ms:
                    incident_durations_ms.append(ended_at - incident_open_at_ms)
                incident_open_at_ms = None
        elif event_type == "policy_refresh_failed":
            refresh_failed += 1
            if incident_open_at_ms is None:
                incident_open_at_ms = _to_int(event.get("capturedAtUnixMillis"), now_ms)

        if event_type == "session_started":
            session_start_ok += 1
        elif event_type == "session_start_failed":
            session_start_failed += 1

        if event_type in {"session_start_failed", "policy_refresh_failed", "session_close_failed"}:
            reason = str(details.get("error", event_type) or event_type)
            fallback_reasons[reason] = fallback_reasons.get(reason, 0) + 1

        error_text = str(details.get("error", "") or "")
        if _count_security_event(event_type, error_text):
            security_events_total += 1
            security_reasons[event_type] = security_reasons.get(event_type, 0) + 1

    fallback_count = session_start_failed + refresh_failed
    fallback_denominator = max(1, session_start_ok + session_start_failed + refresh_ok + refresh_failed)
    fallback_rate = float(fallback_count / fallback_denominator)

    command_validation_total = dispatch_sent + dispatch_dry_run + dispatch_rejected_unsupported
    command_validation_reject_rate = (
        float(dispatch_rejected_unsupported / max(1, command_validation_total))
    )

    mttr_ms_avg = float(mean(incident_durations_ms)) if incident_durations_ms else 0.0
    mttr_ms_p95 = _percentile([float(v) for v in incident_durations_ms], 95.0) if incident_durations_ms else 0.0

    command_executor_path = (
        PROJECT_ROOT
        / "runelite-plugin"
        / "src"
        / "main"
        / "java"
        / "com"
        / "xptool"
        / "executor"
        / "CommandExecutor.java"
    )
    planner_core_path = PROJECT_ROOT / "src" / "runelite_planner" / "runtime_core" / "core.py"

    return {
        "generatedAtUnixMillis": now_ms,
        "eventsTotal": len(events),
        "eventTypeCounts": event_types,
        "kpis": {
            "decisionLatencyMs": {
                "samples": len(latency_samples),
                "p50": _percentile(latency_samples, 50.0),
                "p95": _percentile(latency_samples, 95.0),
                "p99": _percentile(latency_samples, 99.0),
            },
            "fallback": {
                "count": fallback_count,
                "rate": fallback_rate,
                "reasons": fallback_reasons,
            },
            "commandValidation": {
                "totalEvaluated": command_validation_total,
                "rejectedUnsupported": dispatch_rejected_unsupported,
                "rejectRate": command_validation_reject_rate,
            },
            "security": {
                "count": security_events_total,
                "reasons": security_reasons,
            },
            "operations": {
                "incidentsClosed": len(incident_durations_ms),
                "incidentsOpen": 1 if incident_open_at_ms is not None else 0,
                "mttrMsAvg": mttr_ms_avg,
                "mttrMsP95": mttr_ms_p95,
            },
            "clientFootprintProxy": {
                "commandExecutorLoc": _count_lines(command_executor_path),
                "plannerCoreLoc": _count_lines(planner_core_path),
                "commandExecutorPath": str(command_executor_path),
                "plannerCorePath": str(planner_core_path),
            },
        },
    }


def parse_args(argv: Optional[list[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="xptool-kpi",
        description="Generate KPI summary from control-plane audit NDJSON.",
    )
    parser.add_argument(
        "--audit-path",
        default=default_control_plane_audit_path(),
        help="Path to control-plane audit NDJSON file",
    )
    parser.add_argument(
        "--from-unix-ms",
        type=int,
        default=None,
        help="Inclusive lower bound for capturedAtUnixMillis",
    )
    parser.add_argument(
        "--to-unix-ms",
        type=int,
        default=None,
        help="Inclusive upper bound for capturedAtUnixMillis",
    )
    parser.add_argument(
        "--json-out",
        default="",
        help="Optional output JSON file path",
    )
    parser.add_argument(
        "--compact",
        action="store_true",
        help="Emit compact JSON to stdout",
    )
    return parser.parse_args(argv)


def main(argv: Optional[list[str]] = None) -> int:
    args = parse_args(argv)
    events = load_audit_events(
        audit_path=str(args.audit_path),
        from_unix_ms=args.from_unix_ms,
        to_unix_ms=args.to_unix_ms,
    )
    summary = compute_kpi_summary(events)
    if args.json_out:
        out_path = Path(str(args.json_out))
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, "w", encoding="utf-8") as fh:
            json.dump(summary, fh, indent=2, sort_keys=True)
            fh.write("\n")
    if args.compact:
        print(json.dumps(summary, separators=(",", ":")))
    else:
        print(json.dumps(summary, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
