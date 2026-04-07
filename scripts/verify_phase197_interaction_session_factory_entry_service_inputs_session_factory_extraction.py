from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE197_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE197_INTERACTION_SESSION_FACTORY_ENTRY_SERVICE_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ENTRY_SERVICE_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactory.java"
)
ENTRY_SERVICE_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryServiceInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE197_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ENTRY_SERVICE_INPUTS_SESSION_FACTORY,
        ENTRY_SERVICE_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase197-interaction-session-factory-entry-service-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase197-interaction-session-factory-entry-service-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase197_plan_text = _read(PHASE197_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    entry_service_inputs_session_factory_text = _read(ENTRY_SERVICE_INPUTS_SESSION_FACTORY)
    entry_service_inputs_session_factory_test_text = _read(ENTRY_SERVICE_INPUTS_SESSION_FACTORY_TEST)

    if "## Phase 197 Slice Status" not in phase197_plan_text:
        errors.append("phase197_plan_missing_slice_status")
    if "`197.1` complete." not in phase197_plan_text:
        errors.append("phase197_plan_missing_197_1_complete")
    if "`197.2` complete." not in phase197_plan_text:
        errors.append("phase197_plan_missing_197_2_complete")
    if "`197.3` complete." not in phase197_plan_text:
        errors.append("phase197_plan_missing_197_3_complete")

    if (
        "## Phase 197 (Interaction Session Factory Entry Service Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase197_section")

    if "PHASE 197 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase197_started")
    if "PHASE 197 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase197_complete")

    required_tasks = [
        "- [x] Define Phase 197 interaction-session factory entry-service-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryEntryServiceInputsSessionFactory` ownership for interaction-session entry service-input session routing seams.",
        "- [x] Run Phase 197 verification + guard pack and mark `PHASE 197 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase197_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryEntryServiceInputsSessionFactory",
        "static InteractionSession create(",
        "InteractionSessionFactoryServiceInputsSessionFactory.createFromServices(",
    ]
    for required_string in required_factory_strings:
        if required_string not in entry_service_inputs_session_factory_text:
            errors.append(f"entry_service_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryEntryServiceInputsSessionFactoryTest",
        "exposesEntryServiceInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in entry_service_inputs_session_factory_test_text:
            errors.append(f"entry_service_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase197-interaction-session-factory-entry-service-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase197-interaction-session-factory-entry-service-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase197-interaction-session-factory-entry-service-inputs-session-factory-extraction] OK: "
        "interaction session factory entry-service-inputs-session-factory extraction Phase 197 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
