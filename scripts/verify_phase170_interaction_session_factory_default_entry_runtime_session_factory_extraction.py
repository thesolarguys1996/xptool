from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE170_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE170_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeSessionFactory.java"
)
DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE170_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY,
        DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase170-interaction-session-factory-default-entry-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase170-interaction-session-factory-default-entry-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase170_plan_text = _read(PHASE170_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_runtime_session_factory_text = _read(DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY)
    default_entry_runtime_session_factory_test_text = _read(DEFAULT_ENTRY_RUNTIME_SESSION_FACTORY_TEST)

    if "## Phase 170 Slice Status" not in phase170_plan_text:
        errors.append("phase170_plan_missing_slice_status")
    if "`170.1` complete." not in phase170_plan_text:
        errors.append("phase170_plan_missing_170_1_complete")
    if "`170.2` complete." not in phase170_plan_text:
        errors.append("phase170_plan_missing_170_2_complete")
    if "`170.3` complete." not in phase170_plan_text:
        errors.append("phase170_plan_missing_170_3_complete")

    if "## Phase 170 (Interaction Session Factory Default Entry Runtime Session Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase170_section")

    if "PHASE 170 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase170_started")
    if "PHASE 170 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase170_complete")

    required_tasks = [
        "- [x] Define Phase 170 interaction-session factory default-entry-runtime-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultEntryRuntimeSessionFactory` ownership for interaction-session default-entry runtime-session routing seams.",
        "- [x] Run Phase 170 verification + guard pack and mark `PHASE 170 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase170_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryDefaultEntryRuntimeSessionFactory",
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_entry_runtime_session_factory_text:
            errors.append(f"default_entry_runtime_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryDefaultEntryRuntimeSessionFactoryTest",
        "exposesDefaultEntryRuntimeSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_entry_runtime_session_factory_test_text:
            errors.append(f"default_entry_runtime_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase170-interaction-session-factory-default-entry-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase170-interaction-session-factory-default-entry-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase170-interaction-session-factory-default-entry-runtime-session-factory-extraction] OK: "
        "interaction session factory default-entry-runtime-session-factory extraction Phase 170 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
