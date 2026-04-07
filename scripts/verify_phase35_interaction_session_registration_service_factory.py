from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE35_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE35_INTERACTION_SESSION_REGISTRATION_SERVICE_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_REGISTRATION_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationService.java"
)
INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryRegistrationHostTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE35_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_REGISTRATION_SERVICE,
        INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase35-interaction-session-registration-service-factory] FAILED")
        for error in errors:
            print(f"[phase35-interaction-session-registration-service-factory] ERROR {error}")
        return 1

    phase35_plan_text = _read(PHASE35_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    registration_service_text = _read(INTERACTION_SESSION_REGISTRATION_SERVICE)
    registration_host_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_REGISTRATION_TEST)

    if "## Phase 35 Slice Status" not in phase35_plan_text:
        errors.append("phase35_plan_missing_slice_status")
    if "`35.1` complete." not in phase35_plan_text:
        errors.append("phase35_plan_missing_35_1_complete")
    if "`35.2` complete." not in phase35_plan_text:
        errors.append("phase35_plan_missing_35_2_complete")
    if "`35.3` complete." not in phase35_plan_text:
        errors.append("phase35_plan_missing_35_3_complete")

    if "## Phase 35 (Interaction Session Registration Service Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase35_section")

    if "PHASE 35 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase35_started")
    if "PHASE 35 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase35_complete")

    required_tasks = [
        "- [x] Define Phase 35 interaction session registration service-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session registration-service construction from `InteractionSession` into focused host-factory method.",
        "- [x] Run Phase 35 verification + guard pack and mark `PHASE 35 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase35_line:{task_line}")

    if "InteractionSessionHostFactory.createRegistrationService(" not in interaction_session_text:
        errors.append("interaction_session_missing_registration_service_factory_usage")
    if "new InteractionSessionRegistrationService(" in interaction_session_text:
        errors.append("interaction_session_still_constructs_registration_service_inline")

    if "static InteractionSessionRegistrationService createRegistrationService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_factory_method")
    if "static InteractionSessionRegistrationService.Host createRegistrationHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_delegate_host_factory_method")
    if "return new InteractionSessionRegistrationService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_construction")

    if "interface Host" not in registration_service_text:
        errors.append("interaction_session_registration_service_missing_host_interface")
    if "registration = host.registerSession(sessionName);" not in registration_service_text:
        errors.append("interaction_session_registration_service_missing_host_registration_delegate")

    if "createRegistrationHostFromDelegatesRoutesSessionRegistration" not in registration_host_test_text:
        errors.append("interaction_session_host_factory_registration_test_missing_delegate_case")

    if errors:
        print("[phase35-interaction-session-registration-service-factory] FAILED")
        for error in errors:
            print(f"[phase35-interaction-session-registration-service-factory] ERROR {error}")
        return 1

    print(
        "[phase35-interaction-session-registration-service-factory] OK: interaction session registration service factory Phase 35 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
