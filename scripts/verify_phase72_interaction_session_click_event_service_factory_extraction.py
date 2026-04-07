from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE72_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE72_INTERACTION_SESSION_CLICK_EVENT_SERVICE_FACTORY_EXTRACTION_PLAN.md"
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
        PHASE72_PLAN,
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
        print("[phase72-interaction-session-click-event-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase72-interaction-session-click-event-service-factory-extraction] ERROR {error}")
        return 1

    phase72_plan_text = _read(PHASE72_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    click_event_factory_text = _read(INTERACTION_SESSION_CLICK_EVENT_FACTORY)
    click_event_factory_test_text = _read(INTERACTION_SESSION_CLICK_EVENT_FACTORY_TEST)

    if "## Phase 72 Slice Status" not in phase72_plan_text:
        errors.append("phase72_plan_missing_slice_status")
    if "`72.1` complete." not in phase72_plan_text:
        errors.append("phase72_plan_missing_72_1_complete")
    if "`72.2` complete." not in phase72_plan_text:
        errors.append("phase72_plan_missing_72_2_complete")
    if "`72.3` complete." not in phase72_plan_text:
        errors.append("phase72_plan_missing_72_3_complete")

    if "## Phase 72 (Interaction Session Click-Event Service Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase72_section")

    if "PHASE 72 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase72_started")
    if "PHASE 72 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase72_complete")

    required_tasks = [
        "- [x] Define Phase 72 interaction session click-event service factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session click-event service assembly into focused `InteractionSessionClickEventFactory` ownership.",
        "- [x] Run Phase 72 verification + guard pack and mark `PHASE 72 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase72_line:{task_line}")

    if "InteractionSessionClickEventFactory.createClickEventService(onInteractionClickEvent);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_service_factory_delegation")
    compatibility_strings = [
        "InteractionSessionClickEventFactory.createClickEventServiceFromHost(host);",
        "InteractionSessionClickEventFactory.createClickEventHost(onInteractionClickEvent);",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "static InteractionSessionClickEventService createClickEventService(" not in click_event_factory_text:
        errors.append("interaction_session_click_event_factory_missing_click_event_service_method")
    if "return createClickEventServiceFromHost(" not in click_event_factory_text:
        errors.append("interaction_session_click_event_factory_missing_click_event_service_delegation")

    if "createClickEventServiceRoutesInteractionClickEventLifecycle" not in click_event_factory_test_text:
        errors.append("interaction_session_click_event_factory_test_missing_service_case")

    if errors:
        print("[phase72-interaction-session-click-event-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase72-interaction-session-click-event-service-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase72-interaction-session-click-event-service-factory-extraction] OK: interaction session click-event service factory extraction Phase 72 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
