from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE83_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE83_INTERACTION_SESSION_MACRO_PASS_H_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE83_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase83-interaction-session-macro-pass-h-signoff] FAILED")
        for error in errors:
            print(f"[phase83-interaction-session-macro-pass-h-signoff] ERROR {error}")
        return 1

    phase83_plan_text = _read(PHASE83_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 83 Slice Status" not in phase83_plan_text:
        errors.append("phase83_plan_missing_slice_status")
    if "`83.1` complete." not in phase83_plan_text:
        errors.append("phase83_plan_missing_83_1_complete")
    if "`83.2` complete." not in phase83_plan_text:
        errors.append("phase83_plan_missing_83_2_complete")
    if "`83.3` complete." not in phase83_plan_text:
        errors.append("phase83_plan_missing_83_3_complete")

    if "## Phase 83 (Interaction Session Macro Pass H Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase83_section")

    if "PHASE 83 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase83_started")
    if "PHASE 83 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase83_complete")

    required_tasks = [
        "- [x] Define Phase 83 interaction-session macro pass H signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 80-83.",
        "- [x] Run Phase 83 verification + guard pack and mark `PHASE 83 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase83_line:{task_line}")

    required_migration_sections = [
        "## Phase 80 (Interaction Session Post-Click Settle Service Composite Factory Extraction)",
        "## Phase 81 (Interaction Session Command-Router Service Composite Factory Extraction)",
        "## Phase 82 (Interaction Session Host-Factory Consolidation H)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 80 extracted post-click-settle composite service assembly from host-factory ownership",
        "- Phase 81 extracted command-router composite service assembly from host-factory ownership",
        "- Phase 82 consolidated host-factory post-click-settle/command-router composite service delegation seams while preserving compatibility sentinel strings",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase83-interaction-session-macro-pass-h-signoff] FAILED")
        for error in errors:
            print(f"[phase83-interaction-session-macro-pass-h-signoff] ERROR {error}")
        return 1

    print(
        "[phase83-interaction-session-macro-pass-h-signoff] OK: interaction session macro pass H signoff Phase 83 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
