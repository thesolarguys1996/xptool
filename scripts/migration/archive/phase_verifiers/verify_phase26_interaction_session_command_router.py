from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE26_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE26_INTERACTION_SESSION_COMMAND_ROUTER_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_COMMAND_ROUTER = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouter.java"
)
INTERACTION_SESSION_COMMAND_ROUTER_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE26_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_COMMAND_ROUTER,
        INTERACTION_SESSION_COMMAND_ROUTER_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase26-interaction-session-command-router] FAILED")
        for error in errors:
            print(f"[phase26-interaction-session-command-router] ERROR {error}")
        return 1

    phase26_plan_text = _read(PHASE26_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    command_router_text = _read(INTERACTION_SESSION_COMMAND_ROUTER)
    command_router_test_text = _read(INTERACTION_SESSION_COMMAND_ROUTER_TEST)

    if "## Phase 26 Slice Status" not in phase26_plan_text:
        errors.append("phase26_plan_missing_slice_status")
    if "`26.1` complete." not in phase26_plan_text:
        errors.append("phase26_plan_missing_26_1_complete")
    if "`26.2` complete." not in phase26_plan_text:
        errors.append("phase26_plan_missing_26_2_complete")
    if "`26.3` complete." not in phase26_plan_text:
        errors.append("phase26_plan_missing_26_3_complete")

    if "## Phase 26 (Interaction Session Command Router Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase26_section")

    if "PHASE 26 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase26_started")
    if "PHASE 26 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase26_complete")

    required_tasks = [
        "- [x] Define Phase 26 interaction session command router decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session command support/dispatch ownership from `InteractionSession` into focused router service.",
        "- [x] Run Phase 26 verification + guard pack and mark `PHASE 26 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase26_line:{task_line}")

    if "private final InteractionSessionCommandRouter interactionSessionCommandRouter;" not in interaction_session_text:
        errors.append("interaction_session_missing_command_router_field")
    if "interactionSessionCommandRouter.supports(commandType);" not in interaction_session_text:
        errors.append("interaction_session_missing_supports_delegate")
    if "interactionSessionCommandRouter.execute(commandType, payload, motionProfile);" not in interaction_session_text:
        errors.append("interaction_session_missing_execute_delegate")
    if 'return "WOODCUT_CHOP_NEAREST_TREE_SAFE".equals(commandType)' in interaction_session_text:
        errors.append("interaction_session_still_owns_supports_chain")
    if "switch (commandType)" in interaction_session_text:
        errors.append("interaction_session_still_owns_execute_switch")

    if "static InteractionSessionCommandRouter.Host createCommandRouterHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_host_factory")
    if "commandFacade.executeWoodcutChopNearestTree(payload, motionProfile);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_woodcut_delegate")

    if "final class InteractionSessionCommandRouter" not in command_router_text:
        errors.append("interaction_session_command_router_missing_class")
    if "interface Host" not in command_router_text:
        errors.append("interaction_session_command_router_missing_host_interface")
    if "boolean supports(String commandType)" not in command_router_text:
        errors.append("interaction_session_command_router_missing_supports")
    if "CommandExecutor.CommandDecision execute(String commandType, JsonObject payload, MotionProfile motionProfile)" not in command_router_text:
        errors.append("interaction_session_command_router_missing_execute")
    if "return host.rejectUnsupportedCommandType();" not in command_router_text:
        errors.append("interaction_session_command_router_missing_unsupported_delegate")

    if "supportsRecognizedInteractionCommandTypes" not in command_router_test_text:
        errors.append("interaction_session_command_router_test_missing_supports_case")
    if "executeRoutesSkillingCommandsWithPayloadAndMotionProfile" not in command_router_test_text:
        errors.append("interaction_session_command_router_test_missing_skilling_route_case")
    if "executeRoutesCameraAndUnsupportedCommandsWithoutMotionProfileDependency" not in command_router_test_text:
        errors.append("interaction_session_command_router_test_missing_camera_unsupported_case")

    if errors:
        print("[phase26-interaction-session-command-router] FAILED")
        for error in errors:
            print(f"[phase26-interaction-session-command-router] ERROR {error}")
        return 1

    print(
        "[phase26-interaction-session-command-router] OK: interaction session command router Phase 26 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
