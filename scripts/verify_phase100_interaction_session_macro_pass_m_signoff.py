from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE100_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE100_INTERACTION_SESSION_MACRO_PASS_M_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE100_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase100-interaction-session-macro-pass-m-signoff] FAILED")
        for error in errors:
            print(f"[phase100-interaction-session-macro-pass-m-signoff] ERROR {error}")
        return 1

    phase100_plan_text = _read(PHASE100_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 100 Slice Status" not in phase100_plan_text:
        errors.append("phase100_plan_missing_slice_status")
    if "`100.1` complete." not in phase100_plan_text:
        errors.append("phase100_plan_missing_100_1_complete")
    if "`100.2` complete." not in phase100_plan_text:
        errors.append("phase100_plan_missing_100_2_complete")
    if "`100.3` complete." not in phase100_plan_text:
        errors.append("phase100_plan_missing_100_3_complete")

    if "## Phase 100 (Interaction Session Macro Pass M Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase100_section")

    if "PHASE 100 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase100_started")
    if "PHASE 100 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase100_complete")

    required_tasks = [
        "- [x] Define Phase 100 interaction-session macro pass M signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 97-100.",
        "- [x] Run Phase 100 verification + guard pack and mark `PHASE 100 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase100_line:{task_line}")

    required_migration_sections = [
        "## Phase 97 (Interaction Session Runtime Operations Extraction)",
        "## Phase 98 (Interaction Session Runtime Operations Factory Extraction)",
        "## Phase 99 (Interaction Session Wiring Consolidation M)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 97 extracted interaction-session command/click/tick/shutdown delegation",
        "- Phase 98 extracted interaction-session runtime-operations construction boundary into focused `InteractionSessionRuntimeOperationsFactory` ownership",
        "- Phase 99 consolidated interaction-session runtime-bundle wiring seam through focused `InteractionSessionRuntimeOperationsFactory` ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase100-interaction-session-macro-pass-m-signoff] FAILED")
        for error in errors:
            print(f"[phase100-interaction-session-macro-pass-m-signoff] ERROR {error}")
        return 1

    print("[phase100-interaction-session-macro-pass-m-signoff] OK: interaction session macro pass M signoff Phase 100 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
