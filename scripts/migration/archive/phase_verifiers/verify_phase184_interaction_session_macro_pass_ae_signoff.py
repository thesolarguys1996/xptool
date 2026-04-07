from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE184_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE184_INTERACTION_SESSION_MACRO_PASS_AE_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE184_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase184-interaction-session-macro-pass-ae-signoff] FAILED")
        for error in errors:
            print(f"[phase184-interaction-session-macro-pass-ae-signoff] ERROR {error}")
        return 1

    phase184_plan_text = _read(PHASE184_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 184 Slice Status" not in phase184_plan_text:
        errors.append("phase184_plan_missing_slice_status")
    if "`184.1` complete." not in phase184_plan_text:
        errors.append("phase184_plan_missing_184_1_complete")
    if "`184.2` complete." not in phase184_plan_text:
        errors.append("phase184_plan_missing_184_2_complete")
    if "`184.3` complete." not in phase184_plan_text:
        errors.append("phase184_plan_missing_184_3_complete")

    if "## Phase 184 (Interaction Session Macro Pass AE Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase184_section")

    if "PHASE 184 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase184_started")
    if "PHASE 184 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase184_complete")

    required_tasks = [
        "- [x] Define Phase 184 interaction-session macro pass AE signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 179-184.",
        "- [x] Run Phase 184 verification + guard pack and mark `PHASE 184 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase184_line:{task_line}")

    required_migration_sections = [
        "## Phase 179 (Interaction Session Factory Assembly Factory Inputs Session Factory Extraction)",
        "## Phase 180 (Interaction Session Factory Assembly Factory Input Typed Routing Extraction)",
        "## Phase 181 (Interaction Session Factory Runtime Bundle Factory Inputs Session Factory Extraction)",
        "## Phase 182 (Interaction Session Factory Runtime Bundle Factory Input Typed Routing Extraction)",
        "## Phase 183 (Interaction Session Factory Runtime Bundle Session Factory Extraction)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 179 extracted focused interaction-session factory assembly-factory-inputs session factory ownership",
        "- Phase 180 consolidated `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` seams through focused assembly-factory-input session routing ownership",
        "- Phase 181 extracted focused interaction-session factory runtime-bundle-factory-inputs session factory ownership",
        "- Phase 182 consolidated `InteractionSessionFactory.createFromRuntimeBundleFactoryInputs(...)` seams through focused runtime-bundle-factory-input session routing ownership",
        "- Phase 183 extracted focused interaction-session factory runtime-bundle session factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase184-interaction-session-macro-pass-ae-signoff] FAILED")
        for error in errors:
            print(f"[phase184-interaction-session-macro-pass-ae-signoff] ERROR {error}")
        return 1

    print("[phase184-interaction-session-macro-pass-ae-signoff] OK: interaction session macro pass AE signoff Phase 184 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
