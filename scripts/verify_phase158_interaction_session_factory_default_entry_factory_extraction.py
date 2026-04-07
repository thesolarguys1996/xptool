from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE158_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE158_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java"
)
DEFAULT_ENTRY_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE158_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ENTRY_FACTORY,
        DEFAULT_ENTRY_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase158-interaction-session-factory-default-entry-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase158-interaction-session-factory-default-entry-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase158_plan_text = _read(PHASE158_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)
    default_entry_factory_test_text = _read(DEFAULT_ENTRY_FACTORY_TEST)

    if "## Phase 158 Slice Status" not in phase158_plan_text:
        errors.append("phase158_plan_missing_slice_status")
    if "`158.1` complete." not in phase158_plan_text:
        errors.append("phase158_plan_missing_158_1_complete")
    if "`158.2` complete." not in phase158_plan_text:
        errors.append("phase158_plan_missing_158_2_complete")
    if "`158.3` complete." not in phase158_plan_text:
        errors.append("phase158_plan_missing_158_3_complete")

    if "## Phase 158 (Interaction Session Factory Default Entry Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase158_section")

    if "PHASE 158 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase158_started")
    if "PHASE 158 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase158_complete")

    required_tasks = [
        "- [x] Define Phase 158 interaction-session factory default-entry-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultEntryFactory` ownership for interaction-session factory default-entry creation seams.",
        "- [x] Run Phase 158 verification + guard pack and mark `PHASE 158 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase158_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryDefaultEntryFactory",
        "static InteractionSession createFromFactoryInputs(",
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryDefaultEntryFactoryTest",
        "defaultRuntimeBundleFactoryInputsFactoryBuildsInputsWithProvidedSessionKey",
        "exposesInteractionSessionFactoryDefaultEntryFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_entry_factory_test_text:
            errors.append(f"default_entry_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase158-interaction-session-factory-default-entry-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase158-interaction-session-factory-default-entry-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase158-interaction-session-factory-default-entry-factory-extraction] OK: "
        "interaction session factory default-entry-factory extraction Phase 158 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
