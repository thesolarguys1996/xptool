from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE80_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE80_INTERACTION_SESSION_POST_CLICK_SETTLE_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md"
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
INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryPostClickSettleServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE80_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_POST_CLICK_SETTLE_FACTORY,
        INTERACTION_POST_CLICK_SETTLE_FACTORY_TEST,
        INTERACTION_SESSION_HOST_FACTORY_POST_CLICK_SETTLE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase80-interaction-session-post-click-settle-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase80-interaction-session-post-click-settle-service-composite-factory-extraction] ERROR {error}")
        return 1

    phase80_plan_text = _read(PHASE80_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    post_click_settle_factory_text = _read(INTERACTION_POST_CLICK_SETTLE_FACTORY)

    if "## Phase 80 Slice Status" not in phase80_plan_text:
        errors.append("phase80_plan_missing_slice_status")
    if "`80.1` complete." not in phase80_plan_text:
        errors.append("phase80_plan_missing_80_1_complete")
    if "`80.2` complete." not in phase80_plan_text:
        errors.append("phase80_plan_missing_80_2_complete")
    if "`80.3` complete." not in phase80_plan_text:
        errors.append("phase80_plan_missing_80_3_complete")

    if "## Phase 80 (Interaction Session Post-Click Settle Service Composite Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase80_section")

    if "PHASE 80 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase80_started")
    if "PHASE 80 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase80_complete")

    required_tasks = [
        "- [x] Define Phase 80 interaction session post-click settle service composite factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session post-click settle composite service assembly into focused `InteractionPostClickSettleFactory` ownership.",
        "- [x] Run Phase 80 verification + guard pack and mark `PHASE 80 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase80_line:{task_line}")

    if "InteractionPostClickSettleFactory.createPostClickSettleService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_post_click_settle_composite_service_factory_delegation")

    compatibility_strings = [
        "return createPostClickSettleServiceFromHost(",
        "createPostClickSettleHost(",
        "InteractionPostClickSettleFactory.createPostClickSettleServiceFromHost(host);",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "static InteractionPostClickSettleService createPostClickSettleService(" not in post_click_settle_factory_text:
        errors.append("interaction_post_click_settle_factory_missing_composite_service_method")
    if "return createPostClickSettleServiceFromHost(" not in post_click_settle_factory_text:
        errors.append("interaction_post_click_settle_factory_missing_composite_service_delegation")

    if errors:
        print("[phase80-interaction-session-post-click-settle-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase80-interaction-session-post-click-settle-service-composite-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase80-interaction-session-post-click-settle-service-composite-factory-extraction] OK: interaction session post-click settle service composite factory extraction Phase 80 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
