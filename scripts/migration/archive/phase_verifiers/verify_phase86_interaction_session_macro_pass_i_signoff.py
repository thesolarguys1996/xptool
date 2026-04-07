from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE86_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE86_INTERACTION_SESSION_MACRO_PASS_I_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE86_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase86-interaction-session-macro-pass-i-signoff] FAILED")
        for error in errors:
            print(f"[phase86-interaction-session-macro-pass-i-signoff] ERROR {error}")
        return 1

    phase86_plan_text = _read(PHASE86_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 86 Slice Status" not in phase86_plan_text:
        errors.append("phase86_plan_missing_slice_status")
    if "`86.1` complete." not in phase86_plan_text:
        errors.append("phase86_plan_missing_86_1_complete")
    if "`86.2` complete." not in phase86_plan_text:
        errors.append("phase86_plan_missing_86_2_complete")
    if "`86.3` complete." not in phase86_plan_text:
        errors.append("phase86_plan_missing_86_3_complete")

    if "## Phase 86 (Interaction Session Macro Pass I Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase86_section")

    if "PHASE 86 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase86_started")
    if "PHASE 86 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase86_complete")

    required_tasks = [
        "- [x] Define Phase 86 interaction-session macro pass I signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 84-86.",
        "- [x] Run Phase 86 verification + guard pack and mark `PHASE 86 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase86_line:{task_line}")

    required_migration_sections = [
        "## Phase 84 (Interaction Session Ownership Service Composite Factory Extraction)",
        "## Phase 85 (Interaction Session Host-Factory Consolidation I)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 84 extracted ownership composite service assembly from host-factory ownership",
        "- Phase 85 consolidated host-factory ownership composite service delegation seam while preserving compatibility sentinel strings",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase86-interaction-session-macro-pass-i-signoff] FAILED")
        for error in errors:
            print(f"[phase86-interaction-session-macro-pass-i-signoff] ERROR {error}")
        return 1

    print(
        "[phase86-interaction-session-macro-pass-i-signoff] OK: interaction session macro pass I signoff Phase 86 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
