from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE34_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE34_INTERACTION_SESSION_MOTOR_OWNERSHIP_SERVICE_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipHostTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE34_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase34-interaction-session-motor-ownership-service-factory] FAILED")
        for error in errors:
            print(f"[phase34-interaction-session-motor-ownership-service-factory] ERROR {error}")
        return 1

    phase34_plan_text = _read(PHASE34_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    motor_ownership_host_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_TEST)

    if "## Phase 34 Slice Status" not in phase34_plan_text:
        errors.append("phase34_plan_missing_slice_status")
    if "`34.1` complete." not in phase34_plan_text:
        errors.append("phase34_plan_missing_34_1_complete")
    if "`34.2` complete." not in phase34_plan_text:
        errors.append("phase34_plan_missing_34_2_complete")
    if "`34.3` complete." not in phase34_plan_text:
        errors.append("phase34_plan_missing_34_3_complete")

    if "## Phase 34 (Interaction Session Motor-Ownership Service Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase34_section")

    if "PHASE 34 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase34_started")
    if "PHASE 34 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase34_complete")

    required_tasks = [
        "- [x] Define Phase 34 interaction session motor-ownership service-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session motor-ownership service construction from `InteractionSession` into focused host-factory method.",
        "- [x] Run Phase 34 verification + guard pack and mark `PHASE 34 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase34_line:{task_line}")

    if "InteractionSessionHostFactory.createMotorOwnershipService(executor)" not in interaction_session_text:
        errors.append("interaction_session_missing_motor_ownership_service_factory_usage")
    if "new InteractionSessionMotorOwnershipService(" in interaction_session_text:
        errors.append("interaction_session_still_constructs_motor_ownership_service_inline")

    if "static InteractionSessionMotorOwnershipService createMotorOwnershipService(CommandExecutor executor)" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_service_factory_method")
    if "static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_delegate_host_factory_method")
    if "return new InteractionSessionMotorOwnershipService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_service_construction")

    if "createMotorOwnershipHostFromDelegatesRoutesAcquireAndRelease" not in motor_ownership_host_test_text:
        errors.append("interaction_session_host_factory_motor_ownership_test_missing_delegate_case")

    if errors:
        print("[phase34-interaction-session-motor-ownership-service-factory] FAILED")
        for error in errors:
            print(f"[phase34-interaction-session-motor-ownership-service-factory] ERROR {error}")
        return 1

    print(
        "[phase34-interaction-session-motor-ownership-service-factory] OK: interaction session motor-ownership service factory Phase 34 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
