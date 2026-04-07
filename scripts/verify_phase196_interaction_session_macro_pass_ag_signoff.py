from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE196_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE196_INTERACTION_SESSION_MACRO_PASS_AG_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE196_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase196-interaction-session-macro-pass-ag-signoff] FAILED")
        for error in errors:
            print(f"[phase196-interaction-session-macro-pass-ag-signoff] ERROR {error}")
        return 1

    phase196_plan_text = _read(PHASE196_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 196 Slice Status" not in phase196_plan_text:
        errors.append("phase196_plan_missing_slice_status")
    if "`196.1` complete." not in phase196_plan_text:
        errors.append("phase196_plan_missing_196_1_complete")
    if "`196.2` complete." not in phase196_plan_text:
        errors.append("phase196_plan_missing_196_2_complete")
    if "`196.3` complete." not in phase196_plan_text:
        errors.append("phase196_plan_missing_196_3_complete")

    if "## Phase 196 (Interaction Session Macro Pass AG Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase196_section")

    if "PHASE 196 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase196_started")
    if "PHASE 196 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase196_complete")

    required_tasks = [
        "- [x] Define Phase 196 interaction-session macro pass AG signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 191-196.",
        "- [x] Run Phase 196 verification + guard pack and mark `PHASE 196 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase196_line:{task_line}")

    required_migration_sections = [
        "## Phase 191 (Interaction Session Factory Factory Inputs Session Factory Extraction)",
        "## Phase 192 (Interaction Session Factory Factory Input Typed Routing Extraction)",
        "## Phase 193 (Interaction Session Factory Assembly Runtime Session Factory Extraction)",
        "## Phase 194 (Interaction Session Factory Assembly Runtime Typed Routing Extraction)",
        "## Phase 195 (Interaction Session Factory Runtime Entry Session Factory Extraction)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 191 extracted focused interaction-session factory factory-inputs session factory ownership",
        "- Phase 192 consolidated `InteractionSessionFactory.createFromFactoryInputs(...)` seams through focused factory-input session routing ownership",
        "- Phase 193 extracted focused interaction-session factory assembly-runtime session factory ownership",
        "- Phase 194 consolidated `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` and `createFromRuntimeBundleFactoryInputs(...)` seams through focused assembly/runtime session routing ownership",
        "- Phase 195 extracted focused interaction-session factory runtime-entry session factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase196-interaction-session-macro-pass-ag-signoff] FAILED")
        for error in errors:
            print(f"[phase196-interaction-session-macro-pass-ag-signoff] ERROR {error}")
        return 1

    print("[phase196-interaction-session-macro-pass-ag-signoff] OK: interaction session macro pass AG signoff Phase 196 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
