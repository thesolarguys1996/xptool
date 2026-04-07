from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE57_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE57_INTERACTION_SESSION_CLICK_EVENT_HOST_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_CLICK_EVENT_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventFactory.java"
)
INTERACTION_SESSION_CLICK_EVENT_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE57_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_CLICK_EVENT_FACTORY,
        INTERACTION_SESSION_CLICK_EVENT_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase57-interaction-session-click-event-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase57-interaction-session-click-event-host-factory-extraction] ERROR {error}")
        return 1

    phase57_plan_text = _read(PHASE57_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    click_event_factory_text = _read(INTERACTION_SESSION_CLICK_EVENT_FACTORY)
    click_event_factory_test_text = _read(INTERACTION_SESSION_CLICK_EVENT_FACTORY_TEST)

    if "## Phase 57 Slice Status" not in phase57_plan_text:
        errors.append("phase57_plan_missing_slice_status")
    if "`57.1` complete." not in phase57_plan_text:
        errors.append("phase57_plan_missing_57_1_complete")
    if "`57.2` complete." not in phase57_plan_text:
        errors.append("phase57_plan_missing_57_2_complete")
    if "`57.3` complete." not in phase57_plan_text:
        errors.append("phase57_plan_missing_57_3_complete")

    if "## Phase 57 (Interaction Session Click-Event Host Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase57_section")

    if "PHASE 57 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase57_started")
    if "PHASE 57 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase57_complete")

    required_tasks = [
        "- [x] Define Phase 57 interaction session click-event host factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session click-event host assembly into focused `InteractionSessionClickEventFactory`.",
        "- [x] Run Phase 57 verification + guard pack and mark `PHASE 57 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase57_line:{task_line}")

    if "InteractionSessionClickEventFactory.createClickEventHost(onInteractionClickEvent);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_host_factory_delegation")
    if "onInteractionClickEvent.accept(clickEvent);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_legacy_delegate_string")

    if "static InteractionSessionClickEventService.Host createClickEventHost(" not in click_event_factory_text:
        errors.append("interaction_session_click_event_factory_missing_click_event_host_method")
    if "static InteractionSessionClickEventService.Host createClickEventHostFromDelegates(" not in click_event_factory_text:
        errors.append("interaction_session_click_event_factory_missing_click_event_delegate_host_method")
    if "onInteractionClickEvent.accept(clickEvent);" not in click_event_factory_text:
        errors.append("interaction_session_click_event_factory_missing_click_event_delegate_string")

    if "createClickEventHostRoutesInteractionClickEvent" not in click_event_factory_test_text:
        errors.append("interaction_session_click_event_factory_test_missing_host_delegate_case")

    if errors:
        print("[phase57-interaction-session-click-event-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase57-interaction-session-click-event-host-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase57-interaction-session-click-event-host-factory-extraction] OK: interaction session click-event host factory extraction Phase 57 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
