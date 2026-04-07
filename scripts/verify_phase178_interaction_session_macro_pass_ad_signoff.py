from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE178_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE178_INTERACTION_SESSION_MACRO_PASS_AD_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE178_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase178-interaction-session-macro-pass-ad-signoff] FAILED")
        for error in errors:
            print(f"[phase178-interaction-session-macro-pass-ad-signoff] ERROR {error}")
        return 1

    phase178_plan_text = _read(PHASE178_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 178 Slice Status" not in phase178_plan_text:
        errors.append("phase178_plan_missing_slice_status")
    if "`178.1` complete." not in phase178_plan_text:
        errors.append("phase178_plan_missing_178_1_complete")
    if "`178.2` complete." not in phase178_plan_text:
        errors.append("phase178_plan_missing_178_2_complete")
    if "`178.3` complete." not in phase178_plan_text:
        errors.append("phase178_plan_missing_178_3_complete")

    if "## Phase 178 (Interaction Session Macro Pass AD Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase178_section")

    if "PHASE 178 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase178_started")
    if "PHASE 178 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase178_complete")

    required_tasks = [
        "- [x] Define Phase 178 interaction-session macro pass AD signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 173-178.",
        "- [x] Run Phase 178 verification + guard pack and mark `PHASE 178 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase178_line:{task_line}")

    required_migration_sections = [
        "## Phase 173 (Interaction Session Factory Default Entry Runtime Bundle Factory Inputs Factory Extraction)",
        "## Phase 174 (Interaction Session Factory Default Entry Runtime Bundle Factory Input Typed Routing Extraction)",
        "## Phase 175 (Interaction Session Factory Default Entry Wiring Consolidation AD)",
        "## Phase 176 (Interaction Session Factory Default Factory Inputs Session Factory Extraction)",
        "## Phase 177 (Interaction Session Factory Wiring Consolidation AC)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 173 extracted focused interaction-session factory default-entry runtime-bundle-factory-inputs factory ownership",
        "- Phase 174 extracted typed default-entry runtime-bundle-factory-input routing through `InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory` ownership",
        "- Phase 175 consolidated `InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(...)` seams through focused default-entry runtime-bundle-factory-input routing ownership",
        "- Phase 176 extracted focused interaction-session factory default-factory-inputs session factory ownership",
        "- Phase 177 consolidated `InteractionSessionFactory.createFromFactoryInputs(...)` seams through focused default factory-input session routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase178-interaction-session-macro-pass-ad-signoff] FAILED")
        for error in errors:
            print(f"[phase178-interaction-session-macro-pass-ad-signoff] ERROR {error}")
        return 1

    print("[phase178-interaction-session-macro-pass-ad-signoff] OK: interaction session macro pass AD signoff Phase 178 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
