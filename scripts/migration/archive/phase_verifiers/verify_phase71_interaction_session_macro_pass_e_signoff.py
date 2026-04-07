from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE71_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE71_INTERACTION_SESSION_MACRO_PASS_E_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE71_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase71-interaction-session-macro-pass-e-signoff] FAILED")
        for error in errors:
            print(f"[phase71-interaction-session-macro-pass-e-signoff] ERROR {error}")
        return 1

    phase71_plan_text = _read(PHASE71_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 71 Slice Status" not in phase71_plan_text:
        errors.append("phase71_plan_missing_slice_status")
    if "`71.1` complete." not in phase71_plan_text:
        errors.append("phase71_plan_missing_71_1_complete")
    if "`71.2` complete." not in phase71_plan_text:
        errors.append("phase71_plan_missing_71_2_complete")
    if "`71.3` complete." not in phase71_plan_text:
        errors.append("phase71_plan_missing_71_3_complete")

    if "## Phase 71 (Interaction Session Macro Pass E Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase71_section")

    if "PHASE 71 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase71_started")
    if "PHASE 71 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase71_complete")

    required_tasks = [
        "- [x] Define Phase 71 interaction-session macro pass E signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 68-71.",
        "- [x] Run Phase 71 verification + guard pack and mark `PHASE 71 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase71_line:{task_line}")

    required_migration_sections = [
        "## Phase 68 (Interaction Session Click-Event Delegate-Host Factory Extraction)",
        "## Phase 69 (Interaction Session Shutdown Delegate-Host Factory Extraction)",
        "## Phase 70 (Interaction Session Host-Factory Consolidation E)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 68 extracted click-event delegate-host assembly from host-factory compatibility delegate ownership",
        "- Phase 69 extracted shutdown delegate-host assembly from host-factory compatibility delegate ownership",
        "- Phase 70 consolidated host-factory click-event/shutdown delegate-host delegation seams while preserving compatibility sentinel strings",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase71-interaction-session-macro-pass-e-signoff] FAILED")
        for error in errors:
            print(f"[phase71-interaction-session-macro-pass-e-signoff] ERROR {error}")
        return 1

    print(
        "[phase71-interaction-session-macro-pass-e-signoff] OK: interaction session macro pass E signoff Phase 71 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
