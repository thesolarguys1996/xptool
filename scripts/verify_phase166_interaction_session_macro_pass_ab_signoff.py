from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE166_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE166_INTERACTION_SESSION_MACRO_PASS_AB_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE166_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase166-interaction-session-macro-pass-ab-signoff] FAILED")
        for error in errors:
            print(f"[phase166-interaction-session-macro-pass-ab-signoff] ERROR {error}")
        return 1

    phase166_plan_text = _read(PHASE166_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 166 Slice Status" not in phase166_plan_text:
        errors.append("phase166_plan_missing_slice_status")
    if "`166.1` complete." not in phase166_plan_text:
        errors.append("phase166_plan_missing_166_1_complete")
    if "`166.2` complete." not in phase166_plan_text:
        errors.append("phase166_plan_missing_166_2_complete")
    if "`166.3` complete." not in phase166_plan_text:
        errors.append("phase166_plan_missing_166_3_complete")

    if "## Phase 166 (Interaction Session Macro Pass AB Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase166_section")

    if "PHASE 166 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase166_started")
    if "PHASE 166 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase166_complete")

    required_tasks = [
        "- [x] Define Phase 166 interaction-session macro pass AB signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 161-166.",
        "- [x] Run Phase 166 verification + guard pack and mark `PHASE 166 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase166_line:{task_line}")

    required_migration_sections = [
        "## Phase 161 (Interaction Session Factory Default Runtime Bundle Factory Inputs Factory Extraction)",
        "## Phase 162 (Interaction Session Factory Default Runtime Bundle Factory Input Typed Routing Extraction)",
        "## Phase 163 (Interaction Session Factory Default Entry Factory Wiring Consolidation AA)",
        "## Phase 164 (Interaction Session Factory Default Runtime Session Factory Extraction)",
        "## Phase 165 (Interaction Session Factory Default Entry Wiring Consolidation AB)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 161 extracted focused interaction-session factory default runtime-bundle-factory-inputs factory ownership",
        "- Phase 162 extracted typed default runtime-bundle-factory-input routing through `InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory` ownership",
        "- Phase 163 consolidated `InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(...)` seams through focused default runtime-bundle-factory-input ownership",
        "- Phase 164 extracted focused interaction-session factory default runtime session factory ownership",
        "- Phase 165 consolidated `InteractionSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` seam through focused `InteractionSessionFactoryDefaultEntryFactory` routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase166-interaction-session-macro-pass-ab-signoff] FAILED")
        for error in errors:
            print(f"[phase166-interaction-session-macro-pass-ab-signoff] ERROR {error}")
        return 1

    print("[phase166-interaction-session-macro-pass-ab-signoff] OK: interaction session macro pass AB signoff Phase 166 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
