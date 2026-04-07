from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE60_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE60_INTERACTION_SESSION_MOTOR_OWNERSHIP_DELEGATE_FACTORY_EXTRACTION_PLAN.md"
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
        PHASE60_PLAN,
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
        print("[phase60-interaction-session-motor-ownership-delegate-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase60-interaction-session-motor-ownership-delegate-factory-extraction] ERROR {error}")
        return 1

    phase60_plan_text = _read(PHASE60_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    motor_ownership_factory_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY)
    motor_ownership_factory_test_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_TEST)

    if "## Phase 60 Slice Status" not in phase60_plan_text:
        errors.append("phase60_plan_missing_slice_status")
    if "`60.1` complete." not in phase60_plan_text:
        errors.append("phase60_plan_missing_60_1_complete")
    if "`60.2` complete." not in phase60_plan_text:
        errors.append("phase60_plan_missing_60_2_complete")
    if "`60.3` complete." not in phase60_plan_text:
        errors.append("phase60_plan_missing_60_3_complete")

    if "## Phase 60 (Interaction Session Motor-Ownership Delegate Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase60_section")

    if "PHASE 60 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase60_started")
    if "PHASE 60 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase60_complete")

    required_tasks = [
        "- [x] Define Phase 60 interaction session motor-ownership delegate-factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session motor-ownership delegate-host assembly into focused `InteractionSessionMotorOwnershipFactory` ownership.",
        "- [x] Run Phase 60 verification + guard pack and mark `PHASE 60 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase60_line:{task_line}")

    if "static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_delegate_host_method")
    if "InteractionSessionMotorOwnershipFactory.createMotorOwnershipHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_delegate_factory_delegation")

    if "static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHostFromDelegates(" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_delegate_host_method")
    if "InteractionSessionHostFactory.createMotorOwnershipHostFromDelegates(" in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_still_depends_on_host_factory_delegate_method")
    if "releaseInteractionMotorOwnership.run();" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_release_runnable_delegate_string")

    if "createMotorOwnershipHostFromDelegatesRoutesAcquireAndRelease" not in motor_ownership_factory_test_text:
        errors.append("interaction_session_motor_ownership_factory_test_missing_delegate_case")

    if errors:
        print("[phase60-interaction-session-motor-ownership-delegate-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase60-interaction-session-motor-ownership-delegate-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase60-interaction-session-motor-ownership-delegate-factory-extraction] OK: interaction session motor-ownership delegate factory extraction Phase 60 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
