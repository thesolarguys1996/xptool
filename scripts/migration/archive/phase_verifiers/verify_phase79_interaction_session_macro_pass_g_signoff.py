from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE79_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE79_INTERACTION_SESSION_MACRO_PASS_G_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE79_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase79-interaction-session-macro-pass-g-signoff] FAILED")
        for error in errors:
            print(f"[phase79-interaction-session-macro-pass-g-signoff] ERROR {error}")
        return 1

    phase79_plan_text = _read(PHASE79_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 79 Slice Status" not in phase79_plan_text:
        errors.append("phase79_plan_missing_slice_status")
    if "`79.1` complete." not in phase79_plan_text:
        errors.append("phase79_plan_missing_79_1_complete")
    if "`79.2` complete." not in phase79_plan_text:
        errors.append("phase79_plan_missing_79_2_complete")
    if "`79.3` complete." not in phase79_plan_text:
        errors.append("phase79_plan_missing_79_3_complete")

    if "## Phase 79 (Interaction Session Macro Pass G Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase79_section")

    if "PHASE 79 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase79_started")
    if "PHASE 79 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase79_complete")

    required_tasks = [
        "- [x] Define Phase 79 interaction-session macro pass G signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 76-79.",
        "- [x] Run Phase 79 verification + guard pack and mark `PHASE 79 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase79_line:{task_line}")

    required_migration_sections = [
        "## Phase 76 (Interaction Session Registration Service Composite Factory Extraction)",
        "## Phase 77 (Interaction Session Motor Service Composite Factory Extraction)",
        "## Phase 78 (Interaction Session Host-Factory Consolidation G)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 76 extracted registration composite service assembly from host-factory ownership",
        "- Phase 77 extracted motor composite service assembly from host-factory ownership",
        "- Phase 78 consolidated host-factory registration/motor composite service delegation seams while preserving compatibility sentinel strings",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase79-interaction-session-macro-pass-g-signoff] FAILED")
        for error in errors:
            print(f"[phase79-interaction-session-macro-pass-g-signoff] ERROR {error}")
        return 1

    print(
        "[phase79-interaction-session-macro-pass-g-signoff] OK: interaction session macro pass G signoff Phase 79 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
