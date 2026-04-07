from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE61_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE61_INTERACTION_SESSION_SHUTDOWN_DELEGATE_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_SHUTDOWN_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java"
)
INTERACTION_SESSION_SHUTDOWN_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionShutdownFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE61_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_SHUTDOWN_FACTORY,
        INTERACTION_SESSION_SHUTDOWN_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase61-interaction-session-shutdown-delegate-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase61-interaction-session-shutdown-delegate-factory-extraction] ERROR {error}")
        return 1

    phase61_plan_text = _read(PHASE61_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    shutdown_factory_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY)
    shutdown_factory_test_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY_TEST)

    if "## Phase 61 Slice Status" not in phase61_plan_text:
        errors.append("phase61_plan_missing_slice_status")
    if "`61.1` complete." not in phase61_plan_text:
        errors.append("phase61_plan_missing_61_1_complete")
    if "`61.2` complete." not in phase61_plan_text:
        errors.append("phase61_plan_missing_61_2_complete")
    if "`61.3` complete." not in phase61_plan_text:
        errors.append("phase61_plan_missing_61_3_complete")

    if "## Phase 61 (Interaction Session Shutdown Delegate Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase61_section")

    if "PHASE 61 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase61_started")
    if "PHASE 61 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase61_complete")

    required_tasks = [
        "- [x] Define Phase 61 interaction session shutdown delegate-factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session shutdown delegate-host assembly into focused `InteractionSessionShutdownFactory` ownership.",
        "- [x] Run Phase 61 verification + guard pack and mark `PHASE 61 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase61_line:{task_line}")

    if "static InteractionSessionShutdownService.Host createShutdownHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_delegate_host_method")
    if "clearPendingPostClickSettle.run();" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_settle_delegate_string")
    if "clearRegistration.run();" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_registration_delegate_string")
    if "releaseInteractionMotorOwnership.run();" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_release_delegate_string")

    if "static InteractionSessionShutdownService.Host createShutdownHostFromDelegates(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_delegate_host_method")
    if "return createShutdownHostFromDelegates(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_delegate_host_delegation")
    if "InteractionSessionHostFactory.createShutdownHostFromDelegates(" in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_still_depends_on_host_factory_delegate_method")

    if "createShutdownHostFromDelegatesRoutesLifecycleRunnables" not in shutdown_factory_test_text:
        errors.append("interaction_session_shutdown_factory_test_missing_delegate_case")

    if errors:
        print("[phase61-interaction-session-shutdown-delegate-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase61-interaction-session-shutdown-delegate-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase61-interaction-session-shutdown-delegate-factory-extraction] OK: interaction session shutdown delegate factory extraction Phase 61 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
