from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE137_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE137_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_U_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE137_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase137-interaction-session-factory-wiring-consolidation-u] FAILED")
        for error in errors:
            print(f"[phase137-interaction-session-factory-wiring-consolidation-u] ERROR {error}")
        return 1

    phase137_plan_text = _read(PHASE137_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 137 Slice Status" not in phase137_plan_text:
        errors.append("phase137_plan_missing_slice_status")
    if "`137.1` complete." not in phase137_plan_text:
        errors.append("phase137_plan_missing_137_1_complete")
    if "`137.2` complete." not in phase137_plan_text:
        errors.append("phase137_plan_missing_137_2_complete")
    if "`137.3` complete." not in phase137_plan_text:
        errors.append("phase137_plan_missing_137_3_complete")

    if "## Phase 137 (Interaction Session Factory Wiring Consolidation U)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase137_section")

    if "PHASE 137 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase137_started")
    if "PHASE 137 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase137_complete")

    required_tasks = [
        "- [x] Define Phase 137 interaction-session factory wiring consolidation U scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` through typed runtime-bundle-factory-input assembly mapping ownership.",
        "- [x] Run Phase 137 verification + guard pack and mark `PHASE 137 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase137_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromAssemblyFactoryInputs(",
        "return createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(runtimeBundleFactoryInputs)",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase137-interaction-session-factory-wiring-consolidation-u] FAILED")
        for error in errors:
            print(f"[phase137-interaction-session-factory-wiring-consolidation-u] ERROR {error}")
        return 1

    print(
        "[phase137-interaction-session-factory-wiring-consolidation-u] OK: interaction session factory wiring "
        "consolidation U Phase 137 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
