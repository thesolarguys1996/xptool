from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE27_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE27_INTERACTION_SESSION_REGISTRATION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_REGISTRATION_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRegistrationService.java"
)
INTERACTION_SESSION_REGISTRATION_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRegistrationServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE27_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_REGISTRATION_SERVICE,
        INTERACTION_SESSION_REGISTRATION_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase27-interaction-session-registration] FAILED")
        for error in errors:
            print(f"[phase27-interaction-session-registration] ERROR {error}")
        return 1

    phase27_plan_text = _read(PHASE27_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    registration_service_text = _read(INTERACTION_SESSION_REGISTRATION_SERVICE)
    registration_service_test_text = _read(INTERACTION_SESSION_REGISTRATION_SERVICE_TEST)

    if "## Phase 27 Slice Status" not in phase27_plan_text:
        errors.append("phase27_plan_missing_slice_status")
    if "`27.1` complete." not in phase27_plan_text:
        errors.append("phase27_plan_missing_27_1_complete")
    if "`27.2` complete." not in phase27_plan_text:
        errors.append("phase27_plan_missing_27_2_complete")
    if "`27.3` complete." not in phase27_plan_text:
        errors.append("phase27_plan_missing_27_3_complete")

    if "## Phase 27 (Interaction Session Registration Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase27_section")

    if "PHASE 27 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase27_started")
    if "PHASE 27 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase27_complete")

    required_tasks = [
        "- [x] Define Phase 27 interaction session registration decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session registration lifecycle ownership from `InteractionSession` into focused registration service.",
        "- [x] Run Phase 27 verification + guard pack and mark `PHASE 27 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase27_line:{task_line}")

    if "private final InteractionSessionRegistrationService interactionSessionRegistrationService;" not in interaction_session_text:
        errors.append("interaction_session_missing_registration_service_field")
    if (
        "interactionSessionRegistrationService::clearRegistration" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_clear_registration_delegate")
    if (
        "interactionSessionRegistrationService::ensureRegistered" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_ensure_registration_delegate")
    if (
        "interactionSessionRegistrationService.clearRegistration();" not in interaction_session_text
        and "interactionSessionShutdownService.shutdown();" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_shutdown_registration_clear")
    if "private SessionManager.Registration registration = null;" in interaction_session_text:
        errors.append("interaction_session_still_owns_registration_field")
    if "private void ensureRegistered()" in interaction_session_text:
        errors.append("interaction_session_still_owns_ensure_registered_method")
    if "private void clearRegistration()" in interaction_session_text:
        errors.append("interaction_session_still_owns_clear_registration_method")

    if "final class InteractionSessionRegistrationService" not in registration_service_text:
        errors.append("interaction_session_registration_service_missing_class")
    if "void ensureRegistered()" not in registration_service_text:
        errors.append("interaction_session_registration_service_missing_ensure_method")
    if "void clearRegistration()" not in registration_service_text:
        errors.append("interaction_session_registration_service_missing_clear_method")

    if "ensureRegisteredIsIdempotentAndActivatesSession" not in registration_service_test_text:
        errors.append("interaction_session_registration_test_missing_idempotent_case")
    if "clearRegistrationIsNoopWhenNotRegistered" not in registration_service_test_text:
        errors.append("interaction_session_registration_test_missing_noop_case")
    if "clearRegistrationReleasesActiveSessionAndAllowsReregister" not in registration_service_test_text:
        errors.append("interaction_session_registration_test_missing_reregister_case")

    if errors:
        print("[phase27-interaction-session-registration] FAILED")
        for error in errors:
            print(f"[phase27-interaction-session-registration] ERROR {error}")
        return 1

    print(
        "[phase27-interaction-session-registration] OK: interaction session registration Phase 27 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
