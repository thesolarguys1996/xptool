from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE64_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE64_INTERACTION_SESSION_REGISTRATION_SERVICE_FACTORY_EXTRACTION_PLAN.md"
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
        PHASE64_PLAN,
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
        print("[phase64-interaction-session-registration-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase64-interaction-session-registration-service-factory-extraction] ERROR {error}")
        return 1

    phase64_plan_text = _read(PHASE64_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    registration_factory_text = _read(INTERACTION_SESSION_REGISTRATION_FACTORY)
    registration_factory_test_text = _read(INTERACTION_SESSION_REGISTRATION_FACTORY_TEST)

    if "## Phase 64 Slice Status" not in phase64_plan_text:
        errors.append("phase64_plan_missing_slice_status")
    if "`64.1` complete." not in phase64_plan_text:
        errors.append("phase64_plan_missing_64_1_complete")
    if "`64.2` complete." not in phase64_plan_text:
        errors.append("phase64_plan_missing_64_2_complete")
    if "`64.3` complete." not in phase64_plan_text:
        errors.append("phase64_plan_missing_64_3_complete")

    if "## Phase 64 (Interaction Session Registration Service-From-Host Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase64_section")

    if "PHASE 64 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase64_started")
    if "PHASE 64 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase64_complete")

    required_tasks = [
        "- [x] Define Phase 64 interaction session registration service-from-host factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session registration service-from-host assembly into focused `InteractionSessionRegistrationFactory` ownership.",
        "- [x] Run Phase 64 verification + guard pack and mark `PHASE 64 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase64_line:{task_line}")

    if "InteractionSessionRegistrationFactory.createRegistrationServiceFromHost(host, sessionInteractionKey);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_from_host_factory_delegation")
    if "return new InteractionSessionRegistrationService(host, sessionInteractionKey);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_registration_service_compatibility_sentinel")

    if "static InteractionSessionRegistrationService createRegistrationServiceFromHost(" not in registration_factory_text:
        errors.append("interaction_session_registration_factory_missing_service_from_host_method")
    if "return new InteractionSessionRegistrationService(host, sessionInteractionKey);" not in registration_factory_text:
        errors.append("interaction_session_registration_factory_missing_service_construction")

    if "createRegistrationServiceFromHostEnsuresAndClearsRegistrationLifecycle" not in registration_factory_test_text:
        errors.append("interaction_session_registration_factory_test_missing_service_from_host_case")

    if errors:
        print("[phase64-interaction-session-registration-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase64-interaction-session-registration-service-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase64-interaction-session-registration-service-factory-extraction] OK: interaction session registration service-from-host factory extraction Phase 64 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
