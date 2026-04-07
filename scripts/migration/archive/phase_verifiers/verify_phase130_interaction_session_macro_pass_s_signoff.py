from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE130_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE130_INTERACTION_SESSION_MACRO_PASS_S_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE130_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase130-interaction-session-macro-pass-s-signoff] FAILED")
        for error in errors:
            print(f"[phase130-interaction-session-macro-pass-s-signoff] ERROR {error}")
        return 1

    phase130_plan_text = _read(PHASE130_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 130 Slice Status" not in phase130_plan_text:
        errors.append("phase130_plan_missing_slice_status")
    if "`130.1` complete." not in phase130_plan_text:
        errors.append("phase130_plan_missing_130_1_complete")
    if "`130.2` complete." not in phase130_plan_text:
        errors.append("phase130_plan_missing_130_2_complete")
    if "`130.3` complete." not in phase130_plan_text:
        errors.append("phase130_plan_missing_130_3_complete")

    if "## Phase 130 (Interaction Session Macro Pass S Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase130_section")

    if "PHASE 130 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase130_started")
    if "PHASE 130 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase130_complete")

    required_tasks = [
        "- [x] Define Phase 130 interaction-session macro pass S signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 127-130.",
        "- [x] Run Phase 130 verification + guard pack and mark `PHASE 130 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase130_line:{task_line}")

    required_migration_sections = [
        "## Phase 127 (Interaction Session Factory Runtime Bundle Factory Inputs Extraction)",
        "## Phase 128 (Interaction Session Factory Runtime Bundle Factory Typed Entry Extraction)",
        "## Phase 129 (Interaction Session Factory Wiring Consolidation S)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 127 extracted focused interaction-session factory runtime-bundle-factory typed input ownership",
        "- Phase 128 extracted typed-entry runtime-bundle-factory seams through `InteractionSessionFactoryRuntimeBundleFactoryInputs` ownership",
        "- Phase 129 consolidated public `InteractionSessionFactory.create(...)` seam through typed runtime-bundle-factory input ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase130-interaction-session-macro-pass-s-signoff] FAILED")
        for error in errors:
            print(f"[phase130-interaction-session-macro-pass-s-signoff] ERROR {error}")
        return 1

    print("[phase130-interaction-session-macro-pass-s-signoff] OK: interaction session macro pass S signoff Phase 130 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
