from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
BOOTSTRAP_RUNTIME = PROJECT_ROOT / "scripts/bootstrap-runtime.ps1"
BOOTSTRAP_NATIVE_RUNTIME = PROJECT_ROOT / "scripts/bootstrap-native-runtime.ps1"
CUTOVER_RUNBOOK = PROJECT_ROOT / "docs/NATIVE_CLIENT_CUTOVER_RUNBOOK.md"
PHASE12_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE12_NATIVE_DEFAULT_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        BOOTSTRAP_RUNTIME,
        BOOTSTRAP_NATIVE_RUNTIME,
        CUTOVER_RUNBOOK,
        PHASE12_PLAN,
        PHASE_STATUS,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase12-native-default] FAILED")
        for error in errors:
            print(f"[phase12-native-default] ERROR {error}")
        return 1

    bootstrap_runtime_text = _read(BOOTSTRAP_RUNTIME)
    bootstrap_native_runtime_text = _read(BOOTSTRAP_NATIVE_RUNTIME)
    cutover_runbook_text = _read(CUTOVER_RUNBOOK)
    phase12_plan_text = _read(PHASE12_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)

    if "Initialized runtime paths (native-default):" not in bootstrap_runtime_text:
        errors.append("bootstrap_runtime_missing_native_default_banner")

    if "Test-LoopbackBindAddress" not in bootstrap_native_runtime_text:
        errors.append("bootstrap_native_runtime_missing_loopback_check")
    if "Missing bridge auth token." not in bootstrap_native_runtime_text:
        errors.append("bootstrap_native_runtime_missing_token_required_error")
    if "Bridge bind address must resolve to loopback only." not in bootstrap_native_runtime_text:
        errors.append("bootstrap_native_runtime_missing_loopback_enforcement_error")

    if "native-only defaults" not in cutover_runbook_text:
        errors.append("cutover_runbook_missing_native_only_defaults_language")
    if "bind address is loopback-only by default" not in cutover_runbook_text:
        errors.append("cutover_runbook_missing_loopback_default_note")
    if "auth token is required to start bridge" not in cutover_runbook_text:
        errors.append("cutover_runbook_missing_token_required_note")

    if "PHASE 11 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase11_complete")
    if "PHASE 12 STARTED" not in phase_status_text and "PHASE 12 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase12_started_or_complete")

    if "## Phase 12 Slice Status" not in phase12_plan_text:
        errors.append("phase12_plan_missing_slice_status")
    if "`12.1` complete." not in phase12_plan_text:
        errors.append("phase12_plan_missing_12_1_complete")
    if "`12.2` complete." not in phase12_plan_text:
        errors.append("phase12_plan_missing_12_2_complete")
    if "`12.3` complete." not in phase12_plan_text:
        errors.append("phase12_plan_missing_12_3_complete")

    required_tasks = [
        "- [x] Define Phase 12 native-default cutover scope and completion evidence gates.",
        "- [x] Validate native-default bootstrap/launcher enforcement and operational runbook alignment.",
        "- [x] Run full Phase 12 signoff gate pack and mark `PHASE 12 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase12_line:{task_line}")

    if errors:
        print("[phase12-native-default] FAILED")
        for error in errors:
            print(f"[phase12-native-default] ERROR {error}")
        return 1

    print("[phase12-native-default] OK: native-default Phase 12 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
