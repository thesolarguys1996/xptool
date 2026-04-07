from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE118_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE118_INTERACTION_SESSION_MACRO_PASS_P_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE118_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase118-interaction-session-macro-pass-p-signoff] FAILED")
        for error in errors:
            print(f"[phase118-interaction-session-macro-pass-p-signoff] ERROR {error}")
        return 1

    phase118_plan_text = _read(PHASE118_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 118 Slice Status" not in phase118_plan_text:
        errors.append("phase118_plan_missing_slice_status")
    if "`118.1` complete." not in phase118_plan_text:
        errors.append("phase118_plan_missing_118_1_complete")
    if "`118.2` complete." not in phase118_plan_text:
        errors.append("phase118_plan_missing_118_2_complete")
    if "`118.3` complete." not in phase118_plan_text:
        errors.append("phase118_plan_missing_118_3_complete")

    if "## Phase 118 (Interaction Session Macro Pass P Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase118_section")

    if "PHASE 118 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase118_started")
    if "PHASE 118 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase118_complete")

    required_tasks = [
        "- [x] Define Phase 118 interaction-session macro pass P signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 115-118.",
        "- [x] Run Phase 118 verification + guard pack and mark `PHASE 118 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase118_line:{task_line}")

    required_migration_sections = [
        "## Phase 115 (Interaction Session Factory Inputs Extraction)",
        "## Phase 116 (Interaction Session Factory Typed Entry Extraction)",
        "## Phase 117 (Interaction Session Factory Wiring Consolidation P)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 115 extracted focused interaction-session factory typed inputs ownership",
        "- Phase 116 extracted typed-entry interaction-session construction seam in `InteractionSessionFactory` ownership",
        "- Phase 117 consolidated public `InteractionSessionFactory.create(...)` seam through typed `InteractionSessionFactoryInputs` ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase118-interaction-session-macro-pass-p-signoff] FAILED")
        for error in errors:
            print(f"[phase118-interaction-session-macro-pass-p-signoff] ERROR {error}")
        return 1

    print("[phase118-interaction-session-macro-pass-p-signoff] OK: interaction session macro pass P signoff Phase 118 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
