from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE76_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE76_INTERACTION_SESSION_REGISTRATION_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_REGISTRATION_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationFactory.java"
)
INTERACTION_SESSION_REGISTRATION_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationFactoryTest.java"
)
INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE76_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_REGISTRATION_FACTORY,
        INTERACTION_SESSION_REGISTRATION_FACTORY_TEST,
        INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase76-interaction-session-registration-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase76-interaction-session-registration-service-composite-factory-extraction] ERROR {error}")
        return 1

    phase76_plan_text = _read(PHASE76_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    registration_factory_text = _read(INTERACTION_SESSION_REGISTRATION_FACTORY)
    registration_factory_test_text = _read(INTERACTION_SESSION_REGISTRATION_FACTORY_TEST)
    host_factory_registration_service_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_SERVICE_TEST)

    if "## Phase 76 Slice Status" not in phase76_plan_text:
        errors.append("phase76_plan_missing_slice_status")
    if "`76.1` complete." not in phase76_plan_text:
        errors.append("phase76_plan_missing_76_1_complete")
    if "`76.2` complete." not in phase76_plan_text:
        errors.append("phase76_plan_missing_76_2_complete")
    if "`76.3` complete." not in phase76_plan_text:
        errors.append("phase76_plan_missing_76_3_complete")

    if "## Phase 76 (Interaction Session Registration Service Composite Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase76_section")

    if "PHASE 76 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase76_started")
    if "PHASE 76 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase76_complete")

    required_tasks = [
        "- [x] Define Phase 76 interaction session registration service composite factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session registration composite service assembly into focused `InteractionSessionRegistrationFactory` ownership.",
        "- [x] Run Phase 76 verification + guard pack and mark `PHASE 76 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase76_line:{task_line}")

    if "InteractionSessionRegistrationFactory.createRegistrationService(sessionManager, sessionInteractionKey);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_composite_service_factory_delegation")

    compatibility_strings = [
        "return createRegistrationServiceFromHost(",
        "createRegistrationHost(sessionManager)",
        "return new InteractionSessionRegistrationService(host, sessionInteractionKey);",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "static InteractionSessionRegistrationService createRegistrationService(" not in registration_factory_text:
        errors.append("interaction_session_registration_factory_missing_composite_service_method")
    if "return createRegistrationServiceFromHost(" not in registration_factory_text:
        errors.append("interaction_session_registration_factory_missing_composite_service_delegation")

    if "createRegistrationServiceEnsuresAndClearsRegistrationLifecycle" not in registration_factory_test_text:
        errors.append("interaction_session_registration_factory_test_missing_composite_service_case")
    if "createRegistrationServiceEnsuresAndClearsRegistrationLifecycle" not in host_factory_registration_service_test_text:
        errors.append("interaction_session_host_factory_registration_service_test_missing_composite_service_case")

    if errors:
        print("[phase76-interaction-session-registration-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase76-interaction-session-registration-service-composite-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase76-interaction-session-registration-service-composite-factory-extraction] OK: interaction session registration service composite factory extraction Phase 76 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
