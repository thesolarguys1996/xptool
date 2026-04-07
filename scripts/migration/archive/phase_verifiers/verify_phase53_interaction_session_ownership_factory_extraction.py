from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE53_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE53_INTERACTION_SESSION_OWNERSHIP_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_OWNERSHIP_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipFactory.java"
)
INTERACTION_SESSION_OWNERSHIP_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE53_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_OWNERSHIP_FACTORY,
        INTERACTION_SESSION_OWNERSHIP_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase53-interaction-session-ownership-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase53-interaction-session-ownership-factory-extraction] ERROR {error}")
        return 1

    phase53_plan_text = _read(PHASE53_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    ownership_factory_text = _read(INTERACTION_SESSION_OWNERSHIP_FACTORY)
    ownership_factory_test_text = _read(INTERACTION_SESSION_OWNERSHIP_FACTORY_TEST)

    if "## Phase 53 Slice Status" not in phase53_plan_text:
        errors.append("phase53_plan_missing_slice_status")
    if "`53.1` complete." not in phase53_plan_text:
        errors.append("phase53_plan_missing_53_1_complete")
    if "`53.2` complete." not in phase53_plan_text:
        errors.append("phase53_plan_missing_53_2_complete")
    if "`53.3` complete." not in phase53_plan_text:
        errors.append("phase53_plan_missing_53_3_complete")

    if "## Phase 53 (Interaction Session Ownership Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase53_section")

    if "PHASE 53 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase53_started")
    if "PHASE 53 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase53_complete")

    required_tasks = [
        "- [x] Define Phase 53 interaction session ownership factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session ownership host assembly into focused `InteractionSessionOwnershipFactory`.",
        "- [x] Run Phase 53 verification + guard pack and mark `PHASE 53 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase53_line:{task_line}")

    if "InteractionSessionOwnershipFactory.createOwnershipHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_host_factory_delegation")
    if "InteractionSessionOwnershipFactory.createOwnershipHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_delegate_host_factory_delegation")
    if "return new InteractionSessionOwnershipService(host);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_service_compat_construction")

    if "final class InteractionSessionOwnershipFactory" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_class")
    if "static InteractionSessionOwnershipService.Host createOwnershipHost(" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_host_factory_method")
    if (
        "static InteractionSessionOwnershipService.Host createOwnershipHostFromDelegates("
        not in ownership_factory_text
    ):
        errors.append("interaction_session_ownership_factory_missing_delegate_host_factory_method")
    if "tryRunPostClickSettle.run();" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_try_run_post_click_delegate_string")

    if "createOwnershipHostFromDelegatesRoutesAllCallbacks" not in ownership_factory_test_text:
        errors.append("interaction_session_ownership_factory_test_missing_delegate_case")

    if errors:
        print("[phase53-interaction-session-ownership-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase53-interaction-session-ownership-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase53-interaction-session-ownership-factory-extraction] OK: interaction session ownership factory extraction Phase 53 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
