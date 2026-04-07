from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE122_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE122_INTERACTION_SESSION_MACRO_PASS_Q_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE122_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase122-interaction-session-macro-pass-q-signoff] FAILED")
        for error in errors:
            print(f"[phase122-interaction-session-macro-pass-q-signoff] ERROR {error}")
        return 1

    phase122_plan_text = _read(PHASE122_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 122 Slice Status" not in phase122_plan_text:
        errors.append("phase122_plan_missing_slice_status")
    if "`122.1` complete." not in phase122_plan_text:
        errors.append("phase122_plan_missing_122_1_complete")
    if "`122.2` complete." not in phase122_plan_text:
        errors.append("phase122_plan_missing_122_2_complete")
    if "`122.3` complete." not in phase122_plan_text:
        errors.append("phase122_plan_missing_122_3_complete")

    if "## Phase 122 (Interaction Session Macro Pass Q Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase122_section")

    if "PHASE 122 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase122_started")
    if "PHASE 122 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase122_complete")

    required_tasks = [
        "- [x] Define Phase 122 interaction-session macro pass Q signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 119-122.",
        "- [x] Run Phase 122 verification + guard pack and mark `PHASE 122 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase122_line:{task_line}")

    required_migration_sections = [
        "## Phase 119 (Interaction Session Factory Runtime Bundle Factory Extraction)",
        "## Phase 120 (Interaction Session Factory Runtime Bundle Typed Entry Extraction)",
        "## Phase 121 (Interaction Session Factory Wiring Consolidation Q)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 119 extracted focused interaction-session factory runtime-bundle routing ownership",
        "- Phase 120 extracted typed-entry interaction-session runtime-bundle creation seam in `InteractionSessionFactory` ownership",
        "- Phase 121 consolidated public `InteractionSessionFactory.create(...)` seam through typed runtime-bundle factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase122-interaction-session-macro-pass-q-signoff] FAILED")
        for error in errors:
            print(f"[phase122-interaction-session-macro-pass-q-signoff] ERROR {error}")
        return 1

    print("[phase122-interaction-session-macro-pass-q-signoff] OK: interaction session macro pass Q signoff Phase 122 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
