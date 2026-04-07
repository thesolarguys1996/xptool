from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE129_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE129_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_S_PLAN.md"
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

    required_paths = [PHASE129_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase129-interaction-session-factory-wiring-consolidation-s] FAILED")
        for error in errors:
            print(f"[phase129-interaction-session-factory-wiring-consolidation-s] ERROR {error}")
        return 1

    phase129_plan_text = _read(PHASE129_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 129 Slice Status" not in phase129_plan_text:
        errors.append("phase129_plan_missing_slice_status")
    if "`129.1` complete." not in phase129_plan_text:
        errors.append("phase129_plan_missing_129_1_complete")
    if "`129.2` complete." not in phase129_plan_text:
        errors.append("phase129_plan_missing_129_2_complete")
    if "`129.3` complete." not in phase129_plan_text:
        errors.append("phase129_plan_missing_129_3_complete")

    if "## Phase 129 (Interaction Session Factory Wiring Consolidation S)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase129_section")

    if "PHASE 129 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase129_started")
    if "PHASE 129 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase129_complete")

    required_tasks = [
        "- [x] Define Phase 129 interaction-session factory wiring consolidation S scope and completion evidence gates.",
        "- [x] Consolidate public `InteractionSessionFactory.create(...)` seam through typed `InteractionSessionFactoryRuntimeBundleFactoryInputs` ownership.",
        "- [x] Run Phase 129 verification + guard pack and mark `PHASE 129 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase129_line:{task_line}")

    required_factory_strings = [
        "public static InteractionSession create(",
        "return createFromFactoryInputs(",
        "InteractionSessionFactoryInputs.fromServices(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(factoryInputs)",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(factoryInputs)",
        "InteractionSessionAssemblyFactory.createRuntimeBundle(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase129-interaction-session-factory-wiring-consolidation-s] FAILED")
        for error in errors:
            print(f"[phase129-interaction-session-factory-wiring-consolidation-s] ERROR {error}")
        return 1

    print(
        "[phase129-interaction-session-factory-wiring-consolidation-s] OK: interaction session factory wiring "
        "consolidation S Phase 129 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
