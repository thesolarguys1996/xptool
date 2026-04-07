from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE176_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE176_INTERACTION_SESSION_FACTORY_DEFAULT_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultFactoryInputsSessionFactory.java"
)
DEFAULT_FACTORY_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultFactoryInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE176_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_FACTORY_INPUTS_SESSION_FACTORY,
        DEFAULT_FACTORY_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase176-interaction-session-factory-default-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase176-interaction-session-factory-default-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase176_plan_text = _read(PHASE176_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_factory_inputs_session_factory_text = _read(DEFAULT_FACTORY_INPUTS_SESSION_FACTORY)
    default_factory_inputs_session_factory_test_text = _read(DEFAULT_FACTORY_INPUTS_SESSION_FACTORY_TEST)

    if "## Phase 176 Slice Status" not in phase176_plan_text:
        errors.append("phase176_plan_missing_slice_status")
    if "`176.1` complete." not in phase176_plan_text:
        errors.append("phase176_plan_missing_176_1_complete")
    if "`176.2` complete." not in phase176_plan_text:
        errors.append("phase176_plan_missing_176_2_complete")
    if "`176.3` complete." not in phase176_plan_text:
        errors.append("phase176_plan_missing_176_3_complete")

    if (
        "## Phase 176 (Interaction Session Factory Default Factory Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase176_section")

    if "PHASE 176 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase176_started")
    if "PHASE 176 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase176_complete")

    required_tasks = [
        "- [x] Define Phase 176 interaction-session factory default-factory-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultFactoryInputsSessionFactory` ownership for interaction-session default factory-input session creation seams.",
        "- [x] Run Phase 176 verification + guard pack and mark `PHASE 176 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase176_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryDefaultFactoryInputsSessionFactory",
        "static InteractionSession createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_factory_inputs_session_factory_text:
            errors.append(f"default_factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryDefaultFactoryInputsSessionFactoryTest",
        "exposesDefaultFactoryInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_factory_inputs_session_factory_test_text:
            errors.append(f"default_factory_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase176-interaction-session-factory-default-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase176-interaction-session-factory-default-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase176-interaction-session-factory-default-factory-inputs-session-factory-extraction] OK: "
        "interaction session factory default-factory-inputs-session-factory extraction Phase 176 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
