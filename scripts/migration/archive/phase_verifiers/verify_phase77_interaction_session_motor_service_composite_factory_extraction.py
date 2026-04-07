from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE77_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE77_INTERACTION_SESSION_MOTOR_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md"
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
INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE77_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY,
        INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_TEST,
        INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase77-interaction-session-motor-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase77-interaction-session-motor-service-composite-factory-extraction] ERROR {error}")
        return 1

    phase77_plan_text = _read(PHASE77_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    motor_ownership_factory_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY)
    motor_ownership_factory_test_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY_TEST)
    host_factory_motor_ownership_service_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_MOTOR_OWNERSHIP_SERVICE_TEST)

    if "## Phase 77 Slice Status" not in phase77_plan_text:
        errors.append("phase77_plan_missing_slice_status")
    if "`77.1` complete." not in phase77_plan_text:
        errors.append("phase77_plan_missing_77_1_complete")
    if "`77.2` complete." not in phase77_plan_text:
        errors.append("phase77_plan_missing_77_2_complete")
    if "`77.3` complete." not in phase77_plan_text:
        errors.append("phase77_plan_missing_77_3_complete")

    if "## Phase 77 (Interaction Session Motor Service Composite Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase77_section")

    if "PHASE 77 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase77_started")
    if "PHASE 77 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase77_complete")

    required_tasks = [
        "- [x] Define Phase 77 interaction session motor service composite factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session motor composite service assembly into focused `InteractionSessionMotorOwnershipFactory` ownership.",
        "- [x] Run Phase 77 verification + guard pack and mark `PHASE 77 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase77_line:{task_line}")

    if "InteractionSessionMotorOwnershipFactory.createMotorOwnershipService(executor);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_motor_composite_service_factory_delegation")

    compatibility_strings = [
        "return createMotorOwnershipServiceFromHost(",
        "createMotorOwnershipHost(executor)",
        "return new InteractionSessionMotorOwnershipService(host);",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "static InteractionSessionMotorOwnershipService createMotorOwnershipService(CommandExecutor executor)" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_composite_service_method")
    if "return createMotorOwnershipServiceFromHost(" not in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_missing_composite_service_delegation")

    if "createMotorOwnershipServiceFromHostDelegatesAcquireAndReleaseLifecycle" not in motor_ownership_factory_test_text:
        errors.append("interaction_session_motor_ownership_factory_test_missing_service_from_host_case")
    if "createMotorOwnershipServiceFromHostDelegatesAcquireAndReleaseLifecycle" not in host_factory_motor_ownership_service_test_text:
        errors.append("interaction_session_host_factory_motor_ownership_service_test_missing_service_from_host_case")

    if errors:
        print("[phase77-interaction-session-motor-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase77-interaction-session-motor-service-composite-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase77-interaction-session-motor-service-composite-factory-extraction] OK: interaction session motor service composite factory extraction Phase 77 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
