from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE146_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE146_INTERACTION_SESSION_MACRO_PASS_W_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE146_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase146-interaction-session-macro-pass-w-signoff] FAILED")
        for error in errors:
            print(f"[phase146-interaction-session-macro-pass-w-signoff] ERROR {error}")
        return 1

    phase146_plan_text = _read(PHASE146_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 146 Slice Status" not in phase146_plan_text:
        errors.append("phase146_plan_missing_slice_status")
    if "`146.1` complete." not in phase146_plan_text:
        errors.append("phase146_plan_missing_146_1_complete")
    if "`146.2` complete." not in phase146_plan_text:
        errors.append("phase146_plan_missing_146_2_complete")
    if "`146.3` complete." not in phase146_plan_text:
        errors.append("phase146_plan_missing_146_3_complete")

    if "## Phase 146 (Interaction Session Macro Pass W Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase146_section")

    if "PHASE 146 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase146_started")
    if "PHASE 146 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase146_complete")

    required_tasks = [
        "- [x] Define Phase 146 interaction-session macro pass W signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 143-146.",
        "- [x] Run Phase 146 verification + guard pack and mark `PHASE 146 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase146_line:{task_line}")

    required_migration_sections = [
        "## Phase 143 (Interaction Session Factory Runtime Bundle Default Assembly Inputs Factory Extraction)",
        "## Phase 144 (Interaction Session Factory Runtime Bundle Default Entry Typed Routing Extraction)",
        "## Phase 145 (Interaction Session Factory Wiring Consolidation W)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 143 extracted focused interaction-session runtime-bundle default assembly-input factory ownership",
        "- Phase 144 extracted typed runtime-bundle default-entry routing through `InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory` ownership",
        "- Phase 145 consolidated `InteractionSessionFactory.createFromFactoryInputs(...)` seam through runtime-bundle-factory default assembly-input routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase146-interaction-session-macro-pass-w-signoff] FAILED")
        for error in errors:
            print(f"[phase146-interaction-session-macro-pass-w-signoff] ERROR {error}")
        return 1

    print("[phase146-interaction-session-macro-pass-w-signoff] OK: interaction session macro pass W signoff Phase 146 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
