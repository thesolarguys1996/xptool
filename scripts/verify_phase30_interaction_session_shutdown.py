from __future__ import annotations

import re
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE30_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE30_INTERACTION_SESSION_SHUTDOWN_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_SHUTDOWN_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownService.java"
)
INTERACTION_SESSION_SHUTDOWN_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE30_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_SHUTDOWN_SERVICE,
        INTERACTION_SESSION_SHUTDOWN_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase30-interaction-session-shutdown] FAILED")
        for error in errors:
            print(f"[phase30-interaction-session-shutdown] ERROR {error}")
        return 1

    phase30_plan_text = _read(PHASE30_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    shutdown_service_text = _read(INTERACTION_SESSION_SHUTDOWN_SERVICE)
    shutdown_service_test_text = _read(INTERACTION_SESSION_SHUTDOWN_SERVICE_TEST)

    if "## Phase 30 Slice Status" not in phase30_plan_text:
        errors.append("phase30_plan_missing_slice_status")
    if "`30.1` complete." not in phase30_plan_text:
        errors.append("phase30_plan_missing_30_1_complete")
    if "`30.2` complete." not in phase30_plan_text:
        errors.append("phase30_plan_missing_30_2_complete")
    if "`30.3` complete." not in phase30_plan_text:
        errors.append("phase30_plan_missing_30_3_complete")

    if "## Phase 30 (Interaction Session Shutdown Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase30_section")

    if "PHASE 30 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase30_started")
    if "PHASE 30 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase30_complete")

    required_tasks = [
        "- [x] Define Phase 30 interaction session shutdown decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session shutdown lifecycle ownership from `InteractionSession` into focused service.",
        "- [x] Run Phase 30 verification + guard pack and mark `PHASE 30 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase30_line:{task_line}")

    if "private final InteractionSessionShutdownService interactionSessionShutdownService;" not in interaction_session_text:
        errors.append("interaction_session_missing_shutdown_service_field")
    if (
        "new InteractionSessionShutdownService(" not in interaction_session_text
        and "InteractionSessionHostFactory.createShutdownService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_shutdown_service_construction")
    shutdown_method_match = re.search(
        r"public void shutdown\(\)\s*\{(?P<body>.*?)\n\s*\}",
        interaction_session_text,
        flags=re.DOTALL,
    )
    if shutdown_method_match is None:
        errors.append("interaction_session_missing_shutdown_method")
    else:
        shutdown_body = shutdown_method_match.group("body")
        if "interactionSessionShutdownService.shutdown();" not in shutdown_body:
            errors.append("interaction_session_missing_shutdown_delegate")
        if "interactionPostClickSettleService.clearPendingPostClickSettle();" in shutdown_body:
            errors.append("interaction_session_still_owns_direct_shutdown_settle_clear")
        if "interactionSessionRegistrationService.clearRegistration();" in shutdown_body:
            errors.append("interaction_session_still_owns_direct_shutdown_registration_clear")
        if "interactionSessionMotorOwnershipService.releaseInteractionMotorOwnership();" in shutdown_body:
            errors.append("interaction_session_still_owns_direct_shutdown_motor_release")

    if "final class InteractionSessionShutdownService" not in shutdown_service_text:
        errors.append("interaction_session_shutdown_service_missing_class")
    if "interface Host" not in shutdown_service_text:
        errors.append("interaction_session_shutdown_service_missing_host_interface")
    if "void shutdown()" not in shutdown_service_text:
        errors.append("interaction_session_shutdown_service_missing_shutdown_method")
    if "host.clearPendingPostClickSettle();" not in shutdown_service_text:
        errors.append("interaction_session_shutdown_service_missing_settle_step")
    if "host.clearRegistration();" not in shutdown_service_text:
        errors.append("interaction_session_shutdown_service_missing_registration_step")
    if "host.releaseInteractionMotorOwnership();" not in shutdown_service_text:
        errors.append("interaction_session_shutdown_service_missing_motor_release_step")

    if "shutdownDelegatesAllLifecycleStepsInOrder" not in shutdown_service_test_text:
        errors.append("interaction_session_shutdown_test_missing_delegate_order_case")

    if errors:
        print("[phase30-interaction-session-shutdown] FAILED")
        for error in errors:
            print(f"[phase30-interaction-session-shutdown] ERROR {error}")
        return 1

    print("[phase30-interaction-session-shutdown] OK: interaction session shutdown Phase 30 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
