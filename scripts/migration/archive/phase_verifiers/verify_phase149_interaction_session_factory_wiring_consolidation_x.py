from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE149_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE149_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_X_PLAN.md"
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

    required_paths = [PHASE149_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase149-interaction-session-factory-wiring-consolidation-x] FAILED")
        for error in errors:
            print(f"[phase149-interaction-session-factory-wiring-consolidation-x] ERROR {error}")
        return 1

    phase149_plan_text = _read(PHASE149_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 149 Slice Status" not in phase149_plan_text:
        errors.append("phase149_plan_missing_slice_status")
    if "`149.1` complete." not in phase149_plan_text:
        errors.append("phase149_plan_missing_149_1_complete")
    if "`149.2` complete." not in phase149_plan_text:
        errors.append("phase149_plan_missing_149_2_complete")
    if "`149.3` complete." not in phase149_plan_text:
        errors.append("phase149_plan_missing_149_3_complete")

    if "## Phase 149 (Interaction Session Factory Wiring Consolidation X)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase149_section")

    if "PHASE 149 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase149_started")
    if "PHASE 149 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase149_complete")

    required_tasks = [
        "- [x] Define Phase 149 interaction-session factory wiring consolidation X scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromFactoryInputs(...)` through runtime-bundle-factory default factory-input routing ownership.",
        "- [x] Run Phase 149 verification + guard pack and mark `PHASE 149 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase149_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "return createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(factoryInputs)",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase149-interaction-session-factory-wiring-consolidation-x] FAILED")
        for error in errors:
            print(f"[phase149-interaction-session-factory-wiring-consolidation-x] ERROR {error}")
        return 1

    print(
        "[phase149-interaction-session-factory-wiring-consolidation-x] OK: interaction session factory wiring "
        "consolidation X Phase 149 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
