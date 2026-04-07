from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE98_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE98_INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY_EXTRACTION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactory.java"
)
INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE98_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY,
        INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase98-interaction-session-runtime-operations-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase98-interaction-session-runtime-operations-factory-extraction] ERROR {error}")
        return 1

    phase98_plan_text = _read(PHASE98_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_operations_factory_text = _read(INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY)
    runtime_operations_factory_test_text = _read(INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY_TEST)

    if "## Phase 98 Slice Status" not in phase98_plan_text:
        errors.append("phase98_plan_missing_slice_status")
    if "`98.1` complete." not in phase98_plan_text:
        errors.append("phase98_plan_missing_98_1_complete")
    if "`98.2` complete." not in phase98_plan_text:
        errors.append("phase98_plan_missing_98_2_complete")
    if "`98.3` complete." not in phase98_plan_text:
        errors.append("phase98_plan_missing_98_3_complete")

    if "## Phase 98 (Interaction Session Runtime Operations Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase98_section")

    if "PHASE 98 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase98_started")
    if "PHASE 98 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase98_complete")

    required_tasks = [
        "- [x] Define Phase 98 interaction-session runtime-operations factory extraction scope and completion evidence gates.",
        "- [x] Extract interaction-session runtime-operations construction into focused `InteractionSessionRuntimeOperationsFactory` ownership.",
        "- [x] Run Phase 98 verification + guard pack and mark `PHASE 98 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase98_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionRuntimeOperationsFactory",
        "static InteractionSessionRuntimeOperations createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle)",
        "static InteractionSessionRuntimeOperations createFromServices(",
    ]
    for required_string in required_factory_strings:
        if required_string not in runtime_operations_factory_text:
            errors.append(f"runtime_operations_factory_missing_string:{required_string}")

    if "createFromRuntimeBundleBuildsOperationsThatDelegateToRuntimeServices" not in runtime_operations_factory_test_text:
        errors.append("runtime_operations_factory_test_missing_delegation_case")

    if errors:
        print("[phase98-interaction-session-runtime-operations-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase98-interaction-session-runtime-operations-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase98-interaction-session-runtime-operations-factory-extraction] OK: interaction session runtime operations "
        "factory extraction Phase 98 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
