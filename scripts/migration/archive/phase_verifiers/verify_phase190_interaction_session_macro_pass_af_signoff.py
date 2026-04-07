from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE190_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE190_INTERACTION_SESSION_MACRO_PASS_AF_SIGNOFF_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
JAVA_SURFACE_INVENTORY = PROJECT_ROOT / "docs/NATIVE_JAVA_SURFACE_INVENTORY.md"
TASKS = PROJECT_ROOT / "TASKS.md"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE190_PLAN, MIGRATION_PLAN, PHASE_STATUS, JAVA_SURFACE_INVENTORY, TASKS]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase190-interaction-session-macro-pass-af-signoff] FAILED")
        for error in errors:
            print(f"[phase190-interaction-session-macro-pass-af-signoff] ERROR {error}")
        return 1

    phase190_plan_text = _read(PHASE190_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    java_surface_inventory_text = _read(JAVA_SURFACE_INVENTORY)
    tasks_text = _read(TASKS)

    if "## Phase 190 Slice Status" not in phase190_plan_text:
        errors.append("phase190_plan_missing_slice_status")
    if "`190.1` complete." not in phase190_plan_text:
        errors.append("phase190_plan_missing_190_1_complete")
    if "`190.2` complete." not in phase190_plan_text:
        errors.append("phase190_plan_missing_190_2_complete")
    if "`190.3` complete." not in phase190_plan_text:
        errors.append("phase190_plan_missing_190_3_complete")

    if "## Phase 190 (Interaction Session Macro Pass AF Signoff)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase190_section")

    if "PHASE 190 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase190_started")
    if "PHASE 190 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase190_complete")

    required_tasks = [
        "- [x] Define Phase 190 interaction-session macro pass AF signoff scope and completion evidence gates.",
        "- [x] Publish updated Java surface inventory and migration-plan/task/status artifacts for Phases 185-190.",
        "- [x] Run Phase 190 verification + guard pack and mark `PHASE 190 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase190_line:{task_line}")

    required_migration_sections = [
        "## Phase 185 (Interaction Session Factory Default Runtime Bundle Factory Inputs Session Factory Extraction)",
        "## Phase 186 (Interaction Session Factory Default Runtime Bundle Factory Input Typed Routing Extraction)",
        "## Phase 187 (Interaction Session Factory Service Inputs Session Factory Extraction)",
        "## Phase 188 (Interaction Session Factory Service Input Typed Routing Extraction)",
        "## Phase 189 (Interaction Session Factory Entry Session Factory Extraction)",
    ]
    for migration_section in required_migration_sections:
        if migration_section not in migration_plan_text:
            errors.append(f"migration_plan_missing_required_section:{migration_section}")

    required_inventory_lines = [
        "- Phase 185 extracted focused interaction-session factory default-runtime-bundle-factory-inputs session factory ownership",
        "- Phase 186 consolidated `InteractionSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` seams through focused default-runtime-bundle-factory-input session routing ownership",
        "- Phase 187 extracted focused interaction-session factory service-inputs session factory ownership",
        "- Phase 188 consolidated `InteractionSessionFactory.create(...)` seams through focused service-input session routing ownership",
        "- Phase 189 extracted focused interaction-session factory entry session factory ownership",
    ]
    for inventory_line in required_inventory_lines:
        if inventory_line not in java_surface_inventory_text:
            errors.append(f"java_surface_inventory_missing_line:{inventory_line}")

    if errors:
        print("[phase190-interaction-session-macro-pass-af-signoff] FAILED")
        for error in errors:
            print(f"[phase190-interaction-session-macro-pass-af-signoff] ERROR {error}")
        return 1

    print("[phase190-interaction-session-macro-pass-af-signoff] OK: interaction session macro pass AF signoff Phase 190 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
