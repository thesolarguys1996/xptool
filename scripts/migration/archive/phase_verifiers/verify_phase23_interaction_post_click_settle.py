from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE23_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE23_INTERACTION_POST_CLICK_SETTLE_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_POST_CLICK_SETTLE_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleService.java"
)
INTERACTION_POST_CLICK_SETTLE_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE23_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_POST_CLICK_SETTLE_SERVICE,
        INTERACTION_POST_CLICK_SETTLE_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase23-interaction-post-click-settle] FAILED")
        for error in errors:
            print(f"[phase23-interaction-post-click-settle] ERROR {error}")
        return 1

    phase23_plan_text = _read(PHASE23_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    settle_service_text = _read(INTERACTION_POST_CLICK_SETTLE_SERVICE)
    settle_service_test_text = _read(INTERACTION_POST_CLICK_SETTLE_SERVICE_TEST)

    if "## Phase 23 Slice Status" not in phase23_plan_text:
        errors.append("phase23_plan_missing_slice_status")
    if "`23.1` complete." not in phase23_plan_text:
        errors.append("phase23_plan_missing_23_1_complete")
    if "`23.2` complete." not in phase23_plan_text:
        errors.append("phase23_plan_missing_23_2_complete")
    if "`23.3` complete." not in phase23_plan_text:
        errors.append("phase23_plan_missing_23_3_complete")

    if "## Phase 23 (Interaction Post-Click Settle Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase23_section")

    if "PHASE 23 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase23_started")
    if "PHASE 23 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase23_complete")

    required_tasks = [
        "- [x] Define Phase 23 interaction post-click settle decomposition scope and completion evidence gates.",
        "- [x] Extract interaction post-click settle scheduling/state ownership from `InteractionSession` into focused runtime service.",
        "- [x] Run Phase 23 verification + guard pack and mark `PHASE 23 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase23_line:{task_line}")

    if "private final InteractionPostClickSettleService interactionPostClickSettleService;" not in interaction_session_text:
        errors.append("interaction_session_missing_settle_service_field")
    if (
        "interactionPostClickSettleService.onInteractionClickEvent(clickEvent);" not in interaction_session_text
        and "interactionSessionClickEventService.onInteractionClickEvent(clickEvent);" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_click_event_delegate")
    if (
        "interactionPostClickSettleService.shouldAcquireMotorForPendingSettle();" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipHost(" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_settle_ready_delegate")
    if (
        "interactionPostClickSettleService.tryRunPostClickSettle();" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipHost(" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_try_run_delegate")
    if "POST_CLICK_SETTLE_DELAY_FAST_MIN_MS" in interaction_session_text:
        errors.append("interaction_session_still_owns_settle_delay_constants")
    if "samplePostClickSettleDelayMs" in interaction_session_text:
        errors.append("interaction_session_still_owns_settle_delay_sampler")

    if "final class InteractionPostClickSettleService" not in settle_service_text:
        errors.append("interaction_post_click_settle_service_missing_class")
    if "interface Host" not in settle_service_text:
        errors.append("interaction_post_click_settle_service_missing_host_interface")
    if "void onInteractionClickEvent(InteractionClickEvent clickEvent)" not in settle_service_text:
        errors.append("interaction_post_click_settle_service_missing_click_event_entrypoint")
    if "boolean shouldAcquireMotorForPendingSettle()" not in settle_service_text:
        errors.append("interaction_post_click_settle_service_missing_settle_ready_method")
    if "void tryRunPostClickSettle()" not in settle_service_text:
        errors.append("interaction_post_click_settle_service_missing_try_run_method")

    if "schedulesPendingSettleAndExecutesWhenReady" not in settle_service_test_text:
        errors.append("interaction_post_click_settle_service_test_missing_schedule_execute_case")
    if "shouldAcquireClearsPendingWhenQueueBlocksSettle" not in settle_service_test_text:
        errors.append("interaction_post_click_settle_service_test_missing_queue_block_case")
    if "duplicateClickSerialIsIgnoredAfterSuccessfulSettle" not in settle_service_test_text:
        errors.append("interaction_post_click_settle_service_test_missing_duplicate_serial_case")

    if errors:
        print("[phase23-interaction-post-click-settle] FAILED")
        for error in errors:
            print(f"[phase23-interaction-post-click-settle] ERROR {error}")
        return 1

    print(
        "[phase23-interaction-post-click-settle] OK: interaction post-click settle Phase 23 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
