from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE25_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE25_INTERACTION_SESSION_HOST_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_OWNERSHIP_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java"
)
INTERACTION_POST_CLICK_SETTLE_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionPostClickSettleServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE25_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_OWNERSHIP_SERVICE_TEST,
        INTERACTION_POST_CLICK_SETTLE_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase25-interaction-session-host-factory] FAILED")
        for error in errors:
            print(f"[phase25-interaction-session-host-factory] ERROR {error}")
        return 1

    phase25_plan_text = _read(PHASE25_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    interaction_session_host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    ownership_service_test_text = _read(INTERACTION_SESSION_OWNERSHIP_SERVICE_TEST)
    settle_service_test_text = _read(INTERACTION_POST_CLICK_SETTLE_SERVICE_TEST)

    if "## Phase 25 Slice Status" not in phase25_plan_text:
        errors.append("phase25_plan_missing_slice_status")
    if "`25.1` complete." not in phase25_plan_text:
        errors.append("phase25_plan_missing_25_1_complete")
    if "`25.2` complete." not in phase25_plan_text:
        errors.append("phase25_plan_missing_25_2_complete")
    if "`25.3` complete." not in phase25_plan_text:
        errors.append("phase25_plan_missing_25_3_complete")

    if "## Phase 25 (Interaction Session Host-Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase25_section")

    if "PHASE 25 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase25_started")
    if "PHASE 25 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase25_complete")

    required_tasks = [
        "- [x] Define Phase 25 interaction session host-factory decomposition scope and completion evidence gates.",
        "- [x] Extract `InteractionSession` host wiring assembly into focused host-factory boundary.",
        "- [x] Run Phase 25 verification + guard pack and mark `PHASE 25 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase25_line:{task_line}")

    if (
        "InteractionSessionHostFactory.createPostClickSettleHost(" not in interaction_session_text
        and "InteractionSessionHostFactory.createPostClickSettleService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_post_click_factory_usage")
    if (
        "InteractionSessionHostFactory.createOwnershipHost(" not in interaction_session_text
        and "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text
    ):
        errors.append("interaction_session_missing_ownership_factory_usage")
    if "new InteractionPostClickSettleService.Host()" in interaction_session_text:
        errors.append("interaction_session_still_has_inline_post_click_host")
    if "new InteractionSessionOwnershipService.Host()" in interaction_session_text:
        errors.append("interaction_session_still_has_inline_ownership_host")
    if "ThreadLocalRandom" in interaction_session_text:
        errors.append("interaction_session_still_imports_or_uses_thread_local_random")

    if "final class InteractionSessionHostFactory" not in interaction_session_host_factory_text:
        errors.append("interaction_session_host_factory_missing_class")
    if "static InteractionPostClickSettleService.Host createPostClickSettleHost(" not in interaction_session_host_factory_text:
        errors.append("interaction_session_host_factory_missing_settle_factory_method")
    if "static InteractionSessionOwnershipService.Host createOwnershipHost(" not in interaction_session_host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_factory_method")

    if "onGameTickRunsSettleWhenMotorOwnershipIsAcquired" not in ownership_service_test_text:
        errors.append("interaction_session_ownership_test_missing_settle_case")
    if "schedulesPendingSettleAndExecutesWhenReady" not in settle_service_test_text:
        errors.append("interaction_post_click_settle_test_missing_schedule_case")

    if errors:
        print("[phase25-interaction-session-host-factory] FAILED")
        for error in errors:
            print(f"[phase25-interaction-session-host-factory] ERROR {error}")
        return 1

    print(
        "[phase25-interaction-session-host-factory] OK: interaction session host-factory Phase 25 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
