from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE81_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE81_INTERACTION_SESSION_COMMAND_ROUTER_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_COMMAND_ROUTER_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouterFactory.java"
)
INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterFactoryTest.java"
)
INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE81_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_COMMAND_ROUTER_FACTORY,
        INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_TEST,
        INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase81-interaction-session-command-router-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase81-interaction-session-command-router-service-composite-factory-extraction] ERROR {error}")
        return 1

    phase81_plan_text = _read(PHASE81_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    command_router_factory_text = _read(INTERACTION_SESSION_COMMAND_ROUTER_FACTORY)
    command_router_factory_test_text = _read(INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_TEST)
    host_factory_command_router_service_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_SERVICE_TEST)

    if "## Phase 81 Slice Status" not in phase81_plan_text:
        errors.append("phase81_plan_missing_slice_status")
    if "`81.1` complete." not in phase81_plan_text:
        errors.append("phase81_plan_missing_81_1_complete")
    if "`81.2` complete." not in phase81_plan_text:
        errors.append("phase81_plan_missing_81_2_complete")
    if "`81.3` complete." not in phase81_plan_text:
        errors.append("phase81_plan_missing_81_3_complete")

    if "## Phase 81 (Interaction Session Command-Router Service Composite Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase81_section")

    if "PHASE 81 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase81_started")
    if "PHASE 81 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase81_complete")

    required_tasks = [
        "- [x] Define Phase 81 interaction session command-router service composite factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session command-router composite service assembly into focused `InteractionSessionCommandRouterFactory` ownership.",
        "- [x] Run Phase 81 verification + guard pack and mark `PHASE 81 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase81_line:{task_line}")

    if "InteractionSessionCommandRouterFactory.createCommandRouterService(commandFacade);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_composite_service_factory_delegation")

    compatibility_strings = [
        "return createCommandRouterServiceFromHost(createCommandRouterHost(commandFacade));",
        "InteractionSessionCommandRouterFactory.createCommandRouterServiceFromHost(host);",
        "InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "static InteractionSessionCommandRouter createCommandRouterService(SessionCommandFacade commandFacade)" not in command_router_factory_text:
        errors.append("interaction_session_command_router_factory_missing_composite_service_method")
    if "return createCommandRouterServiceFromHost(" not in command_router_factory_text:
        errors.append("interaction_session_command_router_factory_missing_composite_service_delegation")
    if "static InteractionSessionCommandRouter.Host createCommandRouterHostFromDelegates(" not in command_router_factory_text:
        errors.append("interaction_session_command_router_factory_missing_delegate_host_factory_method")

    if "createCommandRouterServiceRoutesUnsupportedBranchFromFacade" not in command_router_factory_test_text:
        errors.append("interaction_session_command_router_factory_test_missing_composite_service_case")
    if "createCommandRouterServiceRoutesUnsupportedBranchFromFacade" not in host_factory_command_router_service_test_text:
        errors.append("interaction_session_host_factory_command_router_service_test_missing_composite_service_case")

    if errors:
        print("[phase81-interaction-session-command-router-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase81-interaction-session-command-router-service-composite-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase81-interaction-session-command-router-service-composite-factory-extraction] OK: interaction session command-router service composite factory extraction Phase 81 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
