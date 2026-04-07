from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE134_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE134_INTERACTION_SESSION_MACRO_PASS_T_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE134_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase134-interaction-session-macro-pass-t-signoff] FAILED")
        for error in errors:
            print(f"[phase134-interaction-session-macro-pass-t-signoff] ERROR {error}")
        return 1

    phase134_plan_text = _read(PHASE134_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 134 Slice Status" not in phase134_plan_text:
        errors.append("phase134_plan_missing_slice_status")
    if "`134.1` complete." not in phase134_plan_text:
        errors.append("phase134_plan_missing_134_1_complete")
    if "`134.2` complete." not in phase134_plan_text:
        errors.append("phase134_plan_missing_134_2_complete")
    if "`134.3` complete." not in phase134_plan_text:
        errors.append("phase134_plan_missing_134_3_complete")

    if "## Phase 134 (Interaction Session Macro Pass T Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase134_section")

    if "PHASE 134 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase134_started")
    if "PHASE 134 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase134_complete")

    required_tasks = [
        "- [x] Define Phase 134 interaction-session macro pass T signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 131-134.",
        "- [x] Run Phase 134 verification + guard pack and mark `PHASE 134 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase134_line:{task_line}")

    required_migration_sections = [
        "## Phase 131 (Interaction Session Factory Runtime Bundle Assembly Inputs Factory Extraction)",
        "## Phase 132 (Interaction Session Factory Runtime Bundle Factory Input Typed Entry Extraction)",
        "## Phase 133 (Interaction Session Factory Wiring Consolidation T)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 131 extracted focused interaction-session runtime-bundle assembly-input factory ownership",
        "- Phase 132 extracted typed-entry runtime-bundle-factory seams through `InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory` ownership",
        "- Phase 133 consolidated public `InteractionSessionFactory.create(...)` seam through typed runtime-bundle-factory input routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase134-interaction-session-macro-pass-t-signoff] FAILED")
        for error in errors:
            print(f"[phase134-interaction-session-macro-pass-t-signoff] ERROR {error}")
        return 1

    print("[phase134-interaction-session-macro-pass-t-signoff] OK: interaction session macro pass T signoff Phase 134 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
