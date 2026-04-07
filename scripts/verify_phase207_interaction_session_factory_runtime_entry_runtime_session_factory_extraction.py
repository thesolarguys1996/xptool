from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE207_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE207_INTERACTION_SESSION_FACTORY_RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.java"
)
RUNTIME_ENTRY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactory.java"
)
RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntryRuntimeSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE207_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY,
        RUNTIME_ENTRY_SESSION_FACTORY,
        RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase207-interaction-session-factory-runtime-entry-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase207-interaction-session-factory-runtime-entry-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase207_plan_text = _read(PHASE207_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_entry_runtime_session_factory_text = _read(RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY)
    runtime_entry_session_factory_text = _read(RUNTIME_ENTRY_SESSION_FACTORY)
    runtime_entry_runtime_session_factory_test_text = _read(RUNTIME_ENTRY_RUNTIME_SESSION_FACTORY_TEST)

    if "## Phase 207 Slice Status" not in phase207_plan_text:
        errors.append("phase207_plan_missing_slice_status")
    if "`207.1` complete." not in phase207_plan_text:
        errors.append("phase207_plan_missing_207_1_complete")
    if "`207.2` complete." not in phase207_plan_text:
        errors.append("phase207_plan_missing_207_2_complete")
    if "`207.3` complete." not in phase207_plan_text:
        errors.append("phase207_plan_missing_207_3_complete")

    if (
        "## Phase 207 (Interaction Session Factory Runtime Entry Runtime Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase207_section")

    if "PHASE 207 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase207_started")
    if "PHASE 207 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase207_complete")

    required_tasks = [
        "- [x] Define Phase 207 interaction-session factory runtime-entry-runtime-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory` ownership and route runtime-entry seams through focused ownership.",
        "- [x] Run Phase 207 verification + guard pack and mark `PHASE 207 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase207_line:{task_line}")

    required_runtime_factory_strings = [
        "final class InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory",
        "static InteractionSession createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(",
        "static InteractionSession createFromRuntimeOperations(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_runtime_factory_strings:
        if required_string not in runtime_entry_runtime_session_factory_text:
            errors.append(f"runtime_entry_runtime_session_factory_missing_string:{required_string}")

    required_runtime_entry_strings = [
        "InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.createFromRuntimeOperations(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_runtime_entry_strings:
        if required_string not in runtime_entry_session_factory_text:
            errors.append(f"runtime_entry_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeEntryRuntimeSessionFactoryTest",
        "exposesRuntimeEntryRuntimeSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_entry_runtime_session_factory_test_text:
            errors.append(f"runtime_entry_runtime_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase207-interaction-session-factory-runtime-entry-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase207-interaction-session-factory-runtime-entry-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase207-interaction-session-factory-runtime-entry-runtime-session-factory-extraction] OK: "
        "interaction session factory runtime-entry-runtime-session-factory extraction Phase 207 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
