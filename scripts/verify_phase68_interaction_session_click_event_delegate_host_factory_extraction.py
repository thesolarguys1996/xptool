from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE68_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE68_INTERACTION_SESSION_CLICK_EVENT_DELEGATE_HOST_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_CLICK_EVENT_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionClickEventFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventHostTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE68_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_CLICK_EVENT_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase68-interaction-session-click-event-delegate-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase68-interaction-session-click-event-delegate-host-factory-extraction] ERROR {error}")
        return 1

    phase68_plan_text = _read(PHASE68_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    click_event_factory_text = _read(INTERACTION_SESSION_CLICK_EVENT_FACTORY)
    host_factory_click_event_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_TEST)

    if "## Phase 68 Slice Status" not in phase68_plan_text:
        errors.append("phase68_plan_missing_slice_status")
    if "`68.1` complete." not in phase68_plan_text:
        errors.append("phase68_plan_missing_68_1_complete")
    if "`68.2` complete." not in phase68_plan_text:
        errors.append("phase68_plan_missing_68_2_complete")
    if "`68.3` complete." not in phase68_plan_text:
        errors.append("phase68_plan_missing_68_3_complete")

    if "## Phase 68 (Interaction Session Click-Event Delegate-Host Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase68_section")

    if "PHASE 68 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase68_started")
    if "PHASE 68 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase68_complete")

    required_tasks = [
        "- [x] Define Phase 68 interaction session click-event delegate-host factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session click-event delegate-host assembly into focused `InteractionSessionClickEventFactory` ownership.",
        "- [x] Run Phase 68 verification + guard pack and mark `PHASE 68 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase68_line:{task_line}")

    if "InteractionSessionClickEventFactory.createClickEventHostFromDelegates(onInteractionClickEvent);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_delegate_host_factory_delegation")
    if "onInteractionClickEvent.accept(clickEvent);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_compatibility_sentinel")

    if "static InteractionSessionClickEventService.Host createClickEventHostFromDelegates(" not in click_event_factory_text:
        errors.append("interaction_session_click_event_factory_missing_click_event_delegate_host_method")

    if "createClickEventHostFromDelegatesForwardsInteractionClickEvent" not in host_factory_click_event_test_text:
        errors.append("interaction_session_host_factory_click_event_test_missing_delegate_host_case")

    if errors:
        print("[phase68-interaction-session-click-event-delegate-host-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase68-interaction-session-click-event-delegate-host-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase68-interaction-session-click-event-delegate-host-factory-extraction] OK: interaction session click-event delegate-host factory extraction Phase 68 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
