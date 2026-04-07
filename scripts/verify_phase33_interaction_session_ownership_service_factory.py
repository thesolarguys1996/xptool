from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE33_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE33_INTERACTION_SESSION_OWNERSHIP_SERVICE_FACTORY_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipHostDelegatesTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE33_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase33-interaction-session-ownership-service-factory] FAILED")
        for error in errors:
            print(f"[phase33-interaction-session-ownership-service-factory] ERROR {error}")
        return 1

    phase33_plan_text = _read(PHASE33_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    ownership_host_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_TEST)

    if "## Phase 33 Slice Status" not in phase33_plan_text:
        errors.append("phase33_plan_missing_slice_status")
    if "`33.1` complete." not in phase33_plan_text:
        errors.append("phase33_plan_missing_33_1_complete")
    if "`33.2` complete." not in phase33_plan_text:
        errors.append("phase33_plan_missing_33_2_complete")
    if "`33.3` complete." not in phase33_plan_text:
        errors.append("phase33_plan_missing_33_3_complete")

    if "## Phase 33 (Interaction Session Ownership Service Factory Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase33_section")

    if "PHASE 33 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase33_started")
    if "PHASE 33 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase33_complete")

    required_tasks = [
        "- [x] Define Phase 33 interaction session ownership service-factory decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session ownership-service construction from `InteractionSession` into focused host-factory method.",
        "- [x] Run Phase 33 verification + guard pack and mark `PHASE 33 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase33_line:{task_line}")

    if "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text:
        errors.append("interaction_session_missing_ownership_service_factory_usage")
    if "new InteractionSessionOwnershipService(" in interaction_session_text:
        errors.append("interaction_session_still_constructs_ownership_service_inline")

    if "static InteractionSessionOwnershipService createOwnershipService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_service_factory_method")
    if "static InteractionSessionOwnershipService.Host createOwnershipHostFromDelegates(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_delegate_host_factory_method")
    if "return new InteractionSessionOwnershipService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_service_construction")

    if "createOwnershipHostFromDelegatesRoutesAllCallbacks" not in ownership_host_test_text:
        errors.append("interaction_session_host_factory_ownership_test_missing_delegate_case")

    if errors:
        print("[phase33-interaction-session-ownership-service-factory] FAILED")
        for error in errors:
            print(f"[phase33-interaction-session-ownership-service-factory] ERROR {error}")
        return 1

    print(
        "[phase33-interaction-session-ownership-service-factory] OK: interaction session ownership service factory Phase 33 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
