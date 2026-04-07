from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE48_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE48_INTERACTION_SESSION_SHUTDOWN_FACTORY_EXTRACTION_PLAN.md"
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
        PHASE48_PLAN,
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
        print("[phase48-interaction-session-shutdown-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase48-interaction-session-shutdown-factory-extraction] ERROR {error}")
        return 1

    phase48_plan_text = _read(PHASE48_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    shutdown_factory_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY)
    shutdown_factory_test_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY_TEST)

    if "## Phase 48 Slice Status" not in phase48_plan_text:
        errors.append("phase48_plan_missing_slice_status")
    if "`48.1` complete." not in phase48_plan_text:
        errors.append("phase48_plan_missing_48_1_complete")
    if "`48.2` complete." not in phase48_plan_text:
        errors.append("phase48_plan_missing_48_2_complete")
    if "`48.3` complete." not in phase48_plan_text:
        errors.append("phase48_plan_missing_48_3_complete")

    if "## Phase 48 (Interaction Session Shutdown Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase48_section")

    if "PHASE 48 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase48_started")
    if "PHASE 48 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase48_complete")

    required_tasks = [
        "- [x] Define Phase 48 interaction session shutdown factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session shutdown service/host assembly into focused `InteractionSessionShutdownFactory`.",
        "- [x] Run Phase 48 verification + guard pack and mark `PHASE 48 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase48_line:{task_line}")

    if "InteractionSessionShutdownFactory.createShutdownServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_factory_service_from_host_delegation")
    if "InteractionSessionShutdownFactory.createShutdownHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_factory_host_delegation")

    if "final class InteractionSessionShutdownFactory" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_class")
    if "static InteractionSessionShutdownService createShutdownService(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_service_factory_method")
    if "static InteractionSessionShutdownService createShutdownServiceFromHost(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_service_from_host_method")
    if "static InteractionSessionShutdownService.Host createShutdownHost(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_host_factory_method")

    if "createShutdownServiceFromHostRoutesShutdownLifecycle" not in shutdown_factory_test_text:
        errors.append("interaction_session_shutdown_factory_test_missing_service_delegate_case")
    if "createShutdownHostRoutesLifecycleRunnables" not in shutdown_factory_test_text:
        errors.append("interaction_session_shutdown_factory_test_missing_host_delegate_case")

    if errors:
        print("[phase48-interaction-session-shutdown-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase48-interaction-session-shutdown-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase48-interaction-session-shutdown-factory-extraction] OK: interaction session shutdown factory extraction Phase 48 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
