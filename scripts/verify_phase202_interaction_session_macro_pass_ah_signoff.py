from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE202_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE202_INTERACTION_SESSION_MACRO_PASS_AH_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE202_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase202-interaction-session-macro-pass-ah-signoff] FAILED")
        for error in errors:
            print(f"[phase202-interaction-session-macro-pass-ah-signoff] ERROR {error}")
        return 1

    phase202_plan_text = _read(PHASE202_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 202 Slice Status" not in phase202_plan_text:
        errors.append("phase202_plan_missing_slice_status")
    if "`202.1` complete." not in phase202_plan_text:
        errors.append("phase202_plan_missing_202_1_complete")
    if "`202.2` complete." not in phase202_plan_text:
        errors.append("phase202_plan_missing_202_2_complete")
    if "`202.3` complete." not in phase202_plan_text:
        errors.append("phase202_plan_missing_202_3_complete")

    if "## Phase 202 (Interaction Session Macro Pass AH Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase202_section")

    if "PHASE 202 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase202_started")
    if "PHASE 202 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase202_complete")

    required_tasks = [
        "- [x] Define Phase 202 interaction-session macro pass AH signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 197-202.",
        "- [x] Run Phase 202 verification + guard pack and mark `PHASE 202 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase202_line:{task_line}")

    required_migration_sections = [
        "## Phase 197 (Interaction Session Factory Entry Service Inputs Session Factory Extraction)",
        "## Phase 198 (Interaction Session Factory Entry Service Input Typed Routing Extraction)",
        "## Phase 199 (Interaction Session Factory Entry Default Runtime Bundle Factory Inputs Session Factory Extraction)",
        "## Phase 200 (Interaction Session Factory Entry Default Runtime Bundle Factory Input Typed Routing Extraction)",
        "## Phase 201 (Interaction Session Factory Factory Inputs Default Session Factory Extraction)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 197 extracted focused interaction-session factory entry service-inputs session factory ownership",
        "- Phase 198 consolidated `InteractionSessionFactoryEntrySessionFactory.create(...)` seams through focused entry service-input session routing ownership",
        "- Phase 199 extracted focused interaction-session factory entry default-runtime-bundle-factory-inputs session factory ownership",
        "- Phase 200 consolidated `InteractionSessionFactoryEntrySessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` seams through focused entry default-runtime-bundle-factory-input session routing ownership",
        "- Phase 201 extracted focused interaction-session factory factory-inputs default session factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase202-interaction-session-macro-pass-ah-signoff] FAILED")
        for error in errors:
            print(f"[phase202-interaction-session-macro-pass-ah-signoff] ERROR {error}")
        return 1

    print("[phase202-interaction-session-macro-pass-ah-signoff] OK: interaction session macro pass AH signoff Phase 202 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
