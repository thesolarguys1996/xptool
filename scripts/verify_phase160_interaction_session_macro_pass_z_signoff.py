from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE160_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE160_INTERACTION_SESSION_MACRO_PASS_Z_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE160_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase160-interaction-session-macro-pass-z-signoff] FAILED")
        for error in errors:
            print(f"[phase160-interaction-session-macro-pass-z-signoff] ERROR {error}")
        return 1

    phase160_plan_text = _read(PHASE160_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 160 Slice Status" not in phase160_plan_text:
        errors.append("phase160_plan_missing_slice_status")
    if "`160.1` complete." not in phase160_plan_text:
        errors.append("phase160_plan_missing_160_1_complete")
    if "`160.2` complete." not in phase160_plan_text:
        errors.append("phase160_plan_missing_160_2_complete")
    if "`160.3` complete." not in phase160_plan_text:
        errors.append("phase160_plan_missing_160_3_complete")

    if "## Phase 160 (Interaction Session Macro Pass Z Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase160_section")

    if "PHASE 160 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase160_started")
    if "PHASE 160 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase160_complete")

    required_tasks = [
        "- [x] Define Phase 160 interaction-session macro pass Z signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 155-160.",
        "- [x] Run Phase 160 verification + guard pack and mark `PHASE 160 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase160_line:{task_line}")

    required_migration_sections = [
        "## Phase 155 (Interaction Session Factory Runtime Bundle Default Factory Input Runtime Bundle Factory Extraction)",
        "## Phase 156 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Runtime Bundle Routing Extraction)",
        "## Phase 157 (Interaction Session Factory Runtime Bundle Factory Wiring Consolidation Z)",
        "## Phase 158 (Interaction Session Factory Default Entry Factory Extraction)",
        "## Phase 159 (Interaction Session Factory Default Entry Wiring Consolidation Z)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 155 extracted focused interaction-session runtime-bundle default-factory-input runtime-bundle factory ownership",
        "- Phase 156 extracted typed runtime-bundle default-factory-input routing through `InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory` ownership",
        "- Phase 157 consolidated `InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(...)` seam through focused default-entry routing ownership",
        "- Phase 158 extracted focused interaction-session factory default-entry factory ownership",
        "- Phase 159 consolidated `InteractionSessionFactory.createFromFactoryInputs(...)` seam through focused `InteractionSessionFactoryDefaultEntryFactory` routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase160-interaction-session-macro-pass-z-signoff] FAILED")
        for error in errors:
            print(f"[phase160-interaction-session-macro-pass-z-signoff] ERROR {error}")
        return 1

    print("[phase160-interaction-session-macro-pass-z-signoff] OK: interaction session macro pass Z signoff Phase 160 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
