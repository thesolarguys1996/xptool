from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE92_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE92_INTERACTION_SESSION_MACRO_PASS_K_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE92_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        JAVA_SURFACE_INVENTORY,
        TASKS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase92-interaction-session-macro-pass-k-signoff] FAILED")
        for error in errors:
            print(f"[phase92-interaction-session-macro-pass-k-signoff] ERROR {error}")
        return 1

    phase92_plan_text = _read(PHASE92_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 92 Slice Status" not in phase92_plan_text:
        errors.append("phase92_plan_missing_slice_status")
    if "`92.1` complete." not in phase92_plan_text:
        errors.append("phase92_plan_missing_92_1_complete")
    if "`92.2` complete." not in phase92_plan_text:
        errors.append("phase92_plan_missing_92_2_complete")
    if "`92.3` complete." not in phase92_plan_text:
        errors.append("phase92_plan_missing_92_3_complete")

    if "## Phase 92 (Interaction Session Macro Pass K Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase92_section")

    if "PHASE 92 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase92_started")
    if "PHASE 92 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase92_complete")

    required_tasks = [
        "- [x] Define Phase 92 interaction-session macro pass K signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 90-92.",
        "- [x] Run Phase 92 verification + guard pack and mark `PHASE 92 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase92_line:{task_line}")

    required_migration_sections = [
        "## Phase 90 (Interaction Session Runtime Bundle Factory Extraction)",
        "## Phase 91 (Interaction Session Assembly Consolidation K)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 90 extracted interaction-session runtime-bundle construction from assembly-factory ownership",
        "- Phase 91 consolidated interaction-session assembly runtime-bundle delegation seam while preserving compatibility sentinel strings",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase92-interaction-session-macro-pass-k-signoff] FAILED")
        for error in errors:
            print(f"[phase92-interaction-session-macro-pass-k-signoff] ERROR {error}")
        return 1

    print(
        "[phase92-interaction-session-macro-pass-k-signoff] OK: interaction session macro pass K signoff Phase 92 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
