from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE73_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE73_INTERACTION_SESSION_SHUTDOWN_SERVICE_FACTORY_EXTRACTION_PLAN.md"
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
        PHASE73_PLAN,
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
        print("[phase73-interaction-session-shutdown-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase73-interaction-session-shutdown-service-factory-extraction] ERROR {error}")
        return 1

    phase73_plan_text = _read(PHASE73_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    shutdown_factory_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY)
    shutdown_factory_test_text = _read(INTERACTION_SESSION_SHUTDOWN_FACTORY_TEST)

    if "## Phase 73 Slice Status" not in phase73_plan_text:
        errors.append("phase73_plan_missing_slice_status")
    if "`73.1` complete." not in phase73_plan_text:
        errors.append("phase73_plan_missing_73_1_complete")
    if "`73.2` complete." not in phase73_plan_text:
        errors.append("phase73_plan_missing_73_2_complete")
    if "`73.3` complete." not in phase73_plan_text:
        errors.append("phase73_plan_missing_73_3_complete")

    if "## Phase 73 (Interaction Session Shutdown Service Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase73_section")

    if "PHASE 73 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase73_started")
    if "PHASE 73 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase73_complete")

    required_tasks = [
        "- [x] Define Phase 73 interaction session shutdown service factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session shutdown service assembly into focused `InteractionSessionShutdownFactory` ownership.",
        "- [x] Run Phase 73 verification + guard pack and mark `PHASE 73 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase73_line:{task_line}")

    if "InteractionSessionShutdownFactory.createShutdownService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_shutdown_service_factory_delegation")
    compatibility_strings = [
        "InteractionSessionShutdownFactory.createShutdownServiceFromHost(host);",
        "InteractionSessionShutdownFactory.createShutdownHost(",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "static InteractionSessionShutdownService createShutdownService(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_shutdown_service_method")
    if "return createShutdownServiceFromHost(" not in shutdown_factory_text:
        errors.append("interaction_session_shutdown_factory_missing_shutdown_service_delegation")

    if "createShutdownServiceRoutesShutdownLifecycle" not in shutdown_factory_test_text:
        errors.append("interaction_session_shutdown_factory_test_missing_service_case")

    if errors:
        print("[phase73-interaction-session-shutdown-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase73-interaction-session-shutdown-service-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase73-interaction-session-shutdown-service-factory-extraction] OK: interaction session shutdown service factory extraction Phase 73 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
