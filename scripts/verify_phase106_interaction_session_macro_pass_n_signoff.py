from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE106_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE106_INTERACTION_SESSION_MACRO_PASS_N_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE106_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase106-interaction-session-macro-pass-n-signoff] FAILED")
        for error in errors:
            print(f"[phase106-interaction-session-macro-pass-n-signoff] ERROR {error}")
        return 1

    phase106_plan_text = _read(PHASE106_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 106 Slice Status" not in phase106_plan_text:
        errors.append("phase106_plan_missing_slice_status")
    if "`106.1` complete." not in phase106_plan_text:
        errors.append("phase106_plan_missing_106_1_complete")
    if "`106.2` complete." not in phase106_plan_text:
        errors.append("phase106_plan_missing_106_2_complete")
    if "`106.3` complete." not in phase106_plan_text:
        errors.append("phase106_plan_missing_106_3_complete")

    if "## Phase 106 (Interaction Session Macro Pass N Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase106_section")

    if "PHASE 106 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase106_started")
    if "PHASE 106 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase106_complete")

    required_tasks = [
        "- [x] Define Phase 106 interaction-session macro pass N signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 103-106.",
        "- [x] Run Phase 106 verification + guard pack and mark `PHASE 106 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase106_line:{task_line}")

    required_migration_sections = [
        "## Phase 103 (Interaction Session Runtime Bundle Factory Inputs Extraction)",
        "## Phase 104 (Interaction Session Runtime Bundle Factory Typed Entry Extraction)",
        "## Phase 105 (Interaction Session Assembly Wiring Consolidation N)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 103 extracted focused runtime-bundle-factory typed inputs ownership",
        "- Phase 104 extracted typed-entry runtime-bundle construction seam in `InteractionSessionRuntimeBundleFactory` ownership",
        "- Phase 105 consolidated `InteractionSessionAssemblyFactory` runtime-bundle seam through typed `InteractionSessionRuntimeBundleFactoryInputs` ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase106-interaction-session-macro-pass-n-signoff] FAILED")
        for error in errors:
            print(f"[phase106-interaction-session-macro-pass-n-signoff] ERROR {error}")
        return 1

    print("[phase106-interaction-session-macro-pass-n-signoff] OK: interaction session macro pass N signoff Phase 106 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
