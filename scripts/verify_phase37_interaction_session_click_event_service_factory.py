from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE37_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE37_INTERACTION_SESSION_CLICK_EVENT_SERVICE_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_SERVICE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE37_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_SERVICE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase37-interaction-session-click-event-service-factory] FAILED")
        for error in errors:
            print(f"[phase37-interaction-session-click-event-service-factory] ERROR {error}")
        return 1

    phase37_plan_text = _read(PHASE37_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    click_event_service_factory_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_SERVICE_FACTORY_TEST)

    if "## Phase 37 Slice Status" not in phase37_plan_text:
        errors.append("phase37_plan_missing_slice_status")
    if "`37.1` complete." not in phase37_plan_text:
        errors.append("phase37_plan_missing_37_1_complete")
    if "`37.2` complete." not in phase37_plan_text:
        errors.append("phase37_plan_missing_37_2_complete")
    if "`37.3` complete." not in phase37_plan_text:
        errors.append("phase37_plan_missing_37_3_complete")

    if "## Phase 37 (Interaction Session Click-Event Service Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase37_section")

    if "PHASE 37 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase37_started")
    if "PHASE 37 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase37_complete")

    required_tasks = [
        "- [x] Define Phase 37 interaction session click-event service-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session click-event service construction from `InteractionSession` into focused host-factory method.",
        "- [x] Run Phase 37 verification + guard pack and mark `PHASE 37 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase37_line:{task_line}")

    if "InteractionSessionHostFactory.createClickEventService(" not in interaction_session_text:
        errors.append("interaction_session_missing_click_event_service_factory_usage")
    if "new InteractionSessionClickEventService(" in interaction_session_text:
        errors.append("interaction_session_still_constructs_click_event_service_inline")

    if "static InteractionSessionClickEventService createClickEventService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_service_factory_method")
    if "static InteractionSessionClickEventService.Host createClickEventHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_delegate_host_factory_method")

    if "createClickEventServiceRoutesInteractionClickEvent" not in click_event_service_factory_test_text:
        errors.append("interaction_session_host_factory_click_event_service_test_missing_delegate_case")
    if "createClickEventServiceForwardsNullEvent" not in click_event_service_factory_test_text:
        errors.append("interaction_session_host_factory_click_event_service_test_missing_null_case")

    if errors:
        print("[phase37-interaction-session-click-event-service-factory] FAILED")
        for error in errors:
            print(f"[phase37-interaction-session-click-event-service-factory] ERROR {error}")
        return 1

    print(
        "[phase37-interaction-session-click-event-service-factory] OK: interaction session click-event service factory Phase 37 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
