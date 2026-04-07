from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE67_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE67_INTERACTION_SESSION_MACRO_PASS_D_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE67_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase67-interaction-session-macro-pass-d-signoff] FAILED")
        for error in errors:
            print(f"[phase67-interaction-session-macro-pass-d-signoff] ERROR {error}")
        return 1

    phase67_plan_text = _read(PHASE67_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 67 Slice Status" not in phase67_plan_text:
        errors.append("phase67_plan_missing_slice_status")
    if "`67.1` complete." not in phase67_plan_text:
        errors.append("phase67_plan_missing_67_1_complete")
    if "`67.2` complete." not in phase67_plan_text:
        errors.append("phase67_plan_missing_67_2_complete")
    if "`67.3` complete." not in phase67_plan_text:
        errors.append("phase67_plan_missing_67_3_complete")

    if "## Phase 67 (Interaction Session Macro Pass D Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase67_section")

    if "PHASE 67 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase67_started")
    if "PHASE 67 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase67_complete")

    required_tasks = [
        "- [x] Define Phase 67 interaction-session macro pass D signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 64-67.",
        "- [x] Run Phase 67 verification + guard pack and mark `PHASE 67 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase67_line:{task_line}")

    required_migration_sections = [
        "## Phase 64 (Interaction Session Registration Service-From-Host Factory Extraction)",
        "## Phase 65 (Interaction Session Ownership Service-From-Host Factory Extraction)",
        "## Phase 66 (Interaction Session Host-Factory Consolidation D)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 64 extracted registration service-from-host assembly from host-factory direct-construction ownership",
        "- Phase 65 extracted ownership service-from-host assembly from host-factory direct-construction ownership",
        "- Phase 66 consolidated host-factory registration/motor-ownership/ownership service-from-host delegation seams while preserving compatibility sentinel strings",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase67-interaction-session-macro-pass-d-signoff] FAILED")
        for error in errors:
            print(f"[phase67-interaction-session-macro-pass-d-signoff] ERROR {error}")
        return 1

    print(
        "[phase67-interaction-session-macro-pass-d-signoff] OK: interaction session macro pass D signoff Phase 67 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
