from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE150_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE150_INTERACTION_SESSION_MACRO_PASS_X_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE150_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase150-interaction-session-macro-pass-x-signoff] FAILED")
        for error in errors:
            print(f"[phase150-interaction-session-macro-pass-x-signoff] ERROR {error}")
        return 1

    phase150_plan_text = _read(PHASE150_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 150 Slice Status" not in phase150_plan_text:
        errors.append("phase150_plan_missing_slice_status")
    if "`150.1` complete." not in phase150_plan_text:
        errors.append("phase150_plan_missing_150_1_complete")
    if "`150.2` complete." not in phase150_plan_text:
        errors.append("phase150_plan_missing_150_2_complete")
    if "`150.3` complete." not in phase150_plan_text:
        errors.append("phase150_plan_missing_150_3_complete")

    if "## Phase 150 (Interaction Session Macro Pass X Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase150_section")

    if "PHASE 150 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase150_started")
    if "PHASE 150 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase150_complete")

    required_tasks = [
        "- [x] Define Phase 150 interaction-session macro pass X signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 147-150.",
        "- [x] Run Phase 150 verification + guard pack and mark `PHASE 150 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase150_line:{task_line}")

    required_migration_sections = [
        "## Phase 147 (Interaction Session Factory Runtime Bundle Default Factory Inputs Factory Extraction)",
        "## Phase 148 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Routing Extraction)",
        "## Phase 149 (Interaction Session Factory Wiring Consolidation X)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 147 extracted focused interaction-session runtime-bundle default factory-input construction ownership",
        "- Phase 148 extracted typed runtime-bundle default-entry routing through `InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory` ownership",
        "- Phase 149 consolidated `InteractionSessionFactory.createFromFactoryInputs(...)` seam through runtime-bundle-factory default factory-input routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase150-interaction-session-macro-pass-x-signoff] FAILED")
        for error in errors:
            print(f"[phase150-interaction-session-macro-pass-x-signoff] ERROR {error}")
        return 1

    print("[phase150-interaction-session-macro-pass-x-signoff] OK: interaction session macro pass X signoff Phase 150 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
