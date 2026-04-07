from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE213_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE213_INTERACTION_SESSION_FACTORY_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ENTRY_RUNTIME_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryRuntimeSessionFactory.java"
)
ENTRY_RUNTIME_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryRuntimeSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE213_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ENTRY_RUNTIME_SESSION_FACTORY,
        ENTRY_RUNTIME_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase213-interaction-session-factory-entry-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase213-interaction-session-factory-entry-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase213_plan_text = _read(PHASE213_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    entry_runtime_session_factory_text = _read(ENTRY_RUNTIME_SESSION_FACTORY)
    entry_runtime_session_factory_test_text = _read(ENTRY_RUNTIME_SESSION_FACTORY_TEST)

    if "## Phase 213 Slice Status" not in phase213_plan_text:
        errors.append("phase213_plan_missing_slice_status")
    if "`213.1` complete." not in phase213_plan_text:
        errors.append("phase213_plan_missing_213_1_complete")
    if "`213.2` complete." not in phase213_plan_text:
        errors.append("phase213_plan_missing_213_2_complete")
    if "`213.3` complete." not in phase213_plan_text:
        errors.append("phase213_plan_missing_213_3_complete")

    if (
        "## Phase 213 (Interaction Session Factory Entry Runtime Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase213_section")

    if "PHASE 213 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase213_started")
    if "PHASE 213 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase213_complete")

    required_tasks = [
        "- [x] Define Phase 213 interaction-session factory entry-runtime-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryEntryRuntimeSessionFactory` ownership and route entry runtime seams through focused ownership.",
        "- [x] Run Phase 213 verification + guard pack and mark `PHASE 213 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase213_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryEntryRuntimeSessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryEntryRuntimeSessionFactory.createFromRuntimeOperations(",
        "InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    required_entry_runtime_strings = [
        "final class InteractionSessionFactoryEntryRuntimeSessionFactory",
        "static InteractionSession createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeBundle(",
        "static InteractionSession createFromRuntimeOperations(",
        "InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_entry_runtime_strings:
        if required_string not in entry_runtime_session_factory_text:
            errors.append(f"entry_runtime_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryEntryRuntimeSessionFactoryTest",
        "exposesEntryRuntimeSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in entry_runtime_session_factory_test_text:
            errors.append(f"entry_runtime_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase213-interaction-session-factory-entry-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase213-interaction-session-factory-entry-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase213-interaction-session-factory-entry-runtime-session-factory-extraction] OK: "
        "interaction session factory entry-runtime-session-factory extraction Phase 213 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
