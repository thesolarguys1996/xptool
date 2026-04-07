from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE84_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE84_INTERACTION_SESSION_OWNERSHIP_SERVICE_COMPOSITE_FACTORY_EXTRACTION_PLAN.md"
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
INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionHostFactoryOwnershipServiceFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE84_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_HOST_FACTORY,
        INTERACTION_SESSION_OWNERSHIP_FACTORY,
        INTERACTION_SESSION_OWNERSHIP_FACTORY_TEST,
        INTERACTION_SESSION_HOST_FACTORY_OWNERSHIP_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase84-interaction-session-ownership-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase84-interaction-session-ownership-service-composite-factory-extraction] ERROR {error}")
        return 1

    phase84_plan_text = _read(PHASE84_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    host_factory_text = _read(INTERACTION_SESSION_HOST_FACTORY)
    ownership_factory_text = _read(INTERACTION_SESSION_OWNERSHIP_FACTORY)

    if "## Phase 84 Slice Status" not in phase84_plan_text:
        errors.append("phase84_plan_missing_slice_status")
    if "`84.1` complete." not in phase84_plan_text:
        errors.append("phase84_plan_missing_84_1_complete")
    if "`84.2` complete." not in phase84_plan_text:
        errors.append("phase84_plan_missing_84_2_complete")
    if "`84.3` complete." not in phase84_plan_text:
        errors.append("phase84_plan_missing_84_3_complete")

    if "## Phase 84 (Interaction Session Ownership Service Composite Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase84_section")

    if "PHASE 84 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase84_started")
    if "PHASE 84 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase84_complete")

    required_tasks = [
        "- [x] Define Phase 84 interaction session ownership service composite factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session ownership composite service assembly into focused `InteractionSessionOwnershipFactory` ownership.",
        "- [x] Run Phase 84 verification + guard pack and mark `PHASE 84 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase84_line:{task_line}")

    if "InteractionSessionOwnershipFactory.createOwnershipService(" not in host_factory_text:
        errors.append("interaction_session_host_factory_missing_ownership_composite_service_factory_delegation")

    compatibility_strings = [
        "return createOwnershipServiceFromHost(",
        "createOwnershipHost(",
        "InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(host);",
    ]
    for compatibility_string in compatibility_strings:
        if compatibility_string not in host_factory_text:
            errors.append(f"interaction_session_host_factory_missing_compatibility_string:{compatibility_string}")

    if "static InteractionSessionOwnershipService createOwnershipService(" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_composite_service_method")
    if "return createOwnershipServiceFromHost(" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_composite_service_delegation")
    if "createOwnershipHost(" not in ownership_factory_text:
        errors.append("interaction_session_ownership_factory_missing_composite_service_host_builder")

    if errors:
        print("[phase84-interaction-session-ownership-service-composite-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase84-interaction-session-ownership-service-composite-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase84-interaction-session-ownership-service-composite-factory-extraction] OK: interaction session ownership service composite factory extraction Phase 84 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
