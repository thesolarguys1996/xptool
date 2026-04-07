from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE172_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE172_INTERACTION_SESSION_MACRO_PASS_AC_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE172_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase172-interaction-session-macro-pass-ac-signoff] FAILED")
        for error in errors:
            print(f"[phase172-interaction-session-macro-pass-ac-signoff] ERROR {error}")
        return 1

    phase172_plan_text = _read(PHASE172_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 172 Slice Status" not in phase172_plan_text:
        errors.append("phase172_plan_missing_slice_status")
    if "`172.1` complete." not in phase172_plan_text:
        errors.append("phase172_plan_missing_172_1_complete")
    if "`172.2` complete." not in phase172_plan_text:
        errors.append("phase172_plan_missing_172_2_complete")
    if "`172.3` complete." not in phase172_plan_text:
        errors.append("phase172_plan_missing_172_3_complete")

    if "## Phase 172 (Interaction Session Macro Pass AC Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase172_section")

    if "PHASE 172 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase172_started")
    if "PHASE 172 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase172_complete")

    required_tasks = [
        "- [x] Define Phase 172 interaction-session macro pass AC signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 167-172.",
        "- [x] Run Phase 172 verification + guard pack and mark `PHASE 172 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase172_line:{task_line}")

    required_migration_sections = [
        "## Phase 167 (Interaction Session Factory Default Runtime Bundle Factory Extraction)",
        "## Phase 168 (Interaction Session Factory Default Runtime Bundle Typed Routing Extraction)",
        "## Phase 169 (Interaction Session Factory Default Runtime Session Wiring Consolidation AC)",
        "## Phase 170 (Interaction Session Factory Default Entry Runtime Session Factory Extraction)",
        "## Phase 171 (Interaction Session Factory Default Entry Wiring Consolidation AC)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 167 extracted focused interaction-session factory default runtime-bundle factory ownership",
        "- Phase 168 extracted typed default runtime-bundle routing through `InteractionSessionFactoryDefaultRuntimeBundleFactory` ownership",
        "- Phase 169 consolidated `InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` seams through focused default runtime-bundle routing ownership",
        "- Phase 170 extracted focused interaction-session factory default-entry runtime-session factory ownership",
        "- Phase 171 consolidated `InteractionSessionFactoryDefaultEntryFactory` routing seams through focused default-entry runtime-session factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase172-interaction-session-macro-pass-ac-signoff] FAILED")
        for error in errors:
            print(f"[phase172-interaction-session-macro-pass-ac-signoff] ERROR {error}")
        return 1

    print("[phase172-interaction-session-macro-pass-ac-signoff] OK: interaction session macro pass AC signoff Phase 172 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
