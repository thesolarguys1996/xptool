from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE165_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE165_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_AB_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE165_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase165-interaction-session-factory-default-entry-wiring-consolidation-ab] FAILED")
        for error in errors:
            print(f"[phase165-interaction-session-factory-default-entry-wiring-consolidation-ab] ERROR {error}")
        return 1

    phase165_plan_text = _read(PHASE165_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 165 Slice Status" not in phase165_plan_text:
        errors.append("phase165_plan_missing_slice_status")
    if "`165.1` complete." not in phase165_plan_text:
        errors.append("phase165_plan_missing_165_1_complete")
    if "`165.2` complete." not in phase165_plan_text:
        errors.append("phase165_plan_missing_165_2_complete")
    if "`165.3` complete." not in phase165_plan_text:
        errors.append("phase165_plan_missing_165_3_complete")

    if "## Phase 165 (Interaction Session Factory Default Entry Wiring Consolidation AB)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase165_section")

    if "PHASE 165 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase165_started")
    if "PHASE 165 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase165_complete")

    required_tasks = [
        "- [x] Define Phase 165 interaction-session factory default-entry wiring consolidation AB scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` through focused `InteractionSessionFactoryDefaultEntryFactory` routing ownership.",
        "- [x] Run Phase 165 verification + guard pack and mark `PHASE 165 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase165_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultEntryFactory.createFromDefaultRuntimeBundleFactoryInputs(",
        "return createFromRuntimeBundleFactoryInputs(defaultRuntimeBundleFactoryInputs);",
        "return InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase165-interaction-session-factory-default-entry-wiring-consolidation-ab] FAILED")
        for error in errors:
            print(f"[phase165-interaction-session-factory-default-entry-wiring-consolidation-ab] ERROR {error}")
        return 1

    print(
        "[phase165-interaction-session-factory-default-entry-wiring-consolidation-ab] OK: interaction session factory "
        "default-entry wiring consolidation AB Phase 165 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
