from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE191_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE191_INTERACTION_SESSION_FACTORY_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactory.java"
)
FACTORY_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE191_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY_INPUTS_SESSION_FACTORY,
        FACTORY_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase191-interaction-session-factory-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase191-interaction-session-factory-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase191_plan_text = _read(PHASE191_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_inputs_session_factory_text = _read(FACTORY_INPUTS_SESSION_FACTORY)
    factory_inputs_session_factory_test_text = _read(FACTORY_INPUTS_SESSION_FACTORY_TEST)

    if "## Phase 191 Slice Status" not in phase191_plan_text:
        errors.append("phase191_plan_missing_slice_status")
    if "`191.1` complete." not in phase191_plan_text:
        errors.append("phase191_plan_missing_191_1_complete")
    if "`191.2` complete." not in phase191_plan_text:
        errors.append("phase191_plan_missing_191_2_complete")
    if "`191.3` complete." not in phase191_plan_text:
        errors.append("phase191_plan_missing_191_3_complete")

    if (
        "## Phase 191 (Interaction Session Factory Factory Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase191_section")

    if "PHASE 191 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase191_started")
    if "PHASE 191 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase191_complete")

    required_tasks = [
        "- [x] Define Phase 191 interaction-session factory factory-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryFactoryInputsSessionFactory` ownership for interaction-session factory-input session creation seams.",
        "- [x] Run Phase 191 verification + guard pack and mark `PHASE 191 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase191_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryFactoryInputsSessionFactory",
        "static InteractionSession createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_inputs_session_factory_text:
            errors.append(f"factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryFactoryInputsSessionFactoryTest",
        "exposesFactoryInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in factory_inputs_session_factory_test_text:
            errors.append(f"factory_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase191-interaction-session-factory-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase191-interaction-session-factory-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase191-interaction-session-factory-factory-inputs-session-factory-extraction] OK: "
        "interaction session factory factory-inputs-session-factory extraction Phase 191 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
