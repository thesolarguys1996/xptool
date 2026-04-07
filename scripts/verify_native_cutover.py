from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
RUNTIME_DIR = PROJECT_ROOT / "runtime" / "native-cutover"
REPORT_PATH = RUNTIME_DIR / "phase7-cutover-report.json"
BRIDGE_TELEMETRY_PATH = RUNTIME_DIR / "bridge-telemetry-phase7.ndjson"
UI_OVERLAY_PATH = RUNTIME_DIR / "native-ui-overlay-phase7.txt"


@dataclass
class CommandResult:
    name: str
    command: list[str]
    return_code: int
    stdout: str
    stderr: str


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _run_command(
    name: str,
    command: list[str],
    *,
    env_overrides: dict[str, str] | None = None,
    expected_codes: set[int] | None = None,
) -> CommandResult:
    if expected_codes is None:
        expected_codes = {0}
    env = os.environ.copy()
    if env_overrides:
        env.update(env_overrides)
    proc = subprocess.run(
        command,
        cwd=PROJECT_ROOT,
        env=env,
        text=True,
        capture_output=True,
    )
    result = CommandResult(
        name=name,
        command=command,
        return_code=proc.returncode,
        stdout=proc.stdout,
        stderr=proc.stderr,
    )
    if proc.returncode not in expected_codes:
        raise RuntimeError(
            f"{name}_unexpected_exit return_code={proc.returncode} expected={sorted(expected_codes)}"
        )
    return result


def _require_paths(paths: list[Path]) -> list[str]:
    errors: list[str] = []
    for path in paths:
        if not path.exists():
            errors.append(f"missing_path:{path}")
    return errors


def _extract_metric(stdout: str, metric_key: str) -> str | None:
    pattern = re.compile(rf"^{re.escape(metric_key)}=(.+)$", flags=re.MULTILINE)
    match = pattern.search(stdout)
    if not match:
        return None
    return match.group(1).strip()


def _parse_bridge_reasons(path: Path) -> set[str]:
    reasons: set[str] = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        raw = line.strip()
        if not raw:
            continue
        payload = json.loads(raw)
        reason = payload.get("reasonCode")
        if isinstance(reason, str):
            reasons.add(reason)
    return reasons


def _append_check(results: list[dict[str, object]], name: str, passed: bool, details: str) -> None:
    results.append(
        {
            "name": name,
            "passed": passed,
            "details": details,
        }
    )


def main() -> int:
    RUNTIME_DIR.mkdir(parents=True, exist_ok=True)
    checks: list[dict[str, object]] = []
    command_results: list[CommandResult] = []
    errors: list[str] = []

    required_files = [
        PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md",
        PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md",
        PROJECT_ROOT / "docs/NATIVE_CLIENT_CUTOVER_RUNBOOK.md",
        PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE9_SHIM_RETIREMENT_PLAN.md",
        PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE10_HARDENING_PLAN.md",
        PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md",
        PROJECT_ROOT / "docs/NATIVE_SOAK_SIGNOFF.md",
        PROJECT_ROOT / "docs/NATIVE_INCIDENT_TRIAGE_RUNBOOK.md",
        PROJECT_ROOT / "scripts/bootstrap-runtime.ps1",
        PROJECT_ROOT / "scripts/bootstrap-native-runtime.ps1",
        PROJECT_ROOT / "scripts/verify_java_runtime_ownership_blocked.py",
        PROJECT_ROOT / "scripts/verify_java_shim_retirement_gates.py",
        PROJECT_ROOT / "scripts/verify_native_only_operations_hardening.py",
        PROJECT_ROOT / "build/native-bridge/Release/xptool_native_bridge.exe",
        PROJECT_ROOT / "build/native-core/Release/xptool_native_core_woodcutting_parity.exe",
        PROJECT_ROOT / "build/native-core/Release/xptool_native_core_activity_parity.exe",
        PROJECT_ROOT / "build/native-core/Release/xptool_native_core_state_acquisition_smoke.exe",
        PROJECT_ROOT / "build/native-ui/Release/xptool_native_ui.exe",
        PROJECT_ROOT / "native-core/parity/woodcutting_baseline_v1.csv",
        PROJECT_ROOT / "native-core/parity/activity_baseline_v1.csv",
    ]
    missing_errors = _require_paths(required_files)
    if missing_errors:
        errors.extend(missing_errors)
        _append_check(checks, "required_files_present", False, "; ".join(missing_errors))
    else:
        _append_check(checks, "required_files_present", True, "all required files found")

    try:
        bridge_result = _run_command(
            "bridge_phase7_ingest",
            [
                str(PROJECT_ROOT / "build/native-bridge/Release/xptool_native_bridge.exe"),
                "--bind-address",
                "127.0.0.1",
                "--port",
                "7611",
                "--command-ingest-path",
                str(PROJECT_ROOT / "runtime/bridge/command-envelope.ndjson"),
                "--telemetry-out-path",
                str(BRIDGE_TELEMETRY_PATH),
                "--enable-verifier",
            ],
            env_overrides={"XPTOOL_NATIVE_BRIDGE_TOKEN": "phase7-cutover-token"},
        )
        command_results.append(bridge_result)
        reasons = _parse_bridge_reasons(BRIDGE_TELEMETRY_PATH)
        has_required_reasons = (
            "accepted" in reasons
            and "invalid_schema" in reasons
            and "unsupported_command_type" in reasons
            and any(reason.startswith("replay_rejected_") for reason in reasons)
        )
        if not has_required_reasons:
            errors.append(f"bridge_reason_coverage_incomplete:{sorted(reasons)}")
        _append_check(
            checks,
            "bridge_reason_coverage",
            has_required_reasons,
            f"observed_reasons={sorted(reasons)}",
        )
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"bridge_phase7_ingest_failed:{exc}")
        _append_check(checks, "bridge_reason_coverage", False, str(exc))

    try:
        woodcutting_parity = _run_command(
            "woodcutting_parity",
            [
                str(PROJECT_ROOT / "build/native-core/Release/xptool_native_core_woodcutting_parity.exe"),
                str(PROJECT_ROOT / "native-core/parity/woodcutting_baseline_v1.csv"),
            ],
        )
        command_results.append(woodcutting_parity)
        parity_passed = "parity_passed" in woodcutting_parity.stdout
        exact_metrics = (
            _extract_metric(woodcutting_parity.stdout, "metric.outcome_match_rate") == "1"
            and _extract_metric(woodcutting_parity.stdout, "metric.dispatch_tick_match_rate") == "1"
            and _extract_metric(woodcutting_parity.stdout, "metric.reason_coverage") == "1"
        )
        ok = parity_passed and exact_metrics
        if not ok:
            errors.append("woodcutting_parity_failed")
        _append_check(checks, "woodcutting_parity", ok, "expected parity_passed + metrics=1")
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"woodcutting_parity_error:{exc}")
        _append_check(checks, "woodcutting_parity", False, str(exc))

    try:
        activity_parity = _run_command(
            "activity_parity",
            [
                str(PROJECT_ROOT / "build/native-core/Release/xptool_native_core_activity_parity.exe"),
                str(PROJECT_ROOT / "native-core/parity/activity_baseline_v1.csv"),
            ],
        )
        command_results.append(activity_parity)
        parity_passed = "parity_passed" in activity_parity.stdout
        expected_markers = [
            "metric.mining.outcome_match_rate=1",
            "metric.mining.dispatch_tick_match_rate=1",
            "metric.mining.reason_coverage=1",
            "metric.fishing.outcome_match_rate=1",
            "metric.fishing.dispatch_tick_match_rate=1",
            "metric.fishing.reason_coverage=1",
            "metric.combat.outcome_match_rate=1",
            "metric.combat.dispatch_tick_match_rate=1",
            "metric.combat.reason_coverage=1",
            "metric.banking.outcome_match_rate=1",
            "metric.banking.dispatch_tick_match_rate=1",
            "metric.banking.reason_coverage=1",
        ]
        ok = parity_passed and all(marker in activity_parity.stdout for marker in expected_markers)
        if not ok:
            errors.append("activity_parity_failed")
        _append_check(checks, "activity_parity", ok, "expected parity_passed + all activity metrics=1")
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"activity_parity_error:{exc}")
        _append_check(checks, "activity_parity", False, str(exc))

    try:
        acquisition_smoke = _run_command(
            "state_acquisition_smoke",
            [
                str(PROJECT_ROOT / "build/native-core/Release/xptool_native_core_state_acquisition_smoke.exe"),
            ],
        )
        command_results.append(acquisition_smoke)
        expected_markers = [
            "acquisition reason=state_acquired",
            "acquisition reason=state_acquired_legacy_alias",
            "acquisition reason=schema_version_unsupported accepted=false",
        ]
        ok = all(marker in acquisition_smoke.stdout for marker in expected_markers)
        if not ok:
            errors.append("state_acquisition_smoke_failed")
        _append_check(checks, "state_acquisition_smoke", ok, "expected acquisition hardening markers")
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"state_acquisition_smoke_error:{exc}")
        _append_check(checks, "state_acquisition_smoke", False, str(exc))

    try:
        ui_overlay = _run_command(
            "native_ui_overlay",
            [
                str(PROJECT_ROOT / "build/native-ui/Release/xptool_native_ui.exe"),
                "--telemetry-path",
                str(BRIDGE_TELEMETRY_PATH),
                "--config-path",
                str(PROJECT_ROOT / "native-ui/config/default_ui_config.cfg"),
                "--tail-lines",
                "40",
                "--write-overlay",
                str(UI_OVERLAY_PATH),
            ],
        )
        command_results.append(ui_overlay)
        overlay_ok = UI_OVERLAY_PATH.exists() and "Native UI Overlay" in _read_text(UI_OVERLAY_PATH)
        if not overlay_ok:
            errors.append("native_ui_overlay_failed")
        _append_check(checks, "native_ui_overlay", overlay_ok, f"overlay_path={UI_OVERLAY_PATH}")
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"native_ui_overlay_error:{exc}")
        _append_check(checks, "native_ui_overlay", False, str(exc))

    try:
        java_runtime_guard = _run_command(
            "java_runtime_ownership_guard",
            [sys.executable, "scripts/verify_java_runtime_ownership_blocked.py"],
        )
        command_results.append(java_runtime_guard)
        _append_check(
            checks,
            "java_runtime_ownership_guard",
            True,
            "java shadow runtime path remains removed",
        )
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"java_runtime_ownership_guard_error:{exc}")
        _append_check(checks, "java_runtime_ownership_guard", False, str(exc))

    try:
        java_shim_retirement_guard = _run_command(
            "java_shim_retirement_guard",
            [sys.executable, "scripts/verify_java_shim_retirement_gates.py"],
        )
        command_results.append(java_shim_retirement_guard)
        _append_check(
            checks,
            "java_shim_retirement_guard",
            True,
            "java shim retirement pre-removal gates are enforced",
        )
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"java_shim_retirement_guard_error:{exc}")
        _append_check(checks, "java_shim_retirement_guard", False, str(exc))

    try:
        native_only_hardening_guard = _run_command(
            "native_only_hardening_guard",
            [sys.executable, "scripts/verify_native_only_operations_hardening.py"],
        )
        command_results.append(native_only_hardening_guard)
        _append_check(
            checks,
            "native_only_hardening_guard",
            True,
            "native-only operations hardening baseline is enforced",
        )
    except Exception as exc:  # pragma: no cover - direct command failure path
        errors.append(f"native_only_hardening_guard_error:{exc}")
        _append_check(checks, "native_only_hardening_guard", False, str(exc))

    phase_status_text = _read_text(PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md")
    phase7_complete = "PHASE 7 COMPLETE" in phase_status_text
    if not phase7_complete:
        errors.append("phase7_status_missing_complete")
    _append_check(checks, "phase7_status_complete", phase7_complete, "docs/NATIVE_CLIENT_PHASE_STATUS.md")
    phase8b_complete = "PHASE 8B COMPLETE" in phase_status_text
    if not phase8b_complete:
        errors.append("phase8b_status_missing_complete")
    _append_check(checks, "phase8b_status_complete", phase8b_complete, "docs/NATIVE_CLIENT_PHASE_STATUS.md")
    phase9_status_present = "PHASE 9 STARTED" in phase_status_text or "PHASE 9 COMPLETE" in phase_status_text
    if not phase9_status_present:
        errors.append("phase9_status_missing_started_or_complete")
    _append_check(checks, "phase9_status_present", phase9_status_present, "docs/NATIVE_CLIENT_PHASE_STATUS.md")
    phase10_status_present = "PHASE 10 STARTED" in phase_status_text or "PHASE 10 COMPLETE" in phase_status_text
    if not phase10_status_present:
        errors.append("phase10_status_missing_started_or_complete")
    _append_check(checks, "phase10_status_present", phase10_status_present, "docs/NATIVE_CLIENT_PHASE_STATUS.md")

    tasks_text = _read_text(PROJECT_ROOT / "TASKS.md")
    task_line = "- [x] Remove Java shadow runtime path entirely (no legacy override) with CI reintroduction guard."
    task_complete = task_line in tasks_text
    if not task_complete:
        errors.append("java_runtime_removal_task_unchecked")
    _append_check(checks, "tasks_java_runtime_removal_checked", task_complete, "TASKS.md native migration checklist")
    task_line = "- [x] Remove `XPToolPlugin` and RuneLite plugin registration shim references."
    task_complete = task_line in tasks_text
    if not task_complete:
        errors.append("java_plugin_shim_removal_task_unchecked")
    _append_check(checks, "tasks_java_plugin_shim_removal_checked", task_complete, "TASKS.md native migration checklist")
    task_line = "- [x] Define Phase 10 native-only operations hardening scope and execution slices."
    task_complete = task_line in tasks_text
    if not task_complete:
        errors.append("phase10_scope_task_unchecked")
    _append_check(checks, "tasks_phase10_scope_checked", task_complete, "TASKS.md native migration checklist")
    task_line = "- [x] Audit docs/scripts for native-only operational consistency and patch stale ownership assumptions."
    task_complete = task_line in tasks_text
    if not task_complete:
        errors.append("phase10_ops_audit_task_unchecked")
    _append_check(checks, "tasks_phase10_ops_audit_checked", task_complete, "TASKS.md native migration checklist")
    task_line = "- [x] Add reliability/incident triage runbook updates and artifact capture guidance."
    task_complete = task_line in tasks_text
    if not task_complete:
        errors.append("phase10_incident_runbook_task_unchecked")
    _append_check(checks, "tasks_phase10_incident_runbook_checked", task_complete, "TASKS.md native migration checklist")
    task_line = "- [x] Complete Phase 10 signoff pack and mark `PHASE 10 COMPLETE`."
    task_complete = task_line in tasks_text
    if not task_complete:
        errors.append("phase10_signoff_task_unchecked")
    _append_check(checks, "tasks_phase10_signoff_checked", task_complete, "TASKS.md native migration checklist")

    report = {
        "generatedAtUtc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "projectRoot": str(PROJECT_ROOT),
        "checks": checks,
        "commands": [
            {
                "name": result.name,
                "command": result.command,
                "returnCode": result.return_code,
                "stdout": result.stdout.strip(),
                "stderr": result.stderr.strip(),
            }
            for result in command_results
        ],
        "passed": not errors,
        "errors": errors,
    }
    REPORT_PATH.write_text(json.dumps(report, indent=2), encoding="utf-8")

    if errors:
        print("[native-cutover] FAILED")
        for error in errors:
            print(f"[native-cutover] ERROR: {error}")
        print(f"[native-cutover] report={REPORT_PATH}")
        return 1

    print("[native-cutover] OK: native cutover checks passed")
    print(f"[native-cutover] report={REPORT_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
