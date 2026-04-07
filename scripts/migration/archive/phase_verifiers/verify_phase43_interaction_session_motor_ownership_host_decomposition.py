from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE43_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE43_INTERACTION_SESSION_MOTOR_OWNERSHIP_HOST_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_SERVICE_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE43_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_SERVICE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase43-interaction-session-motor-ownership-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase43-interaction-session-motor-ownership-host-decomposition] ERROR {error}")
        return 1

    phase43_plan_text = _read(PHASE43_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    motor_ownership_service_factory_test_text = _read(
        INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_SERVICE_FACTORY_TEST
    )

    if "## Phase 43 Slice Status" not in phase43_plan_text:
        errors.append("phase43_plan_missing_slice_status")
    if "`43.1` complete." not in phase43_plan_text:
        errors.append("phase43_plan_missing_43_1_complete")
    if "`43.2` complete." not in phase43_plan_text:
        errors.append("phase43_plan_missing_43_2_complete")
    if "`43.3` complete." not in phase43_plan_text:
        errors.append("phase43_plan_missing_43_3_complete")

    if "## Phase 43 (Interaction Session Motor Ownership Host Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase43_section")

    if "PHASE 43 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase43_started")
    if "PHASE 43 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase43_complete")

    required_tasks = [
        "- [x] Define Phase 43 interaction session motor-ownership host decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session motor-ownership service host-based construction boundary in `InteractionSessionHostFactory`.",
        "- [x] Run Phase 43 verification + guard pack and mark `PHASE 43 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase43_line:{task_line}")

    if "static InteractionSessionMotorOwnershipService createMotorOwnershipServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_service_from_host_method")
    if "return createMotorOwnershipServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_ownership_service_from_host_delegation")

    if (
        "createMotorOwnershipServiceFromHostDelegatesAcquireAndReleaseLifecycle"
        not in motor_ownership_service_factory_test_text
    ):
        errors.append("interaction_session_host_factory_motor_ownership_service_test_missing_delegate_case")

    if errors:
        print("[phase43-interaction-session-motor-ownership-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase43-interaction-session-motor-ownership-host-decomposition] ERROR {error}")
        return 1

    print(
        "[phase43-interaction-session-motor-ownership-host-decomposition] OK: interaction session motor ownership host decomposition Phase 43 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
