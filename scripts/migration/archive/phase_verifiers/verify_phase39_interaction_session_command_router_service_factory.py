from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE39_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE39_INTERACTION_SESSION_COMMAND_ROUTER_SERVICE_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_SERVICE_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryCommandRouterServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE39_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_SERVICE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase39-interaction-session-command-router-service-factory] FAILED")
        for error in errors:
            print(f"[phase39-interaction-session-command-router-service-factory] ERROR {error}")
        return 1

    phase39_plan_text = _read(PHASE39_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    command_router_service_factory_test_text = _read(
        INTERACTION_SESSION_HOST_FACTORY_COMMAND_ROUTER_SERVICE_FACTORY_TEST
    )

    if "## Phase 39 Slice Status" not in phase39_plan_text:
        errors.append("phase39_plan_missing_slice_status")
    if "`39.1` complete." not in phase39_plan_text:
        errors.append("phase39_plan_missing_39_1_complete")
    if "`39.2` complete." not in phase39_plan_text:
        errors.append("phase39_plan_missing_39_2_complete")
    if "`39.3` complete." not in phase39_plan_text:
        errors.append("phase39_plan_missing_39_3_complete")

    if "## Phase 39 (Interaction Session Command Router Service Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase39_section")

    if "PHASE 39 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase39_started")
    if "PHASE 39 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase39_complete")

    required_tasks = [
        "- [x] Define Phase 39 interaction session command router service-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session command router service construction from `InteractionSession` into focused host-factory method.",
        "- [x] Run Phase 39 verification + guard pack and mark `PHASE 39 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase39_line:{task_line}")

    if "InteractionSessionHostFactory.createCommandRouterService(" not in interaction_session_text:
        errors.append("interaction_session_missing_command_router_service_factory_usage")
    if "new InteractionSessionCommandRouter(" in interaction_session_text:
        errors.append("interaction_session_still_constructs_command_router_inline")

    if "static InteractionSessionCommandRouter createCommandRouterService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_service_factory_method")
    if "static InteractionSessionCommandRouter createCommandRouterServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_command_router_service_from_host_method")

    if "createCommandRouterServiceFromHostRoutesExecuteAndUnsupportedBranches" not in command_router_service_factory_test_text:
        errors.append("interaction_session_host_factory_command_router_service_test_missing_delegate_case")

    if errors:
        print("[phase39-interaction-session-command-router-service-factory] FAILED")
        for error in errors:
            print(f"[phase39-interaction-session-command-router-service-factory] ERROR {error}")
        return 1

    print(
        "[phase39-interaction-session-command-router-service-factory] OK: interaction session command router service factory Phase 39 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
