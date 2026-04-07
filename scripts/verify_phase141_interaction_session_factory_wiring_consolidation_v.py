from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE141_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE141_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_V_PLAN.md"
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

    required_paths = [PHASE141_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase141-interaction-session-factory-wiring-consolidation-v] FAILED")
        for error in errors:
            print(f"[phase141-interaction-session-factory-wiring-consolidation-v] ERROR {error}")
        return 1

    phase141_plan_text = _read(PHASE141_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 141 Slice Status" not in phase141_plan_text:
        errors.append("phase141_plan_missing_slice_status")
    if "`141.1` complete." not in phase141_plan_text:
        errors.append("phase141_plan_missing_141_1_complete")
    if "`141.2` complete." not in phase141_plan_text:
        errors.append("phase141_plan_missing_141_2_complete")
    if "`141.3` complete." not in phase141_plan_text:
        errors.append("phase141_plan_missing_141_3_complete")

    if "## Phase 141 (Interaction Session Factory Wiring Consolidation V)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase141_section")

    if "PHASE 141 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase141_started")
    if "PHASE 141 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase141_complete")

    required_tasks = [
        "- [x] Define Phase 141 interaction-session factory wiring consolidation V scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` through runtime-bundle-factory typed input routing ownership.",
        "- [x] Run Phase 141 verification + guard pack and mark `PHASE 141 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase141_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromAssemblyFactoryInputs(",
        "return createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(runtimeBundleFactoryInputs)",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase141-interaction-session-factory-wiring-consolidation-v] FAILED")
        for error in errors:
            print(f"[phase141-interaction-session-factory-wiring-consolidation-v] ERROR {error}")
        return 1

    print(
        "[phase141-interaction-session-factory-wiring-consolidation-v] OK: interaction session factory wiring "
        "consolidation V Phase 141 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
