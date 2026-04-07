from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE55_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE55_INTERACTION_SESSION_MACRO_PASS_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE55_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase55-interaction-session-macro-pass-signoff] FAILED")
        for error in errors:
            print(f"[phase55-interaction-session-macro-pass-signoff] ERROR {error}")
        return 1

    phase55_plan_text = _read(PHASE55_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 55 Slice Status" not in phase55_plan_text:
        errors.append("phase55_plan_missing_slice_status")
    if "`55.1` complete." not in phase55_plan_text:
        errors.append("phase55_plan_missing_55_1_complete")
    if "`55.2` complete." not in phase55_plan_text:
        errors.append("phase55_plan_missing_55_2_complete")
    if "`55.3` complete." not in phase55_plan_text:
        errors.append("phase55_plan_missing_55_3_complete")

    if "## Phase 55 (Interaction Session Macro Pass A Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase55_section")

    if "PHASE 55 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase55_started")
    if "PHASE 55 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase55_complete")

    required_tasks = [
        "- [x] Define Phase 55 interaction-session macro pass signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 52-55.",
        "- [x] Run Phase 55 verification + guard pack and mark `PHASE 55 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase55_line:{task_line}")

    required_migration_sections = [
        "## Phase 52 (Interaction Session Post-Click Settle Factory Extraction)",
        "## Phase 53 (Interaction Session Ownership Factory Extraction)",
        "## Phase 54 (Interaction Session Host-Factory Focused-Factory Consolidation)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 52 extracted post-click-settle service/host assembly from monolithic host-factory ownership",
        "- Phase 53 extracted ownership host assembly from monolithic host-factory ownership",
        "- Phase 54 consolidated focused factory delegation boundaries in `InteractionSessionHostFactory`",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase55-interaction-session-macro-pass-signoff] FAILED")
        for error in errors:
            print(f"[phase55-interaction-session-macro-pass-signoff] ERROR {error}")
        return 1

    print(
        "[phase55-interaction-session-macro-pass-signoff] OK: interaction session macro pass A signoff Phase 55 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
