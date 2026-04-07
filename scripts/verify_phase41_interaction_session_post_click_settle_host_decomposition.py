from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE41_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE41_INTERACTION_SESSION_POST_CLICK_SETTLE_HOST_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_SERVICE_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE41_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_SERVICE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase41-interaction-session-post-click-settle-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase41-interaction-session-post-click-settle-host-decomposition] ERROR {error}")
        return 1

    phase41_plan_text = _read(PHASE41_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    post_click_settle_service_factory_test_text = _read(
        INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_SERVICE_FACTORY_TEST
    )

    if "## Phase 41 Slice Status" not in phase41_plan_text:
        errors.append("phase41_plan_missing_slice_status")
    if "`41.1` complete." not in phase41_plan_text:
        errors.append("phase41_plan_missing_41_1_complete")
    if "`41.2` complete." not in phase41_plan_text:
        errors.append("phase41_plan_missing_41_2_complete")
    if "`41.3` complete." not in phase41_plan_text:
        errors.append("phase41_plan_missing_41_3_complete")

    if "## Phase 41 (Interaction Session Post-Click Settle Host Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase41_section")

    if "PHASE 41 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase41_started")
    if "PHASE 41 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase41_complete")

    required_tasks = [
        "- [x] Define Phase 41 interaction session post-click-settle host decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session post-click-settle service host-based construction boundary in `InteractionSessionHostFactory`.",
        "- [x] Run Phase 41 verification + guard pack and mark `PHASE 41 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase41_line:{task_line}")

    if "static InteractionPostClickSettleService createPostClickSettleServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_service_from_host_method")
    if "return createPostClickSettleServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_service_from_host_delegation")

    if (
        "createPostClickSettleServiceFromHostSchedulesAndExecutesSettle"
        not in post_click_settle_service_factory_test_text
    ):
        errors.append("interaction_session_host_factory_post_click_settle_service_test_missing_delegate_case")

    if errors:
        print("[phase41-interaction-session-post-click-settle-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase41-interaction-session-post-click-settle-host-decomposition] ERROR {error}")
        return 1

    print(
        "[phase41-interaction-session-post-click-settle-host-decomposition] OK: interaction session post-click settle host decomposition Phase 41 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
