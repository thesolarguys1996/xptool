from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE142_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE142_INTERACTION_SESSION_MACRO_PASS_V_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE142_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase142-interaction-session-macro-pass-v-signoff] FAILED")
        for error in errors:
            print(f"[phase142-interaction-session-macro-pass-v-signoff] ERROR {error}")
        return 1

    phase142_plan_text = _read(PHASE142_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 142 Slice Status" not in phase142_plan_text:
        errors.append("phase142_plan_missing_slice_status")
    if "`142.1` complete." not in phase142_plan_text:
        errors.append("phase142_plan_missing_142_1_complete")
    if "`142.2` complete." not in phase142_plan_text:
        errors.append("phase142_plan_missing_142_2_complete")
    if "`142.3` complete." not in phase142_plan_text:
        errors.append("phase142_plan_missing_142_3_complete")

    if "## Phase 142 (Interaction Session Macro Pass V Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase142_section")

    if "PHASE 142 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase142_started")
    if "PHASE 142 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase142_complete")

    required_tasks = [
        "- [x] Define Phase 142 interaction-session macro pass V signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 139-142.",
        "- [x] Run Phase 142 verification + guard pack and mark `PHASE 142 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase142_line:{task_line}")

    required_migration_sections = [
        "## Phase 139 (Interaction Session Factory Runtime Bundle Factory Inputs Factory Extraction)",
        "## Phase 140 (Interaction Session Factory Runtime Bundle Factory Input Typed Entry Routing Extraction)",
        "## Phase 141 (Interaction Session Factory Wiring Consolidation V)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 139 extracted focused interaction-session runtime-bundle-factory typed-input construction ownership",
        "- Phase 140 extracted typed runtime-bundle-factory-input entry routing through `InteractionSessionFactoryRuntimeBundleFactoryInputsFactory` ownership",
        "- Phase 141 consolidated `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` seam through runtime-bundle-factory typed input routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase142-interaction-session-macro-pass-v-signoff] FAILED")
        for error in errors:
            print(f"[phase142-interaction-session-macro-pass-v-signoff] ERROR {error}")
        return 1

    print("[phase142-interaction-session-macro-pass-v-signoff] OK: interaction session macro pass V signoff Phase 142 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
