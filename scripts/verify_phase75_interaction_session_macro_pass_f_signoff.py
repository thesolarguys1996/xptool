from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE75_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE75_INTERACTION_SESSION_MACRO_PASS_F_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE75_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase75-interaction-session-macro-pass-f-signoff] FAILED")
        for error in errors:
            print(f"[phase75-interaction-session-macro-pass-f-signoff] ERROR {error}")
        return 1

    phase75_plan_text = _read(PHASE75_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 75 Slice Status" not in phase75_plan_text:
        errors.append("phase75_plan_missing_slice_status")
    if "`75.1` complete." not in phase75_plan_text:
        errors.append("phase75_plan_missing_75_1_complete")
    if "`75.2` complete." not in phase75_plan_text:
        errors.append("phase75_plan_missing_75_2_complete")
    if "`75.3` complete." not in phase75_plan_text:
        errors.append("phase75_plan_missing_75_3_complete")

    if "## Phase 75 (Interaction Session Macro Pass F Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase75_section")

    if "PHASE 75 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase75_started")
    if "PHASE 75 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase75_complete")

    required_tasks = [
        "- [x] Define Phase 75 interaction-session macro pass F signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 72-75.",
        "- [x] Run Phase 75 verification + guard pack and mark `PHASE 75 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase75_line:{task_line}")

    required_migration_sections = [
        "## Phase 72 (Interaction Session Click-Event Service Factory Extraction)",
        "## Phase 73 (Interaction Session Shutdown Service Factory Extraction)",
        "## Phase 74 (Interaction Session Host-Factory Consolidation F)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 72 extracted click-event service assembly from host-factory composite ownership",
        "- Phase 73 extracted shutdown service assembly from host-factory composite ownership",
        "- Phase 74 consolidated host-factory click-event/shutdown service delegation seams while preserving compatibility sentinel strings",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase75-interaction-session-macro-pass-f-signoff] FAILED")
        for error in errors:
            print(f"[phase75-interaction-session-macro-pass-f-signoff] ERROR {error}")
        return 1

    print(
        "[phase75-interaction-session-macro-pass-f-signoff] OK: interaction session macro pass F signoff Phase 75 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
