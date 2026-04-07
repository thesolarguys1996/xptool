from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE50_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE50_INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java"
)
INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE50_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY,
        INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase50-interaction-session-motor-ownership-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase50-interaction-session-motor-ownership-factory-extraction] ERROR {error}")
        return 1

    phase50_plan_text = _read(PHASE50_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    motor_ownership_factory_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY)
    motor_ownership_factory_test_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_TEST)

    if "## Phase 50 Slice Status" not in phase50_plan_text:
        errors.append("phase50_plan_missing_slice_status")
    if "`50.1` complete." not in phase50_plan_text:
        errors.append("phase50_plan_missing_50_1_complete")
    if "`50.2` complete." not in phase50_plan_text:
        errors.append("phase50_plan_missing_50_2_complete")
    if "`50.3` complete." not in phase50_plan_text:
        errors.append("phase50_plan_missing_50_3_complete")

    if "## Phase 50 (Interaction Session Motor-Ownership Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase50_section")

    if "PHASE 50 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase50_started")
    if "PHASE 50 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase50_complete")

    required_tasks = [
        "- [x] Define Phase 50 interaction session motor-ownership factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session motor-ownership host assembly into focused `InteractionSessionMotorOwnershipFactory`.",
        "- [x] Run Phase 50 verification + guard pack and mark `PHASE 50 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase50_line:{task_line}")

    if "static InteractionSessionMotorOwnershipService createMotorOwnershipServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_service_from_host_method")
    if "return createMotorOwnershipServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_service_from_host_delegation")
    if "InteractionSessionMotorOwnershipFactory.createMotorOwnershipHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_factory_host_delegation")
    if "releaseInteractionMotorOwnership.run();" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_legacy_release_delegate_string")

    if "final class InteractionSessionMotorOwnershipFactory" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_class")
    if "static InteractionSessionMotorOwnershipService createMotorOwnershipService(" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_service_factory_method")
    if "static InteractionSessionMotorOwnershipService createMotorOwnershipServiceFromHost(" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_service_from_host_method")
    if "static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHost(" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_host_factory_method")

    if "createMotorOwnershipServiceFromHostDelegatesAcquireAndReleaseLifecycle" not in motor_ownership_factory_test_text:
        errors.append("interaction_session_motor_ownership_factory_test_missing_delegate_case")

    if errors:
        print("[phase50-interaction-session-motor-ownership-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase50-interaction-session-motor-ownership-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase50-interaction-session-motor-ownership-factory-extraction] OK: interaction session motor-ownership factory extraction Phase 50 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
