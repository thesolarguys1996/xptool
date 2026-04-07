from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE36_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE36_INTERACTION_SESSION_POST_CLICK_SETTLE_SERVICE_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleHostTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE36_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase36-interaction-session-post-click-settle-service-factory] FAILED")
        for error in errors:
            print(f"[phase36-interaction-session-post-click-settle-service-factory] ERROR {error}")
        return 1

    phase36_plan_text = _read(PHASE36_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    post_click_settle_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_TEST)

    if "## Phase 36 Slice Status" not in phase36_plan_text:
        errors.append("phase36_plan_missing_slice_status")
    if "`36.1` complete." not in phase36_plan_text:
        errors.append("phase36_plan_missing_36_1_complete")
    if "`36.2` complete." not in phase36_plan_text:
        errors.append("phase36_plan_missing_36_2_complete")
    if "`36.3` complete." not in phase36_plan_text:
        errors.append("phase36_plan_missing_36_3_complete")

    if "## Phase 36 (Interaction Session Post-Click Settle Service Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase36_section")

    if "PHASE 36 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase36_started")
    if "PHASE 36 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase36_complete")

    required_tasks = [
        "- [x] Define Phase 36 interaction session post-click-settle service-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session post-click-settle service construction from `InteractionSession` into focused host-factory method.",
        "- [x] Run Phase 36 verification + guard pack and mark `PHASE 36 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase36_line:{task_line}")

    if "InteractionSessionHostFactory.createPostClickSettleService(" not in interaction_session_text:
        errors.append("interaction_session_missing_post_click_settle_service_factory_usage")
    if "new InteractionPostClickSettleService(" in interaction_session_text:
        errors.append("interaction_session_still_constructs_post_click_settle_service_inline")

    if "static InteractionPostClickSettleService createPostClickSettleService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_service_factory_method")
    if "static InteractionPostClickSettleService.Host createPostClickSettleHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_delegate_host_factory_method")

    if "createPostClickSettleHostFromDelegatesRoutesAllCallbacks" not in post_click_settle_test_text:
        errors.append("interaction_session_host_factory_post_click_settle_test_missing_delegate_case")

    if errors:
        print("[phase36-interaction-session-post-click-settle-service-factory] FAILED")
        for error in errors:
            print(f"[phase36-interaction-session-post-click-settle-service-factory] ERROR {error}")
        return 1

    print(
        "[phase36-interaction-session-post-click-settle-service-factory] OK: interaction session post-click-settle service factory Phase 36 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
