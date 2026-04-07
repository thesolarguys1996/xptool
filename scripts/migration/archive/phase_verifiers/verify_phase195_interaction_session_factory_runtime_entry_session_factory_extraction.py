from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE195_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE195_INTERACTION_SESSION_FACTORY_RUNTIME_ENTRY_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
RUNTIME_ENTRY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactory.java"
)
RUNTIME_ENTRY_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeEntrySessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE195_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        RUNTIME_ENTRY_SESSION_FACTORY,
        RUNTIME_ENTRY_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase195-interaction-session-factory-runtime-entry-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase195-interaction-session-factory-runtime-entry-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase195_plan_text = _read(PHASE195_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    runtime_entry_session_factory_text = _read(RUNTIME_ENTRY_SESSION_FACTORY)
    runtime_entry_session_factory_test_text = _read(RUNTIME_ENTRY_SESSION_FACTORY_TEST)

    if "## Phase 195 Slice Status" not in phase195_plan_text:
        errors.append("phase195_plan_missing_slice_status")
    if "`195.1` complete." not in phase195_plan_text:
        errors.append("phase195_plan_missing_195_1_complete")
    if "`195.2` complete." not in phase195_plan_text:
        errors.append("phase195_plan_missing_195_2_complete")
    if "`195.3` complete." not in phase195_plan_text:
        errors.append("phase195_plan_missing_195_3_complete")

    if (
        "## Phase 195 (Interaction Session Factory Runtime Entry Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase195_section")

    if "PHASE 195 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase195_started")
    if "PHASE 195 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase195_complete")

    required_tasks = [
        "- [x] Define Phase 195 interaction-session factory runtime-entry-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeEntrySessionFactory` ownership for interaction-session runtime-entry session routing seams.",
        "- [x] Run Phase 195 verification + guard pack and mark `PHASE 195 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase195_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeOperations(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    required_runtime_entry_strings = [
        "final class InteractionSessionFactoryRuntimeEntrySessionFactory",
        "static InteractionSession createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(",
        "static InteractionSession createFromRuntimeOperations(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_runtime_entry_strings:
        if required_string not in runtime_entry_session_factory_text:
            errors.append(f"runtime_entry_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeEntrySessionFactoryTest",
        "exposesRuntimeEntrySessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_entry_session_factory_test_text:
            errors.append(f"runtime_entry_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase195-interaction-session-factory-runtime-entry-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase195-interaction-session-factory-runtime-entry-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase195-interaction-session-factory-runtime-entry-session-factory-extraction] OK: "
        "interaction session factory runtime-entry-session-factory extraction Phase 195 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
