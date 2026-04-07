from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE201_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE201_INTERACTION_SESSION_FACTORY_FACTORY_INPUTS_DEFAULT_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY_INPUTS_DEFAULT_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsDefaultSessionFactory.java"
)
FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactory.java"
)
FACTORY_INPUTS_DEFAULT_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsDefaultSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE201_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY_INPUTS_DEFAULT_SESSION_FACTORY,
        FACTORY_INPUTS_SESSION_FACTORY,
        FACTORY_INPUTS_DEFAULT_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase201-interaction-session-factory-factory-inputs-default-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase201-interaction-session-factory-factory-inputs-default-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase201_plan_text = _read(PHASE201_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_inputs_default_session_factory_text = _read(FACTORY_INPUTS_DEFAULT_SESSION_FACTORY)
    factory_inputs_session_factory_text = _read(FACTORY_INPUTS_SESSION_FACTORY)
    factory_inputs_default_session_factory_test_text = _read(FACTORY_INPUTS_DEFAULT_SESSION_FACTORY_TEST)

    if "## Phase 201 Slice Status" not in phase201_plan_text:
        errors.append("phase201_plan_missing_slice_status")
    if "`201.1` complete." not in phase201_plan_text:
        errors.append("phase201_plan_missing_201_1_complete")
    if "`201.2` complete." not in phase201_plan_text:
        errors.append("phase201_plan_missing_201_2_complete")
    if "`201.3` complete." not in phase201_plan_text:
        errors.append("phase201_plan_missing_201_3_complete")

    if (
        "## Phase 201 (Interaction Session Factory Factory Inputs Default Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase201_section")

    if "PHASE 201 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase201_started")
    if "PHASE 201 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase201_complete")

    required_tasks = [
        "- [x] Define Phase 201 interaction-session factory factory-inputs-default-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryFactoryInputsDefaultSessionFactory` ownership and route factory-input default seams through focused ownership.",
        "- [x] Run Phase 201 verification + guard pack and mark `PHASE 201 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase201_line:{task_line}")

    required_default_factory_strings = [
        "final class InteractionSessionFactoryFactoryInputsDefaultSessionFactory",
        "static InteractionSession createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(",
    ]
    for required_string in required_default_factory_strings:
        if required_string not in factory_inputs_default_session_factory_text:
            errors.append(f"factory_inputs_default_session_factory_missing_string:{required_string}")

    required_factory_inputs_strings = [
        "InteractionSessionFactoryFactoryInputsDefaultSessionFactory.createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(",
    ]
    for required_string in required_factory_inputs_strings:
        if required_string not in factory_inputs_session_factory_text:
            errors.append(f"factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryFactoryInputsDefaultSessionFactoryTest",
        "exposesFactoryInputsDefaultSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in factory_inputs_default_session_factory_test_text:
            errors.append(f"factory_inputs_default_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase201-interaction-session-factory-factory-inputs-default-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase201-interaction-session-factory-factory-inputs-default-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase201-interaction-session-factory-factory-inputs-default-session-factory-extraction] OK: "
        "interaction session factory factory-inputs-default-session-factory extraction Phase 201 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
