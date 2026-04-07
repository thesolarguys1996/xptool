from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE22_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE22_INTERACTION_CLICK_EVENT_PACKAGING_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
INTERACTION_CLICK_EVENT = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/InteractionClickEvent.java"
INTERACTION_CLICK_TELEMETRY_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/InteractionClickTelemetryService.java"
)
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_CLICK_EVENT_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/InteractionClickEventTest.java"
)
INTERACTION_CLICK_TELEMETRY_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/InteractionClickTelemetryServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE22_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        INTERACTION_CLICK_EVENT,
        INTERACTION_CLICK_TELEMETRY_SERVICE,
        INTERACTION_SESSION,
        INTERACTION_CLICK_EVENT_TEST,
        INTERACTION_CLICK_TELEMETRY_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase22-interaction-click-event-packaging] FAILED")
        for error in errors:
            print(f"[phase22-interaction-click-event-packaging] ERROR {error}")
        return 1

    phase22_plan_text = _read(PHASE22_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)
    click_event_text = _read(INTERACTION_CLICK_EVENT)
    click_telemetry_text = _read(INTERACTION_CLICK_TELEMETRY_SERVICE)
    interaction_session_text = _read(INTERACTION_SESSION)
    click_event_test_text = _read(INTERACTION_CLICK_EVENT_TEST)
    click_telemetry_test_text = _read(INTERACTION_CLICK_TELEMETRY_SERVICE_TEST)

    if "## Phase 22 Slice Status" not in phase22_plan_text:
        errors.append("phase22_plan_missing_slice_status")
    if "`22.1` complete." not in phase22_plan_text:
        errors.append("phase22_plan_missing_22_1_complete")
    if "`22.2` complete." not in phase22_plan_text:
        errors.append("phase22_plan_missing_22_2_complete")
    if "`22.3` complete." not in phase22_plan_text:
        errors.append("phase22_plan_missing_22_3_complete")

    if "## Phase 22 (Interaction Click Event Packaging Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase22_section")

    if "PHASE 22 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase22_started")
    if "PHASE 22 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase22_complete")

    required_tasks = [
        "- [x] Define Phase 22 interaction click event packaging decomposition scope and completion evidence gates.",
        "- [x] Extract interaction click event packaging ownership from `CommandExecutor` into focused runtime/event contract services.",
        "- [x] Run Phase 22 verification + guard pack and mark `PHASE 22 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase22_line:{task_line}")

    if "public static final class InteractionClickEvent {" in command_executor_text:
        errors.append("command_executor_still_contains_nested_interaction_click_event")
    if "public void onSettleEligibleInteractionClick(InteractionClickEvent clickEvent) {" not in command_executor_text:
        errors.append("command_executor_missing_event_callback_signature")
    if "interactionSession.onInteractionClickEvent(clickEvent);" not in command_executor_text:
        errors.append("command_executor_missing_session_forwarding")

    if "public final class InteractionClickEvent {" not in click_event_text:
        errors.append("interaction_click_event_missing_class")
    if "public boolean isSettleEligible()" not in click_event_text:
        errors.append("interaction_click_event_missing_settle_eligibility")

    if "void onSettleEligibleInteractionClick(InteractionClickEvent clickEvent);" not in click_telemetry_text:
        errors.append("interaction_click_telemetry_service_missing_object_callback")
    if "new InteractionClickEvent(" not in click_telemetry_text:
        errors.append("interaction_click_telemetry_service_missing_event_packaging")

    if "public void onInteractionClickEvent(InteractionClickEvent clickEvent) {" not in interaction_session_text:
        errors.append("interaction_session_missing_object_event_contract")

    if "constructorDefensivelyCopiesPointAndBoundsFields" not in click_event_test_text:
        errors.append("interaction_click_event_test_missing_defensive_copy_case")
    if "settleEligibilityAndNullStringSafetyMatchCurrentContract" not in click_event_test_text:
        errors.append("interaction_click_event_test_missing_contract_case")
    if "public void onSettleEligibleInteractionClick(InteractionClickEvent clickEvent) {" not in click_telemetry_test_text:
        errors.append("interaction_click_telemetry_service_test_missing_object_callback_assertion")

    if errors:
        print("[phase22-interaction-click-event-packaging] FAILED")
        for error in errors:
            print(f"[phase22-interaction-click-event-packaging] ERROR {error}")
        return 1

    print(
        "[phase22-interaction-click-event-packaging] OK: interaction click event packaging Phase 22 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
