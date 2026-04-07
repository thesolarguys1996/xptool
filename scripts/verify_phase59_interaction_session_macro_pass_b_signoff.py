from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE59_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE59_INTERACTION_SESSION_MACRO_PASS_B_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE59_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase59-interaction-session-macro-pass-b-signoff] FAILED")
        for error in errors:
            print(f"[phase59-interaction-session-macro-pass-b-signoff] ERROR {error}")
        return 1

    phase59_plan_text = _read(PHASE59_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 59 Slice Status" not in phase59_plan_text:
        errors.append("phase59_plan_missing_slice_status")
    if "`59.1` complete." not in phase59_plan_text:
        errors.append("phase59_plan_missing_59_1_complete")
    if "`59.2` complete." not in phase59_plan_text:
        errors.append("phase59_plan_missing_59_2_complete")
    if "`59.3` complete." not in phase59_plan_text:
        errors.append("phase59_plan_missing_59_3_complete")

    if "## Phase 59 (Interaction Session Macro Pass B Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase59_section")

    if "PHASE 59 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase59_started")
    if "PHASE 59 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase59_complete")

    required_tasks = [
        "- [x] Define Phase 59 interaction-session macro pass B signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 56-59.",
        "- [x] Run Phase 59 verification + guard pack and mark `PHASE 59 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase59_line:{task_line}")

    required_migration_sections = [
        "## Phase 56 (Interaction Session Command-Router Service Factory Extraction)",
        "## Phase 57 (Interaction Session Click-Event Host Factory Extraction)",
        "## Phase 58 (Interaction Session Host-Factory Consolidation B)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 56 extracted command-router service-from-host assembly from monolithic host-factory ownership",
        "- Phase 57 extracted click-event host assembly from monolithic host-factory ownership",
        "- Phase 58 consolidated command-router service and click-event host focused-factory delegations in `InteractionSessionHostFactory`",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase59-interaction-session-macro-pass-b-signoff] FAILED")
        for error in errors:
            print(f"[phase59-interaction-session-macro-pass-b-signoff] ERROR {error}")
        return 1

    print(
        "[phase59-interaction-session-macro-pass-b-signoff] OK: interaction session macro pass B signoff Phase 59 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
