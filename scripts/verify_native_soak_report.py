from __future__ import annotations

import argparse
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]

DEFAULT_REQUIRED_CHECKS = [
    "required_files_present",
    "bridge_reason_coverage",
    "woodcutting_parity",
    "activity_parity",
    "state_acquisition_smoke",
    "native_ui_overlay",
    "java_runtime_ownership_guard",
    "java_shim_retirement_guard",
    "native_only_hardening_guard",
    "phase7_status_complete",
    "phase8b_status_complete",
    "phase9_status_present",
    "phase10_status_present",
    "tasks_java_runtime_removal_checked",
    "tasks_java_plugin_shim_removal_checked",
    "tasks_phase10_scope_checked",
    "tasks_phase10_ops_audit_checked",
    "tasks_phase10_incident_runbook_checked",
    "tasks_phase10_signoff_checked",
]


def _parse_utc(iso_utc: str) -> datetime:
    return datetime.strptime(iso_utc, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate native soak report gates.")
    parser.add_argument(
        "--report",
        default="runtime/native-soak/soak-report.json",
        help="Path to soak report JSON.",
    )
    parser.add_argument("--min-iterations", type=int, default=6, help="Required minimum iterations.")
    parser.add_argument("--max-failures", type=int, default=0, help="Allowed failed iterations.")
    parser.add_argument("--max-age-hours", type=int, default=48, help="Maximum report age in hours.")
    parser.add_argument(
        "--required-check",
        action="append",
        dest="required_checks",
        help="Add required cutover check name. Can be repeated.",
    )
    args = parser.parse_args()

    report_path = Path(args.report)
    if not report_path.is_absolute():
        report_path = (PROJECT_ROOT / report_path).resolve()
    if not report_path.exists():
        print(f"[native-soak-verify] ERROR missing_report path={report_path}")
        return 1

    payload = json.loads(report_path.read_text(encoding="utf-8"))
    errors: list[str] = []

    generated_at_raw = payload.get("generatedAtUtc")
    if not isinstance(generated_at_raw, str):
        errors.append("missing_generatedAtUtc")
        generated_at = None
    else:
        generated_at = _parse_utc(generated_at_raw)
        max_age = timedelta(hours=max(0, args.max_age_hours))
        if datetime.now(timezone.utc) - generated_at > max_age:
            errors.append(f"stale_report generatedAtUtc={generated_at_raw} maxAgeHours={args.max_age_hours}")

    iterations_requested = int(payload.get("iterationsRequested", 0))
    if iterations_requested < args.min_iterations:
        errors.append(
            f"iterations_below_min actual={iterations_requested} required={args.min_iterations}"
        )

    failed_iterations = int(payload.get("failedIterations", 0))
    if failed_iterations > args.max_failures:
        errors.append(
            f"failed_iterations_above_limit actual={failed_iterations} max={args.max_failures}"
        )

    iterations = payload.get("iterations", [])
    if not isinstance(iterations, list) or len(iterations) != iterations_requested:
        errors.append(
            f"iteration_payload_mismatch requested={iterations_requested} actual={len(iterations) if isinstance(iterations, list) else -1}"
        )

    required_checks = args.required_checks if args.required_checks else DEFAULT_REQUIRED_CHECKS
    failure_counts = payload.get("checkFailureCounts", {})
    if not isinstance(failure_counts, dict):
        failure_counts = {}
    for check_name in required_checks:
        failed_count = int(failure_counts.get(check_name, 0))
        if failed_count > 0:
            errors.append(f"required_check_failed check={check_name} failedCount={failed_count}")

    if isinstance(iterations, list):
        for row in iterations:
            if not isinstance(row, dict):
                continue
            iteration_index = row.get("iteration")
            check_statuses = row.get("checkStatuses", {})
            if not isinstance(check_statuses, dict):
                errors.append(f"missing_iteration_check_statuses iteration={iteration_index}")
                continue
            for check_name in required_checks:
                if check_name not in check_statuses:
                    errors.append(f"required_check_missing iteration={iteration_index} check={check_name}")

    if errors:
        print("[native-soak-verify] FAILED")
        for error in errors:
            print(f"[native-soak-verify] ERROR {error}")
        print(f"[native-soak-verify] report={report_path}")
        return 1

    print("[native-soak-verify] OK")
    print(
        "[native-soak-verify] summary",
        f"iterations={iterations_requested}",
        f"failedIterations={failed_iterations}",
        f"generatedAtUtc={generated_at_raw}",
    )
    print(f"[native-soak-verify] report={report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
