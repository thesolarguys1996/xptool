from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE126_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE126_INTERACTION_SESSION_MACRO_PASS_R_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE126_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase126-interaction-session-macro-pass-r-signoff] FAILED")
        for error in errors:
            print(f"[phase126-interaction-session-macro-pass-r-signoff] ERROR {error}")
        return 1

    phase126_plan_text = _read(PHASE126_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 126 Slice Status" not in phase126_plan_text:
        errors.append("phase126_plan_missing_slice_status")
    if "`126.1` complete." not in phase126_plan_text:
        errors.append("phase126_plan_missing_126_1_complete")
    if "`126.2` complete." not in phase126_plan_text:
        errors.append("phase126_plan_missing_126_2_complete")
    if "`126.3` complete." not in phase126_plan_text:
        errors.append("phase126_plan_missing_126_3_complete")

    if "## Phase 126 (Interaction Session Macro Pass R Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase126_section")

    if "PHASE 126 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase126_started")
    if "PHASE 126 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase126_complete")

    required_tasks = [
        "- [x] Define Phase 126 interaction-session macro pass R signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 123-126.",
        "- [x] Run Phase 126 verification + guard pack and mark `PHASE 126 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase126_line:{task_line}")

    required_migration_sections = [
        "## Phase 123 (Interaction Session Factory Runtime Bundle Key Policy Extraction)",
        "## Phase 124 (Interaction Session Factory Runtime Bundle Default Entry Extraction)",
        "## Phase 125 (Interaction Session Factory Wiring Consolidation R)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 123 extracted focused interaction-session factory runtime-bundle default session-key policy ownership",
        "- Phase 124 extracted typed default runtime-bundle entry seam in `InteractionSessionFactory` ownership",
        "- Phase 125 consolidated public `InteractionSessionFactory.create(...)` seam through typed default runtime-bundle entry ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase126-interaction-session-macro-pass-r-signoff] FAILED")
        for error in errors:
            print(f"[phase126-interaction-session-macro-pass-r-signoff] ERROR {error}")
        return 1

    print("[phase126-interaction-session-macro-pass-r-signoff] OK: interaction session macro pass R signoff Phase 126 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
