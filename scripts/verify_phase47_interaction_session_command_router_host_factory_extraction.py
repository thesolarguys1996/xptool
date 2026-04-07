from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE47_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE47_INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactory.java"
)
INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionCommandRouterHostFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE47_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY,
        INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase47-interaction-session-command-router-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase47-interaction-session-command-router-host-factory-extraction] ERROR {error}")
        return 1

    phase47_plan_text = _read(PHASE47_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    command_router_host_factory_text = _read(INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY)
    command_router_host_factory_test_text = _read(INTERACTION_SESSION_COMMAND_ROUTER_HOST_FACTORY_TEST)

    if "## Phase 47 Slice Status" not in phase47_plan_text:
        errors.append("phase47_plan_missing_slice_status")
    if "`47.1` complete." not in phase47_plan_text:
        errors.append("phase47_plan_missing_47_1_complete")
    if "`47.2` complete." not in phase47_plan_text:
        errors.append("phase47_plan_missing_47_2_complete")
    if "`47.3` complete." not in phase47_plan_text:
        errors.append("phase47_plan_missing_47_3_complete")

    if "## Phase 47 (Interaction Session Command Router Host-Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase47_section")

    if "PHASE 47 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase47_started")
    if "PHASE 47 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase47_complete")

    required_tasks = [
        "- [x] Define Phase 47 interaction session command-router host-factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session command-router host delegate assembly into focused `InteractionSessionCommandRouterHostFactory`.",
        "- [x] Run Phase 47 verification + guard pack and mark `PHASE 47 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase47_line:{task_line}")

    if "static InteractionSessionCommandRouter.Host createCommandRouterHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_host_from_delegates_signature")
    if "InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_host_factory_delegation")

    if "final class InteractionSessionCommandRouterHostFactory" not in command_router_host_factory_text:
        errors.append("interaction_session_command_router_host_factory_missing_class")
    if "static InteractionSessionCommandRouter.Host createCommandRouterHostFromDelegates(" not in command_router_host_factory_text:
        errors.append("interaction_session_command_router_host_factory_missing_delegate_constructor")

    if "createCommandRouterHostFromDelegatesRoutesAllCommandBranches" not in command_router_host_factory_test_text:
        errors.append("interaction_session_command_router_host_factory_test_missing_delegate_case")

    if errors:
        print("[phase47-interaction-session-command-router-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase47-interaction-session-command-router-host-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase47-interaction-session-command-router-host-factory-extraction] OK: interaction session command-router host-factory extraction Phase 47 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
