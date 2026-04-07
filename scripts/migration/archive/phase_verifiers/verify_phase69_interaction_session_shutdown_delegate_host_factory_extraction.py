from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE69_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE69_INTERACTION_SESSION_SHUTDOWN_DELEGATE_HOST_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_SHUTDOWN_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionShutdownFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownHostTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE69_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_SHUTDOWN_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase69-interaction-session-shutdown-delegate-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase69-interaction-session-shutdown-delegate-host-factory-extraction] ERROR {error}")
        return 1

    phase69_plan_text = _read(PHASE69_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    shutdown_factory_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY)
    host_factory_shutdown_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_TEST)

    if "## Phase 69 Slice Status" not in phase69_plan_text:
        errors.append("phase69_plan_missing_slice_status")
    if "`69.1` complete." not in phase69_plan_text:
        errors.append("phase69_plan_missing_69_1_complete")
    if "`69.2` complete." not in phase69_plan_text:
        errors.append("phase69_plan_missing_69_2_complete")
    if "`69.3` complete." not in phase69_plan_text:
        errors.append("phase69_plan_missing_69_3_complete")

    if "## Phase 69 (Interaction Session Shutdown Delegate-Host Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase69_section")

    if "PHASE 69 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase69_started")
    if "PHASE 69 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase69_complete")

    required_tasks = [
        "- [x] Define Phase 69 interaction session shutdown delegate-host factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session shutdown delegate-host assembly into focused `InteractionSessionShutdownFactory` ownership.",
        "- [x] Run Phase 69 verification + guard pack and mark `PHASE 69 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase69_line:{task_line}")

    if "InteractionSessionShutdownFactory.createShutdownHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_delegate_host_factory_delegation")
    compatibility_strings = [
        "clearPendingPostClickSettle.run();",
        "clearRegistration.run();",
        "releaseInteractionMotorOwnership.run();",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_shutdown_compatibility_sentinel:{compatibility_string}")

    if "static InteractionSessionShutdownService.Host createShutdownHostFromDelegates(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_delegate_host_method")

    if "createShutdownHostFromDelegatesDelegatesAllLifecycleRunnables" not in host_factory_shutdown_test_text:
        errors.append("interaction_session_host_factory_shutdown_test_missing_delegate_host_case")

    if errors:
        print("[phase69-interaction-session-shutdown-delegate-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase69-interaction-session-shutdown-delegate-host-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase69-interaction-session-shutdown-delegate-host-factory-extraction] OK: interaction session shutdown delegate-host factory extraction Phase 69 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
