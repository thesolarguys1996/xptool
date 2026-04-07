from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE187_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE187_INTERACTION_SESSION_FACTORY_SERVICE_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
SERVICE_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactory.java"
)
SERVICE_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryServiceInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE187_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        SERVICE_INPUTS_SESSION_FACTORY,
        SERVICE_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase187-interaction-session-factory-service-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase187-interaction-session-factory-service-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase187_plan_text = _read(PHASE187_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    service_inputs_session_factory_text = _read(SERVICE_INPUTS_SESSION_FACTORY)
    service_inputs_session_factory_test_text = _read(SERVICE_INPUTS_SESSION_FACTORY_TEST)

    if "## Phase 187 Slice Status" not in phase187_plan_text:
        errors.append("phase187_plan_missing_slice_status")
    if "`187.1` complete." not in phase187_plan_text:
        errors.append("phase187_plan_missing_187_1_complete")
    if "`187.2` complete." not in phase187_plan_text:
        errors.append("phase187_plan_missing_187_2_complete")
    if "`187.3` complete." not in phase187_plan_text:
        errors.append("phase187_plan_missing_187_3_complete")

    if (
        "## Phase 187 (Interaction Session Factory Service Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase187_section")

    if "PHASE 187 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase187_started")
    if "PHASE 187 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase187_complete")

    required_tasks = [
        "- [x] Define Phase 187 interaction-session factory service-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryServiceInputsSessionFactory` ownership for interaction-session service-input session creation seams.",
        "- [x] Run Phase 187 verification + guard pack and mark `PHASE 187 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase187_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryServiceInputsSessionFactory",
        "static InteractionSession createFromServices(",
        "InteractionSessionFactoryInputs.fromServices(",
        "InteractionSessionFactory.createFromFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in service_inputs_session_factory_text:
            errors.append(f"service_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryServiceInputsSessionFactoryTest",
        "exposesServiceInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in service_inputs_session_factory_test_text:
            errors.append(f"service_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase187-interaction-session-factory-service-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase187-interaction-session-factory-service-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase187-interaction-session-factory-service-inputs-session-factory-extraction] OK: "
        "interaction session factory service-inputs-session-factory extraction Phase 187 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
