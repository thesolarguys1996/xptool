from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE28_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE28_INTERACTION_SESSION_MOTOR_OWNERSHIP_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipService.java"
)
INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE28_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE,
        INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase28-interaction-session-motor-ownership] FAILED")
        for error in errors:
            print(f"[phase28-interaction-session-motor-ownership] ERROR {error}")
        return 1

    phase28_plan_text = _read(PHASE28_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    motor_ownership_service_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE)
    motor_ownership_service_test_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE_TEST)

    if "## Phase 28 Slice Status" not in phase28_plan_text:
        errors.append("phase28_plan_missing_slice_status")
    if "`28.1` complete." not in phase28_plan_text:
        errors.append("phase28_plan_missing_28_1_complete")
    if "`28.2` complete." not in phase28_plan_text:
        errors.append("phase28_plan_missing_28_2_complete")
    if "`28.3` complete." not in phase28_plan_text:
        errors.append("phase28_plan_missing_28_3_complete")

    if "## Phase 28 (Interaction Session Motor-Ownership Adapter Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase28_section")

    if "PHASE 28 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase28_started")
    if "PHASE 28 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase28_complete")

    required_tasks = [
        "- [x] Define Phase 28 interaction session motor-ownership adapter decomposition scope and completion evidence gates.",
        "- [x] Extract remaining interaction-session motor-ownership adapter ownership into focused service.",
        "- [x] Run Phase 28 verification + guard pack and mark `PHASE 28 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase28_line:{task_line}")

    if "private final InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService;" not in interaction_session_text:
        errors.append("interaction_session_missing_motor_ownership_service_field")
    if (
        "InteractionSessionHostFactory.createMotorOwnershipHost(executor)" not in interaction_session_text
        and "InteractionSessionHostFactory.createMotorOwnershipService(executor)" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_motor_ownership_host_factory_usage")
    if (
        "interactionSessionMotorOwnershipService::releaseInteractionMotorOwnership" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_release_delegate")
    if (
        "interactionSessionMotorOwnershipService::acquireOrRenewInteractionMotorOwnership" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_acquire_delegate")
    if (
        "interactionSessionMotorOwnershipService.releaseInteractionMotorOwnership();" not in interaction_session_text
        and "interactionSessionShutdownService.shutdown();" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_shutdown_release_delegate")
    if "private boolean acquireOrRenewMotorOwnership()" in interaction_session_text:
        errors.append("interaction_session_still_owns_motor_ownership_adapter_method")

    if "static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHost(CommandExecutor executor)" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_host_factory")
    if "releaseInteractionMotorOwnership.run();" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_release_runnable_delegate")

    if "final class InteractionSessionMotorOwnershipService" not in motor_ownership_service_text:
        errors.append("interaction_session_motor_ownership_service_missing_class")
    if "boolean acquireOrRenewInteractionMotorOwnership()" not in motor_ownership_service_text:
        errors.append("interaction_session_motor_ownership_service_missing_acquire_method")
    if "void releaseInteractionMotorOwnership()" not in motor_ownership_service_text:
        errors.append("interaction_session_motor_ownership_service_missing_release_method")

    if "acquireOrRenewDelegatesToHostResult" not in motor_ownership_service_test_text:
        errors.append("interaction_session_motor_ownership_test_missing_acquire_case")
    if "releaseDelegatesToHost" not in motor_ownership_service_test_text:
        errors.append("interaction_session_motor_ownership_test_missing_release_case")

    if errors:
        print("[phase28-interaction-session-motor-ownership] FAILED")
        for error in errors:
            print(f"[phase28-interaction-session-motor-ownership] ERROR {error}")
        return 1

    print(
        "[phase28-interaction-session-motor-ownership] OK: interaction session motor-ownership Phase 28 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
