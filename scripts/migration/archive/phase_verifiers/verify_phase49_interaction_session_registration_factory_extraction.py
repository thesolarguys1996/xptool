from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE49_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE49_INTERACTION_SESSION_REGISTRATION_FACTORY_EXTRACTION_PLAN.md"
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


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE49_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_REGISTRATION_FACTORY,
        INTERACTION_SESSION_REGISTRATION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase49-interaction-session-registration-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase49-interaction-session-registration-factory-extraction] ERROR {error}")
        return 1

    phase49_plan_text = _read(PHASE49_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    registration_factory_text = _read(INTERACTION_SESSION_REGISTRATION_FACTORY)
    registration_factory_test_text = _read(INTERACTION_SESSION_REGISTRATION_FACTORY_TEST)

    if "## Phase 49 Slice Status" not in phase49_plan_text:
        errors.append("phase49_plan_missing_slice_status")
    if "`49.1` complete." not in phase49_plan_text:
        errors.append("phase49_plan_missing_49_1_complete")
    if "`49.2` complete." not in phase49_plan_text:
        errors.append("phase49_plan_missing_49_2_complete")
    if "`49.3` complete." not in phase49_plan_text:
        errors.append("phase49_plan_missing_49_3_complete")

    if "## Phase 49 (Interaction Session Registration Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase49_section")

    if "PHASE 49 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase49_started")
    if "PHASE 49 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase49_complete")

    required_tasks = [
        "- [x] Define Phase 49 interaction session registration factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session registration host assembly into focused `InteractionSessionRegistrationFactory`.",
        "- [x] Run Phase 49 verification + guard pack and mark `PHASE 49 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase49_line:{task_line}")

    if "static InteractionSessionRegistrationService createRegistrationServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_from_host_method")
    if "return createRegistrationServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_from_host_delegation")
    if "InteractionSessionRegistrationFactory.createRegistrationHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_factory_host_delegation")
    if "InteractionSessionRegistrationFactory.createRegistrationHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_factory_delegate_host_delegation")

    if "final class InteractionSessionRegistrationFactory" not in registration_factory_text:
        errors.append("interaction_session_registration_factory_missing_class")
    if "static InteractionSessionRegistrationService.Host createRegistrationHost(" not in registration_factory_text:
        errors.append("interaction_session_registration_factory_missing_host_factory_method")
    if "static InteractionSessionRegistrationService.Host createRegistrationHostFromDelegates(" not in registration_factory_text:
        errors.append("interaction_session_registration_factory_missing_delegate_host_factory_method")

    if "createRegistrationHostFromDelegatesRoutesSessionRegistration" not in registration_factory_test_text:
        errors.append("interaction_session_registration_factory_test_missing_delegate_case")

    if errors:
        print("[phase49-interaction-session-registration-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase49-interaction-session-registration-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase49-interaction-session-registration-factory-extraction] OK: interaction session registration factory extraction Phase 49 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
