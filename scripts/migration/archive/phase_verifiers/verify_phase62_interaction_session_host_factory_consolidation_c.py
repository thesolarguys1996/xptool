from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE62_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE62_INTERACTION_SESSION_HOST_FACTORY_CONSOLIDATION_C_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionMotorOwnershipFactory.java"
)
INTERACTION_SESSION_SHUTDOWN_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE62_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY,
        INTERACTION_SESSION_SHUTDOWN_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase62-interaction-session-host-factory-consolidation-c] FAILED")
        for error in errors:
            print(f"[phase62-interaction-session-host-factory-consolidation-c] ERROR {error}")
        return 1

    phase62_plan_text = _read(PHASE62_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    motor_ownership_factory_text = _read(INTERACTION_SESSION_MOTOR_OWNERSHIP_FACTORY)
    shutdown_factory_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY)

    if "## Phase 62 Slice Status" not in phase62_plan_text:
        errors.append("phase62_plan_missing_slice_status")
    if "`62.1` complete." not in phase62_plan_text:
        errors.append("phase62_plan_missing_62_1_complete")
    if "`62.2` complete." not in phase62_plan_text:
        errors.append("phase62_plan_missing_62_2_complete")
    if "`62.3` complete." not in phase62_plan_text:
        errors.append("phase62_plan_missing_62_3_complete")

    if "## Phase 62 (Interaction Session Host-Factory Consolidation C)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase62_section")

    if "PHASE 62 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase62_started")
    if "PHASE 62 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase62_complete")

    required_tasks = [
        "- [x] Define Phase 62 interaction session host-factory consolidation C scope and completion evidence gates.",
        "- [x] Consolidate focused-factory delegate seams for motor-ownership and shutdown factories while preserving legacy host-factory compatibility wrappers.",
        "- [x] Run Phase 62 verification + guard pack and mark `PHASE 62 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase62_line:{task_line}")

    required_host_factory_delegate_strings = [
        "InteractionSessionMotorOwnershipFactory.createMotorOwnershipHostFromDelegates(",
        "InteractionSessionShutdownFactory.createShutdownServiceFromHost(host);",
        "InteractionSessionShutdownFactory.createShutdownHost(",
    ]
    for delegate_string in required_host_factory_delegate_strings:
        if delegate_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_delegate:{delegate_string}")

    compatibility_strings = [
        "onInteractionClickEvent.accept(clickEvent);",
        "clearPendingPostClickSettle.run();",
        "clearRegistration.run();",
        "releaseInteractionMotorOwnership.run();",
        "return new InteractionSessionOwnershipService(host);",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "InteractionSessionHostFactory.createMotorOwnershipHostFromDelegates(" in motor_ownership_factory_text:
        errors.append("interaction_session_motor_ownership_factory_still_depends_on_host_factory_delegate_method")
    if "InteractionSessionHostFactory.createShutdownHostFromDelegates(" in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_still_depends_on_host_factory_delegate_method")

    if errors:
        print("[phase62-interaction-session-host-factory-consolidation-c] FAILED")
        for error in errors:
            print(f"[phase62-interaction-session-host-factory-consolidation-c] ERROR {error}")
        return 1

    print(
        "[phase62-interaction-session-host-factory-consolidation-c] OK: interaction session host-factory consolidation C Phase 62 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
