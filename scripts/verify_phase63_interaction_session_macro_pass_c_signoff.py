from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE63_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE63_INTERACTION_SESSION_MACRO_PASS_C_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE63_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase63-interaction-session-macro-pass-c-signoff] FAILED")
        for error in errors:
            print(f"[phase63-interaction-session-macro-pass-c-signoff] ERROR {error}")
        return 1

    phase63_plan_text = _read(PHASE63_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 63 Slice Status" not in phase63_plan_text:
        errors.append("phase63_plan_missing_slice_status")
    if "`63.1` complete." not in phase63_plan_text:
        errors.append("phase63_plan_missing_63_1_complete")
    if "`63.2` complete." not in phase63_plan_text:
        errors.append("phase63_plan_missing_63_2_complete")
    if "`63.3` complete." not in phase63_plan_text:
        errors.append("phase63_plan_missing_63_3_complete")

    if "## Phase 63 (Interaction Session Macro Pass C Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase63_section")

    if "PHASE 63 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase63_started")
    if "PHASE 63 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase63_complete")

    required_tasks = [
        "- [x] Define Phase 63 interaction-session macro pass C signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 60-63.",
        "- [x] Run Phase 63 verification + guard pack and mark `PHASE 63 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase63_line:{task_line}")

    required_migration_sections = [
        "## Phase 60 (Interaction Session Motor-Ownership Delegate Factory Extraction)",
        "## Phase 61 (Interaction Session Shutdown Delegate Factory Extraction)",
        "## Phase 62 (Interaction Session Host-Factory Consolidation C)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 60 extracted motor-ownership delegate-host assembly from host-factory back-dependency ownership",
        "- Phase 61 extracted shutdown delegate-host assembly from host-factory back-dependency ownership",
        "- Phase 62 consolidated motor-ownership and shutdown delegate seam ownership while preserving host-factory compatibility wrappers",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase63-interaction-session-macro-pass-c-signoff] FAILED")
        for error in errors:
            print(f"[phase63-interaction-session-macro-pass-c-signoff] ERROR {error}")
        return 1

    print(
        "[phase63-interaction-session-macro-pass-c-signoff] OK: interaction session macro pass C signoff Phase 63 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
