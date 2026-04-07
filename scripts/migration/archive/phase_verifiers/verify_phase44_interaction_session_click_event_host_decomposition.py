from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE44_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE44_INTERACTION_SESSION_CLICK_EVENT_HOST_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_SERVICE_HOST_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryClickEventServiceHostDecompositionTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE44_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_SERVICE_HOST_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase44-interaction-session-click-event-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase44-interaction-session-click-event-host-decomposition] ERROR {error}")
        return 1

    phase44_plan_text = _read(PHASE44_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    click_event_service_host_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_CLICK_EVENT_SERVICE_HOST_TEST)

    if "## Phase 44 Slice Status" not in phase44_plan_text:
        errors.append("phase44_plan_missing_slice_status")
    if "`44.1` complete." not in phase44_plan_text:
        errors.append("phase44_plan_missing_44_1_complete")
    if "`44.2` complete." not in phase44_plan_text:
        errors.append("phase44_plan_missing_44_2_complete")
    if "`44.3` complete." not in phase44_plan_text:
        errors.append("phase44_plan_missing_44_3_complete")

    if "## Phase 44 (Interaction Session Click-Event Host Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase44_section")

    if "PHASE 44 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase44_started")
    if "PHASE 44 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase44_complete")

    required_tasks = [
        "- [x] Define Phase 44 interaction session click-event host decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session click-event service host-based construction boundary in `InteractionSessionHostFactory`.",
        "- [x] Run Phase 44 verification + guard pack and mark `PHASE 44 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase44_line:{task_line}")

    if "static InteractionSessionClickEventService createClickEventServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_service_from_host_method")
    if "return createClickEventServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_click_event_service_from_host_delegation")

    if (
        "createClickEventServiceFromHostRoutesInteractionClickEventLifecycle"
        not in click_event_service_host_test_text
    ):
        errors.append("interaction_session_host_factory_click_event_service_host_test_missing_delegate_case")

    if errors:
        print("[phase44-interaction-session-click-event-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase44-interaction-session-click-event-host-decomposition] ERROR {error}")
        return 1

    print(
        "[phase44-interaction-session-click-event-host-decomposition] OK: interaction session click-event host decomposition Phase 44 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
