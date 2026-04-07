from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE189_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE189_INTERACTION_SESSION_FACTORY_ENTRY_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ENTRY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java"
)
ENTRY_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE189_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ENTRY_SESSION_FACTORY,
        ENTRY_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase189-interaction-session-factory-entry-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase189-interaction-session-factory-entry-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase189_plan_text = _read(PHASE189_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    entry_session_factory_text = _read(ENTRY_SESSION_FACTORY)
    entry_session_factory_test_text = _read(ENTRY_SESSION_FACTORY_TEST)

    if "## Phase 189 Slice Status" not in phase189_plan_text:
        errors.append("phase189_plan_missing_slice_status")
    if "`189.1` complete." not in phase189_plan_text:
        errors.append("phase189_plan_missing_189_1_complete")
    if "`189.2` complete." not in phase189_plan_text:
        errors.append("phase189_plan_missing_189_2_complete")
    if "`189.3` complete." not in phase189_plan_text:
        errors.append("phase189_plan_missing_189_3_complete")

    if (
        "## Phase 189 (Interaction Session Factory Entry Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase189_section")

    if "PHASE 189 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase189_started")
    if "PHASE 189 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase189_complete")

    required_tasks = [
        "- [x] Define Phase 189 interaction-session factory entry-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryEntrySessionFactory` ownership for interaction-session top-level entry routing seams.",
        "- [x] Run Phase 189 verification + guard pack and mark `PHASE 189 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase189_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryEntrySessionFactory.create(",
        "InteractionSessionFactoryEntrySessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    required_entry_factory_strings = [
        "final class InteractionSessionFactoryEntrySessionFactory",
        "static InteractionSession create(",
        "InteractionSessionFactoryServiceInputsSessionFactory.createFromServices(",
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_entry_factory_strings:
        if required_string not in entry_session_factory_text:
            errors.append(f"entry_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryEntrySessionFactoryTest",
        "exposesEntrySessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in entry_session_factory_test_text:
            errors.append(f"entry_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase189-interaction-session-factory-entry-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase189-interaction-session-factory-entry-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase189-interaction-session-factory-entry-session-factory-extraction] OK: "
        "interaction session factory entry-session-factory extraction Phase 189 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
