from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE159_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE159_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_WIRING_CONSOLIDATION_Z_PLAN.md"
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

    required_paths = [PHASE159_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase159-interaction-session-factory-default-entry-wiring-consolidation-z] FAILED")
        for error in errors:
            print(f"[phase159-interaction-session-factory-default-entry-wiring-consolidation-z] ERROR {error}")
        return 1

    phase159_plan_text = _read(PHASE159_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 159 Slice Status" not in phase159_plan_text:
        errors.append("phase159_plan_missing_slice_status")
    if "`159.1` complete." not in phase159_plan_text:
        errors.append("phase159_plan_missing_159_1_complete")
    if "`159.2` complete." not in phase159_plan_text:
        errors.append("phase159_plan_missing_159_2_complete")
    if "`159.3` complete." not in phase159_plan_text:
        errors.append("phase159_plan_missing_159_3_complete")

    if "## Phase 159 (Interaction Session Factory Default Entry Wiring Consolidation Z)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase159_section")

    if "PHASE 159 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase159_started")
    if "PHASE 159 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase159_complete")

    required_tasks = [
        "- [x] Define Phase 159 interaction-session factory default-entry wiring consolidation Z scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromFactoryInputs(...)` through focused `InteractionSessionFactoryDefaultEntryFactory` routing ownership.",
        "- [x] Run Phase 159 verification + guard pack and mark `PHASE 159 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase159_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "return InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(",
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "return createFromRuntimeBundleFactoryInputs(defaultRuntimeBundleFactoryInputs);",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase159-interaction-session-factory-default-entry-wiring-consolidation-z] FAILED")
        for error in errors:
            print(f"[phase159-interaction-session-factory-default-entry-wiring-consolidation-z] ERROR {error}")
        return 1

    print(
        "[phase159-interaction-session-factory-default-entry-wiring-consolidation-z] OK: interaction session factory "
        "default-entry wiring consolidation Z Phase 159 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
