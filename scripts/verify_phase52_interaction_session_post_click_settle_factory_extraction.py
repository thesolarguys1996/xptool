from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE52_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE52_INTERACTION_SESSION_POST_CLICK_SETTLE_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_POST_CLICK_SETTLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionPostClickSettleFactory.java"
)
INTERACTION_POST_CLICK_SETTLE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE52_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_POST_CLICK_SETTLE_FACTORY,
        INTERACTION_POST_CLICK_SETTLE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase52-interaction-session-post-click-settle-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase52-interaction-session-post-click-settle-factory-extraction] ERROR {error}")
        return 1

    phase52_plan_text = _read(PHASE52_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    post_click_settle_factory_text = _read(INTERACTION_POST_CLICK_SETTLE_FACTORY)
    post_click_settle_factory_test_text = _read(INTERACTION_POST_CLICK_SETTLE_FACTORY_TEST)

    if "## Phase 52 Slice Status" not in phase52_plan_text:
        errors.append("phase52_plan_missing_slice_status")
    if "`52.1` complete." not in phase52_plan_text:
        errors.append("phase52_plan_missing_52_1_complete")
    if "`52.2` complete." not in phase52_plan_text:
        errors.append("phase52_plan_missing_52_2_complete")
    if "`52.3` complete." not in phase52_plan_text:
        errors.append("phase52_plan_missing_52_3_complete")

    if "## Phase 52 (Interaction Session Post-Click Settle Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase52_section")

    if "PHASE 52 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase52_started")
    if "PHASE 52 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase52_complete")

    required_tasks = [
        "- [x] Define Phase 52 interaction session post-click-settle factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session post-click-settle service/host assembly into focused `InteractionPostClickSettleFactory`.",
        "- [x] Run Phase 52 verification + guard pack and mark `PHASE 52 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase52_line:{task_line}")

    if "InteractionPostClickSettleFactory.createPostClickSettleServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_service_from_host_delegation")
    if "InteractionPostClickSettleFactory.createPostClickSettleHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_host_factory_delegation")
    if "InteractionPostClickSettleFactory.createPostClickSettleHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_delegate_host_factory_delegation")

    if "final class InteractionPostClickSettleFactory" not in post_click_settle_factory_text:
        errors.append("interaction_post_click_settle_factory_missing_class")
    if "static InteractionPostClickSettleService createPostClickSettleService(" not in post_click_settle_factory_text:
        errors.append("interaction_post_click_settle_factory_missing_service_factory_method")
    if (
        "static InteractionPostClickSettleService createPostClickSettleServiceFromHost("
        not in post_click_settle_factory_text
    ):
        errors.append("interaction_post_click_settle_factory_missing_service_from_host_method")
    if "static InteractionPostClickSettleService.Host createPostClickSettleHost(" not in post_click_settle_factory_text:
        errors.append("interaction_post_click_settle_factory_missing_host_factory_method")
    if (
        "static InteractionPostClickSettleService.Host createPostClickSettleHostFromDelegates("
        not in post_click_settle_factory_text
    ):
        errors.append("interaction_post_click_settle_factory_missing_delegate_host_factory_method")

    if (
        "createPostClickSettleServiceFromHostSchedulesAndExecutesSettle"
        not in post_click_settle_factory_test_text
    ):
        errors.append("interaction_post_click_settle_factory_test_missing_service_delegate_case")
    if "createPostClickSettleHostFromDelegatesRoutesAllCallbacks" not in post_click_settle_factory_test_text:
        errors.append("interaction_post_click_settle_factory_test_missing_host_delegate_case")

    if errors:
        print("[phase52-interaction-session-post-click-settle-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase52-interaction-session-post-click-settle-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase52-interaction-session-post-click-settle-factory-extraction] OK: interaction session post-click-settle factory extraction Phase 52 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
