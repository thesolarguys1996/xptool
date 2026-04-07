from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE38_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE38_INTERACTION_SESSION_SHUTDOWN_SERVICE_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_SERVICE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryShutdownServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE38_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_SERVICE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase38-interaction-session-shutdown-service-factory] FAILED")
        for error in errors:
            print(f"[phase38-interaction-session-shutdown-service-factory] ERROR {error}")
        return 1

    phase38_plan_text = _read(PHASE38_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    shutdown_service_factory_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_SHUTDOWN_SERVICE_FACTORY_TEST)

    if "## Phase 38 Slice Status" not in phase38_plan_text:
        errors.append("phase38_plan_missing_slice_status")
    if "`38.1` complete." not in phase38_plan_text:
        errors.append("phase38_plan_missing_38_1_complete")
    if "`38.2` complete." not in phase38_plan_text:
        errors.append("phase38_plan_missing_38_2_complete")
    if "`38.3` complete." not in phase38_plan_text:
        errors.append("phase38_plan_missing_38_3_complete")

    if "## Phase 38 (Interaction Session Shutdown Service Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase38_section")

    if "PHASE 38 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase38_started")
    if "PHASE 38 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase38_complete")

    required_tasks = [
        "- [x] Define Phase 38 interaction session shutdown service-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session shutdown service construction from `InteractionSession` into focused host-factory method.",
        "- [x] Run Phase 38 verification + guard pack and mark `PHASE 38 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase38_line:{task_line}")

    if "InteractionSessionHostFactory.createShutdownService(" not in interaction_session_text:
        errors.append("interaction_session_missing_shutdown_service_factory_usage")
    if "new InteractionSessionShutdownService(" in interaction_session_text:
        errors.append("interaction_session_still_constructs_shutdown_service_inline")

    if "static InteractionSessionShutdownService createShutdownService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_service_factory_method")
    if "static InteractionSessionShutdownService.Host createShutdownHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_delegate_host_factory_method")

    if "createShutdownServiceDelegatesAllLifecycleRunnables" not in shutdown_service_factory_test_text:
        errors.append("interaction_session_host_factory_shutdown_service_test_missing_delegate_case")

    if errors:
        print("[phase38-interaction-session-shutdown-service-factory] FAILED")
        for error in errors:
            print(f"[phase38-interaction-session-shutdown-service-factory] ERROR {error}")
        return 1

    print(
        "[phase38-interaction-session-shutdown-service-factory] OK: interaction session shutdown service factory Phase 38 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
