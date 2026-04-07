from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE163_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE163_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_FACTORY_WIRING_CONSOLIDATION_AA_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE163_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, DEFAULT_ENTRY_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase163-interaction-session-factory-default-entry-factory-wiring-consolidation-aa] FAILED")
        for error in errors:
            print(f"[phase163-interaction-session-factory-default-entry-factory-wiring-consolidation-aa] ERROR {error}")
        return 1

    phase163_plan_text = _read(PHASE163_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)

    if "## Phase 163 Slice Status" not in phase163_plan_text:
        errors.append("phase163_plan_missing_slice_status")
    if "`163.1` complete." not in phase163_plan_text:
        errors.append("phase163_plan_missing_163_1_complete")
    if "`163.2` complete." not in phase163_plan_text:
        errors.append("phase163_plan_missing_163_2_complete")
    if "`163.3` complete." not in phase163_plan_text:
        errors.append("phase163_plan_missing_163_3_complete")

    if "## Phase 163 (Interaction Session Factory Default Entry Factory Wiring Consolidation AA)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase163_section")

    if "PHASE 163 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase163_started")
    if "PHASE 163 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase163_complete")

    required_tasks = [
        "- [x] Define Phase 163 interaction-session factory default-entry-factory wiring consolidation AA scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(...)` through focused default runtime-bundle-factory-input construction ownership.",
        "- [x] Run Phase 163 verification + guard pack and mark `PHASE 163 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase163_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "return createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    if errors:
        print("[phase163-interaction-session-factory-default-entry-factory-wiring-consolidation-aa] FAILED")
        for error in errors:
            print(f"[phase163-interaction-session-factory-default-entry-factory-wiring-consolidation-aa] ERROR {error}")
        return 1

    print(
        "[phase163-interaction-session-factory-default-entry-factory-wiring-consolidation-aa] OK: "
        "interaction session factory default-entry-factory wiring consolidation AA Phase 163 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
