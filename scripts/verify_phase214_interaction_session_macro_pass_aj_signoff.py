from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE214_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE214_INTERACTION_SESSION_MACRO_PASS_AJ_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE214_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase214-interaction-session-macro-pass-aj-signoff] FAILED")
        for error in errors:
            print(f"[phase214-interaction-session-macro-pass-aj-signoff] ERROR {error}")
        return 1

    phase214_plan_text = _read(PHASE214_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 214 Slice Status" not in phase214_plan_text:
        errors.append("phase214_plan_missing_slice_status")
    if "`214.1` complete." not in phase214_plan_text:
        errors.append("phase214_plan_missing_214_1_complete")
    if "`214.2` complete." not in phase214_plan_text:
        errors.append("phase214_plan_missing_214_2_complete")
    if "`214.3` complete." not in phase214_plan_text:
        errors.append("phase214_plan_missing_214_3_complete")

    if "## Phase 214 (Interaction Session Macro Pass AJ Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase214_section")

    if "PHASE 214 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase214_started")
    if "PHASE 214 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase214_complete")

    required_tasks = [
        "- [x] Define Phase 214 interaction-session macro pass AJ signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 209-214.",
        "- [x] Run Phase 214 verification + guard pack and mark `PHASE 214 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase214_line:{task_line}")

    required_migration_sections = [
        "## Phase 209 (Interaction Session Factory Assembly Runtime Entry Assembly Session Factory Extraction)",
        "## Phase 210 (Interaction Session Factory Assembly Runtime Entry Assembly Typed Routing Extraction)",
        "## Phase 211 (Interaction Session Factory Assembly Runtime Entry Bundle Factory Inputs Session Factory Extraction)",
        "## Phase 212 (Interaction Session Factory Assembly Runtime Entry Bundle Factory Input Typed Routing Extraction)",
        "## Phase 213 (Interaction Session Factory Entry Runtime Session Factory Extraction)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 209 extracted focused interaction-session factory assembly-runtime entry assembly session factory ownership",
        "- Phase 210 consolidated `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` seams through focused assembly-runtime entry assembly session routing ownership",
        "- Phase 211 extracted focused interaction-session factory assembly-runtime entry bundle-factory-inputs session factory ownership",
        "- Phase 212 consolidated `InteractionSessionFactory.createFromRuntimeBundleFactoryInputs(...)` seams through focused assembly-runtime entry bundle-factory-input session routing ownership",
        "- Phase 213 extracted focused interaction-session factory entry runtime session factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase214-interaction-session-macro-pass-aj-signoff] FAILED")
        for error in errors:
            print(f"[phase214-interaction-session-macro-pass-aj-signoff] ERROR {error}")
        return 1

    print("[phase214-interaction-session-macro-pass-aj-signoff] OK: interaction session macro pass AJ signoff Phase 214 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
