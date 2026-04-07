from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE40_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE40_INTERACTION_SESSION_OWNERSHIP_SERVICE_HOST_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_HOST_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionHostFactory.java"
)
INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_SERVICE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE40_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_SERVICE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase40-interaction-session-ownership-service-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase40-interaction-session-ownership-service-host-decomposition] ERROR {error}")
        return 1

    phase40_plan_text = _read(PHASE40_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    ownership_service_factory_test_text = _read(INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_SERVICE_FACTORY_TEST)

    if "## Phase 40 Slice Status" not in phase40_plan_text:
        errors.append("phase40_plan_missing_slice_status")
    if "`40.1` complete." not in phase40_plan_text:
        errors.append("phase40_plan_missing_40_1_complete")
    if "`40.2` complete." not in phase40_plan_text:
        errors.append("phase40_plan_missing_40_2_complete")
    if "`40.3` complete." not in phase40_plan_text:
        errors.append("phase40_plan_missing_40_3_complete")

    if "## Phase 40 (Interaction Session Ownership Service Host Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase40_section")

    if "PHASE 40 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase40_started")
    if "PHASE 40 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase40_complete")

    required_tasks = [
        "- [x] Define Phase 40 interaction session ownership-service host decomposition scope and completion evidence gates.",
        "- [x] Extract interaction-session ownership service host-based construction boundary in `InteractionSessionHostFactory`.",
        "- [x] Run Phase 40 verification + guard pack and mark `PHASE 40 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase40_line:{task_line}")

    if "InteractionSessionHostFactory.createOwnershipService(" not in interaction_session_text:
        errors.append("interaction_session_missing_ownership_service_factory_usage")

    if "static InteractionSessionOwnershipService createOwnershipServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_service_from_host_method")
    if "return createOwnershipServiceFromHost(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_service_from_host_delegation")

    if "createOwnershipServiceFromHostRoutesOnGameTickOwnershipLifecycle" not in ownership_service_factory_test_text:
        errors.append("interaction_session_host_factory_ownership_service_test_missing_delegate_case")

    if errors:
        print("[phase40-interaction-session-ownership-service-host-decomposition] FAILED")
        for error in errors:
            print(f"[phase40-interaction-session-ownership-service-host-decomposition] ERROR {error}")
        return 1

    print(
        "[phase40-interaction-session-ownership-service-host-decomposition] OK: interaction session ownership service host decomposition Phase 40 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
