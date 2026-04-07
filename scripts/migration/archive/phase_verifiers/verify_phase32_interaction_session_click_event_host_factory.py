from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE32_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE32_INTERACTION_SESSION_CLICK_EVENT_HOST_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_HOST_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE32_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_HOST_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase32-interaction-session-click-event-host-factory] FAILED")
        for error in errors:
            print(f"[phase32-interaction-session-click-event-host-factory] ERROR {error}")
        return 1

    phase32_plan_text = _read(PHASE32_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    click_event_host_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_HOST_TEST)

    if "## Phase 32 Slice Status" not in phase32_plan_text:
        errors.append("phase32_plan_missing_slice_status")
    if "`32.1` complete." not in phase32_plan_text:
        errors.append("phase32_plan_missing_32_1_complete")
    if "`32.2` complete." not in phase32_plan_text:
        errors.append("phase32_plan_missing_32_2_complete")
    if "`32.3` complete." not in phase32_plan_text:
        errors.append("phase32_plan_missing_32_3_complete")

    if "## Phase 32 (Interaction Session Click-Event Host-Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase32_section")

    if "PHASE 32 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase32_started")
    if "PHASE 32 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase32_complete")

    required_tasks = [
        "- [x] Define Phase 32 interaction session click-event host-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session click-event host wiring from `InteractionSession` constructor into focused host-factory method.",
        "- [x] Run Phase 32 verification + guard pack and mark `PHASE 32 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase32_line:{task_line}")

    if (
        "InteractionSessionHostFactory.createClickEventHost(" not in interaction_session_text
        and "InteractionSessionHostFactory.createClickEventService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_click_event_host_factory_usage")

    if "static InteractionSessionClickEventService.Host createClickEventHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_host_factory_method")
    if "onInteractionClickEvent.accept(clickEvent);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_consumer_delegate")

    if "createClickEventHostDelegatesInteractionClickEvent" not in click_event_host_test_text:
        errors.append("interaction_session_host_factory_click_event_test_missing_delegate_case")
    if "createClickEventHostForwardsNullEvent" not in click_event_host_test_text:
        errors.append("interaction_session_host_factory_click_event_test_missing_null_case")

    if errors:
        print("[phase32-interaction-session-click-event-host-factory] FAILED")
        for error in errors:
            print(f"[phase32-interaction-session-click-event-host-factory] ERROR {error}")
        return 1

    print(
        "[phase32-interaction-session-click-event-host-factory] OK: interaction session click-event host-factory Phase 32 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
