from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE208_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE208_INTERACTION_SESSION_MACRO_PASS_AI_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE208_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase208-interaction-session-macro-pass-ai-signoff] FAILED")
        for error in errors:
            print(f"[phase208-interaction-session-macro-pass-ai-signoff] ERROR {error}")
        return 1

    phase208_plan_text = _read(PHASE208_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 208 Slice Status" not in phase208_plan_text:
        errors.append("phase208_plan_missing_slice_status")
    if "`208.1` complete." not in phase208_plan_text:
        errors.append("phase208_plan_missing_208_1_complete")
    if "`208.2` complete." not in phase208_plan_text:
        errors.append("phase208_plan_missing_208_2_complete")
    if "`208.3` complete." not in phase208_plan_text:
        errors.append("phase208_plan_missing_208_3_complete")

    if "## Phase 208 (Interaction Session Macro Pass AI Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase208_section")

    if "PHASE 208 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase208_started")
    if "PHASE 208 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase208_complete")

    required_tasks = [
        "- [x] Define Phase 208 interaction-session macro pass AI signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 203-208.",
        "- [x] Run Phase 208 verification + guard pack and mark `PHASE 208 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase208_line:{task_line}")

    required_migration_sections = [
        "## Phase 203 (Interaction Session Factory Assembly Runtime Assembly Session Factory Extraction)",
        "## Phase 204 (Interaction Session Factory Assembly Runtime Assembly Typed Routing Extraction)",
        "## Phase 205 (Interaction Session Factory Assembly Runtime Bundle Factory Inputs Session Factory Extraction)",
        "## Phase 206 (Interaction Session Factory Assembly Runtime Bundle Factory Input Typed Routing Extraction)",
        "## Phase 207 (Interaction Session Factory Runtime Entry Runtime Session Factory Extraction)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 203 extracted focused interaction-session factory assembly-runtime assembly session factory ownership",
        "- Phase 204 consolidated `InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromAssemblyFactoryInputs(...)` seams through focused assembly-runtime assembly session routing ownership",
        "- Phase 205 extracted focused interaction-session factory assembly-runtime bundle-factory-inputs session factory ownership",
        "- Phase 206 consolidated `InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(...)` seams through focused assembly-runtime bundle-factory-input session routing ownership",
        "- Phase 207 extracted focused interaction-session factory runtime-entry runtime session factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase208-interaction-session-macro-pass-ai-signoff] FAILED")
        for error in errors:
            print(f"[phase208-interaction-session-macro-pass-ai-signoff] ERROR {error}")
        return 1

    print("[phase208-interaction-session-macro-pass-ai-signoff] OK: interaction session macro pass AI signoff Phase 208 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
