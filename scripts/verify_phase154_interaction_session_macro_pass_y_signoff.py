from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE154_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE154_INTERACTION_SESSION_MACRO_PASS_Y_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE154_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase154-interaction-session-macro-pass-y-signoff] FAILED")
        for error in errors:
            print(f"[phase154-interaction-session-macro-pass-y-signoff] ERROR {error}")
        return 1

    phase154_plan_text = _read(PHASE154_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 154 Slice Status" not in phase154_plan_text:
        errors.append("phase154_plan_missing_slice_status")
    if "`154.1` complete." not in phase154_plan_text:
        errors.append("phase154_plan_missing_154_1_complete")
    if "`154.2` complete." not in phase154_plan_text:
        errors.append("phase154_plan_missing_154_2_complete")
    if "`154.3` complete." not in phase154_plan_text:
        errors.append("phase154_plan_missing_154_3_complete")

    if "## Phase 154 (Interaction Session Macro Pass Y Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase154_section")

    if "PHASE 154 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase154_started")
    if "PHASE 154 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase154_complete")

    required_tasks = [
        "- [x] Define Phase 154 interaction-session macro pass Y signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 151-154.",
        "- [x] Run Phase 154 verification + guard pack and mark `PHASE 154 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase154_line:{task_line}")

    required_migration_sections = [
        "## Phase 151 (Interaction Session Factory Runtime Bundle Default Entry Factory Extraction)",
        "## Phase 152 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Entry Routing Extraction)",
        "## Phase 153 (Interaction Session Factory Wiring Consolidation Y)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 151 extracted focused interaction-session runtime-bundle default-entry factory ownership",
        "- Phase 152 extracted typed runtime-bundle default-entry routing through `InteractionSessionFactoryRuntimeBundleDefaultEntryFactory` ownership",
        "- Phase 153 consolidated `InteractionSessionFactory.createFromFactoryInputs(...)` seam through runtime-bundle-factory default-entry runtime-bundle routing ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase154-interaction-session-macro-pass-y-signoff] FAILED")
        for error in errors:
            print(f"[phase154-interaction-session-macro-pass-y-signoff] ERROR {error}")
        return 1

    print("[phase154-interaction-session-macro-pass-y-signoff] OK: interaction session macro pass Y signoff Phase 154 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
