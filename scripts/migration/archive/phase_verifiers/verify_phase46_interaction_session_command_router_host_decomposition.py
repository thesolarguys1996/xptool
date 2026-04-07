from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE46_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE46_INTERACTION_SESSION_COMMAND_ROUTER_HOST_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_HOST_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterHostDecompositionTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE46_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_HOST_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase46-interaction-session-command-router-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase46-interaction-session-command-router-host-decomposition] ERROR {error}")
        return 1

    phase46_plan_text = _read(PHASE46_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    command_router_host_test_text = _read(
        INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_HOST_TEST
    )

    if "## Phase 46 Slice Status" not in phase46_plan_text:
        errors.append("phase46_plan_missing_slice_status")
    if "`46.1` complete." not in phase46_plan_text:
        errors.append("phase46_plan_missing_46_1_complete")
    if "`46.2` complete." not in phase46_plan_text:
        errors.append("phase46_plan_missing_46_2_complete")
    if "`46.3` complete." not in phase46_plan_text:
        errors.append("phase46_plan_missing_46_3_complete")

    if "## Phase 46 (Interaction Session Command Router Host Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase46_section")

    if "PHASE 46 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase46_started")
    if "PHASE 46 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase46_complete")

    required_tasks = [
        "- [x] Define Phase 46 interaction session command-router host decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session command-router host delegate construction boundary in `InteractionSessionHostFactory`.",
        "- [x] Run Phase 46 verification + guard pack and mark `PHASE 46 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase46_line:{task_line}")

    if "static InteractionSessionCommandRouter.Host createCommandRouterHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_host_from_delegates_method")
    if "return createCommandRouterHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_host_from_delegates_delegation")

    if (
        "createCommandRouterHostFromDelegatesRoutesAllCommandBranches"
        not in command_router_host_test_text
    ):
        errors.append("interaction_session_host_factory_command_router_host_test_missing_delegate_case")

    if errors:
        print("[phase46-interaction-session-command-router-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase46-interaction-session-command-router-host-decomposition] ERROR {error}")
        return 1

    print(
        "[phase46-interaction-session-command-router-host-decomposition] OK: interaction session command router host decomposition Phase 46 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
