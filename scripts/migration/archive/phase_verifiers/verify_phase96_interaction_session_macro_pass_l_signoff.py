from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE96_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE96_INTERACTION_SESSION_MACRO_PASS_L_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE96_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase96-interaction-session-macro-pass-l-signoff] FAILED")
        for error in errors:
            print(f"[phase96-interaction-session-macro-pass-l-signoff] ERROR {error}")
        return 1

    phase96_plan_text = _read(PHASE96_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 96 Slice Status" not in phase96_plan_text:
        errors.append("phase96_plan_missing_slice_status")
    if "`96.1` complete." not in phase96_plan_text:
        errors.append("phase96_plan_missing_96_1_complete")
    if "`96.2` complete." not in phase96_plan_text:
        errors.append("phase96_plan_missing_96_2_complete")
    if "`96.3` complete." not in phase96_plan_text:
        errors.append("phase96_plan_missing_96_3_complete")

    if "## Phase 96 (Interaction Session Macro Pass L Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase96_section")

    if "PHASE 96 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase96_started")
    if "PHASE 96 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase96_complete")

    required_tasks = [
        "- [x] Define Phase 96 interaction-session macro pass L signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 93-96.",
        "- [x] Run Phase 96 verification + guard pack and mark `PHASE 96 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase96_line:{task_line}")

    required_migration_sections = [
        "## Phase 93 (Interaction Session Constructor Runtime Bundle Extraction)",
        "## Phase 94 (Interaction Session Factory Extraction)",
        "## Phase 95 (Interaction Session Wiring Consolidation L)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 93 extracted interaction-session constructor runtime-bundle injection seam",
        "- Phase 94 extracted interaction-session construction boundary into focused `InteractionSessionFactory` ownership",
        "- Phase 95 consolidated executor/session wiring interaction-session seam through focused `InteractionSessionFactory` ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase96-interaction-session-macro-pass-l-signoff] FAILED")
        for error in errors:
            print(f"[phase96-interaction-session-macro-pass-l-signoff] ERROR {error}")
        return 1

    print("[phase96-interaction-session-macro-pass-l-signoff] OK: interaction session macro pass L signoff Phase 96 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
