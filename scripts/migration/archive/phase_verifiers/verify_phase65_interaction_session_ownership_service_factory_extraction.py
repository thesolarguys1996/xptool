from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE65_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE65_INTERACTION_SESSION_OWNERSHIP_SERVICE_FACTORY_EXTRACTION_PLAN.md"
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
        PHASE65_PLAN,
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
        print("[phase65-interaction-session-ownership-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase65-interaction-session-ownership-service-factory-extraction] ERROR {error}")
        return 1

    phase65_plan_text = _read(PHASE65_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    ownership_factory_text = _read(INTERACTION_SESSION_OWNERSHIP_FACTORY)
    ownership_factory_test_text = _read(INTERACTION_SESSION_OWNERSHIP_FACTORY_TEST)

    if "## Phase 65 Slice Status" not in phase65_plan_text:
        errors.append("phase65_plan_missing_slice_status")
    if "`65.1` complete." not in phase65_plan_text:
        errors.append("phase65_plan_missing_65_1_complete")
    if "`65.2` complete." not in phase65_plan_text:
        errors.append("phase65_plan_missing_65_2_complete")
    if "`65.3` complete." not in phase65_plan_text:
        errors.append("phase65_plan_missing_65_3_complete")

    if "## Phase 65 (Interaction Session Ownership Service-From-Host Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase65_section")

    if "PHASE 65 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase65_started")
    if "PHASE 65 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase65_complete")

    required_tasks = [
        "- [x] Define Phase 65 interaction session ownership service-from-host factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session ownership service-from-host assembly into focused `InteractionSessionOwnershipFactory` ownership.",
        "- [x] Run Phase 65 verification + guard pack and mark `PHASE 65 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase65_line:{task_line}")

    if "InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(host);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_service_from_host_factory_delegation")
    if "return new InteractionSessionOwnershipService(host);" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_service_compatibility_sentinel")

    if "static InteractionSessionOwnershipService createOwnershipServiceFromHost(" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_service_from_host_method")
    if "return new InteractionSessionOwnershipService(host);" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_service_construction")

    if "createOwnershipServiceFromHostRoutesOnGameTickOwnershipLifecycle" not in ownership_factory_test_text:
        errors.append("interaction_session_ownership_factory_test_missing_service_from_host_case")

    if errors:
        print("[phase65-interaction-session-ownership-service-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase65-interaction-session-ownership-service-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase65-interaction-session-ownership-service-factory-extraction] OK: interaction session ownership service-from-host factory extraction Phase 65 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
