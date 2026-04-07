from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE29_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE29_INTERACTION_SESSION_CLICK_EVENT_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_CLICK_EVENT_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventService.java"
)
INTERACTION_SESSION_CLICK_EVENT_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionClickEventServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE29_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_CLICK_EVENT_SERVICE,
        INTERACTION_SESSION_CLICK_EVENT_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase29-interaction-session-click-event] FAILED")
        for error in errors:
            print(f"[phase29-interaction-session-click-event] ERROR {error}")
        return 1

    phase29_plan_text = _read(PHASE29_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    click_event_service_text = _read(INTERACTION_SESSION_CLICK_EVENT_SERVICE)
    click_event_service_test_text = _read(INTERACTION_SESSION_CLICK_EVENT_SERVICE_TEST)

    if "## Phase 29 Slice Status" not in phase29_plan_text:
        errors.append("phase29_plan_missing_slice_status")
    if "`29.1` complete." not in phase29_plan_text:
        errors.append("phase29_plan_missing_29_1_complete")
    if "`29.2` complete." not in phase29_plan_text:
        errors.append("phase29_plan_missing_29_2_complete")
    if "`29.3` complete." not in phase29_plan_text:
        errors.append("phase29_plan_missing_29_3_complete")

    if "## Phase 29 (Interaction Session Click-Event Intake Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase29_section")

    if "PHASE 29 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase29_started")
    if "PHASE 29 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase29_complete")

    required_tasks = [
        "- [x] Define Phase 29 interaction session click-event intake decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session click-event intake ownership from `InteractionSession` into focused service.",
        "- [x] Run Phase 29 verification + guard pack and mark `PHASE 29 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase29_line:{task_line}")

    if "private final InteractionSessionClickEventService interactionSessionClickEventService;" not in interaction_session_text:
        errors.append("interaction_session_missing_click_event_service_field")
    if (
        "new InteractionSessionClickEventService(" not in interaction_session_text
        and "InteractionSessionHostFactory.createClickEventService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_click_event_service_construction")
    if "interactionPostClickSettleService::onInteractionClickEvent" not in interaction_session_text:
        errors.append("interaction_session_missing_click_event_service_host_delegate")
    if "interactionSessionClickEventService.onInteractionClickEvent(clickEvent);" not in interaction_session_text:
        errors.append("interaction_session_missing_click_event_service_runtime_delegate")
    if "interactionPostClickSettleService.onInteractionClickEvent(clickEvent);" in interaction_session_text:
        errors.append("interaction_session_still_owns_direct_click_event_delegate")

    if "final class InteractionSessionClickEventService" not in click_event_service_text:
        errors.append("interaction_session_click_event_service_missing_class")
    if "interface Host" not in click_event_service_text:
        errors.append("interaction_session_click_event_service_missing_host_interface")
    if "void onInteractionClickEvent(InteractionClickEvent clickEvent)" not in click_event_service_text:
        errors.append("interaction_session_click_event_service_missing_delegate_method")

    if "onInteractionClickEventDelegatesToHost" not in click_event_service_test_text:
        errors.append("interaction_session_click_event_test_missing_delegate_case")
    if "onInteractionClickEventForwardsNullEvent" not in click_event_service_test_text:
        errors.append("interaction_session_click_event_test_missing_null_case")

    if errors:
        print("[phase29-interaction-session-click-event] FAILED")
        for error in errors:
            print(f"[phase29-interaction-session-click-event] ERROR {error}")
        return 1

    print("[phase29-interaction-session-click-event] OK: interaction session click-event Phase 29 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
