from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE110_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE110_INTERACTION_SESSION_MACRO_PASS_O_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE110_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase110-interaction-session-macro-pass-o-signoff] FAILED")
        for error in errors:
            print(f"[phase110-interaction-session-macro-pass-o-signoff] ERROR {error}")
        return 1

    phase110_plan_text = _read(PHASE110_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 110 Slice Status" not in phase110_plan_text:
        errors.append("phase110_plan_missing_slice_status")
    if "`110.1` complete." not in phase110_plan_text:
        errors.append("phase110_plan_missing_110_1_complete")
    if "`110.2` complete." not in phase110_plan_text:
        errors.append("phase110_plan_missing_110_2_complete")
    if "`110.3` complete." not in phase110_plan_text:
        errors.append("phase110_plan_missing_110_3_complete")

    if "## Phase 110 (Interaction Session Macro Pass O Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase110_section")

    if "PHASE 110 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase110_started")
    if "PHASE 110 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase110_complete")

    required_tasks = [
        "- [x] Define Phase 110 interaction-session macro pass O signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 107-110.",
        "- [x] Run Phase 110 verification + guard pack and mark `PHASE 110 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase110_line:{task_line}")

    required_migration_sections = [
        "## Phase 107 (Interaction Session Assembly Factory Inputs Extraction)",
        "## Phase 108 (Interaction Session Assembly Factory Typed Entry Extraction)",
        "## Phase 109 (Interaction Session Factory Wiring Consolidation O)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 107 extracted focused assembly-factory typed inputs ownership",
        "- Phase 108 extracted typed-entry runtime-bundle assembly seams in `InteractionSessionAssemblyFactory` ownership",
        "- Phase 109 consolidated `InteractionSessionFactory` runtime-bundle seam through typed `InteractionSessionAssemblyFactoryInputs` ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase110-interaction-session-macro-pass-o-signoff] FAILED")
        for error in errors:
            print(f"[phase110-interaction-session-macro-pass-o-signoff] ERROR {error}")
        return 1

    print("[phase110-interaction-session-macro-pass-o-signoff] OK: interaction session macro pass O signoff Phase 110 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
