from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE94_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE94_INTERACTION_SESSION_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
INTERACTION_SESSION_FACTORY_TEST = PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryTest.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE94_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_FACTORY,
        INTERACTION_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase94-interaction-session-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase94-interaction-session-factory-extraction] ERROR {error}")
        return 1

    phase94_plan_text = _read(PHASE94_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)
    interaction_session_factory_test_text = _read(INTERACTION_SESSION_FACTORY_TEST)

    if "## Phase 94 Slice Status" not in phase94_plan_text:
        errors.append("phase94_plan_missing_slice_status")
    if "`94.1` complete." not in phase94_plan_text:
        errors.append("phase94_plan_missing_94_1_complete")
    if "`94.2` complete." not in phase94_plan_text:
        errors.append("phase94_plan_missing_94_2_complete")
    if "`94.3` complete." not in phase94_plan_text:
        errors.append("phase94_plan_missing_94_3_complete")

    if "## Phase 94 (Interaction Session Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase94_section")

    if "PHASE 94 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase94_started")
    if "PHASE 94 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase94_complete")

    required_tasks = [
        "- [x] Define Phase 94 interaction session factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session construction into focused `InteractionSessionFactory` ownership.",
        "- [x] Run Phase 94 verification + guard pack and mark `PHASE 94 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase94_line:{task_line}")

    required_factory_strings = [
        "public final class InteractionSessionFactory",
        "public static InteractionSession create(",
        "static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle)",
        "InteractionSessionAssemblyFactory.createRuntimeBundle(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if "createFromRuntimeBundleBuildsSessionThatDelegatesToRuntimeServices" not in interaction_session_factory_test_text:
        errors.append("interaction_session_factory_test_missing_delegation_case")

    if errors:
        print("[phase94-interaction-session-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase94-interaction-session-factory-extraction] ERROR {error}")
        return 1

    print("[phase94-interaction-session-factory-extraction] OK: interaction session factory extraction Phase 94 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
