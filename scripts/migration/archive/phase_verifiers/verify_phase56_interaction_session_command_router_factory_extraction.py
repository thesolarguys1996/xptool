from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE56_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE56_INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_EXTRACTION_PLAN.md"
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


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE56_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_COMMAND_ROUTER_FACTORY,
        INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase56-interaction-session-command-router-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase56-interaction-session-command-router-factory-extraction] ERROR {error}")
        return 1

    phase56_plan_text = _read(PHASE56_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    command_router_factory_text = _read(INTERACTION_SESSION_COMMAND_ROUTER_FACTORY)
    command_router_factory_test_text = _read(INTERACTION_SESSION_COMMAND_ROUTER_FACTORY_TEST)

    if "## Phase 56 Slice Status" not in phase56_plan_text:
        errors.append("phase56_plan_missing_slice_status")
    if "`56.1` complete." not in phase56_plan_text:
        errors.append("phase56_plan_missing_56_1_complete")
    if "`56.2` complete." not in phase56_plan_text:
        errors.append("phase56_plan_missing_56_2_complete")
    if "`56.3` complete." not in phase56_plan_text:
        errors.append("phase56_plan_missing_56_3_complete")

    if "## Phase 56 (Interaction Session Command-Router Service Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase56_section")

    if "PHASE 56 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase56_started")
    if "PHASE 56 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase56_complete")

    required_tasks = [
        "- [x] Define Phase 56 interaction session command-router service factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session command-router service-from-host assembly into focused `InteractionSessionCommandRouterFactory`.",
        "- [x] Run Phase 56 verification + guard pack and mark `PHASE 56 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase56_line:{task_line}")

    if "static InteractionSessionCommandRouter createCommandRouterServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_service_from_host_method")
    if "InteractionSessionCommandRouterFactory.createCommandRouterServiceFromHost(host);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_service_factory_delegation")

    if "final class InteractionSessionCommandRouterFactory" not in command_router_factory_text:
        errors.append("interaction_session_command_router_factory_missing_class")
    if (
        "static InteractionSessionCommandRouter createCommandRouterServiceFromHost("
        not in command_router_factory_text
    ):
        errors.append("interaction_session_command_router_factory_missing_service_from_host_method")

    if "createCommandRouterServiceFromHostRoutesExecuteAndUnsupportedBranches" not in command_router_factory_test_text:
        errors.append("interaction_session_command_router_factory_test_missing_delegate_case")

    if errors:
        print("[phase56-interaction-session-command-router-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase56-interaction-session-command-router-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase56-interaction-session-command-router-factory-extraction] OK: interaction session command-router service factory extraction Phase 56 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
