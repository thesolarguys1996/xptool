from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE89_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE89_INTERACTION_SESSION_MACRO_PASS_J_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE89_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase89-interaction-session-macro-pass-j-signoff] FAILED")
        for error in errors:
            print(f"[phase89-interaction-session-macro-pass-j-signoff] ERROR {error}")
        return 1

    phase89_plan_text = _read(PHASE89_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 89 Slice Status" not in phase89_plan_text:
        errors.append("phase89_plan_missing_slice_status")
    if "`89.1` complete." not in phase89_plan_text:
        errors.append("phase89_plan_missing_89_1_complete")
    if "`89.2` complete." not in phase89_plan_text:
        errors.append("phase89_plan_missing_89_2_complete")
    if "`89.3` complete." not in phase89_plan_text:
        errors.append("phase89_plan_missing_89_3_complete")

    if "## Phase 89 (Interaction Session Macro Pass J Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase89_section")

    if "PHASE 89 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase89_started")
    if "PHASE 89 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase89_complete")

    required_tasks = [
        "- [x] Define Phase 89 interaction-session macro pass J signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 87-89.",
        "- [x] Run Phase 89 verification + guard pack and mark `PHASE 89 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase89_line:{task_line}")

    required_migration_sections = [
        "## Phase 87 (Interaction Session Assembly Factory Extraction)",
        "## Phase 88 (Interaction Session Assembly Consolidation J)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 87 extracted interaction-session constructor assembly from direct constructor ownership",
        "- Phase 88 consolidated interaction-session assembly runtime bundle seam through explicit session-key ownership routing",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase89-interaction-session-macro-pass-j-signoff] FAILED")
        for error in errors:
            print(f"[phase89-interaction-session-macro-pass-j-signoff] ERROR {error}")
        return 1

    print(
        "[phase89-interaction-session-macro-pass-j-signoff] OK: interaction session macro pass J signoff Phase 89 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
