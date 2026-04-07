from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE177_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE177_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_AC_PLAN.md"
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

    required_paths = [PHASE177_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase177-interaction-session-factory-wiring-consolidation-ac] FAILED")
        for error in errors:
            print(f"[phase177-interaction-session-factory-wiring-consolidation-ac] ERROR {error}")
        return 1

    phase177_plan_text = _read(PHASE177_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 177 Slice Status" not in phase177_plan_text:
        errors.append("phase177_plan_missing_slice_status")
    if "`177.1` complete." not in phase177_plan_text:
        errors.append("phase177_plan_missing_177_1_complete")
    if "`177.2` complete." not in phase177_plan_text:
        errors.append("phase177_plan_missing_177_2_complete")
    if "`177.3` complete." not in phase177_plan_text:
        errors.append("phase177_plan_missing_177_3_complete")

    if "## Phase 177 (Interaction Session Factory Wiring Consolidation AC)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase177_section")

    if "PHASE 177 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase177_started")
    if "PHASE 177 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase177_complete")

    required_tasks = [
        "- [x] Define Phase 177 interaction-session factory wiring consolidation AC scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromFactoryInputs(...)` through focused default factory-input session routing ownership.",
        "- [x] Run Phase 177 verification + guard pack and mark `PHASE 177 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase177_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase177-interaction-session-factory-wiring-consolidation-ac] FAILED")
        for error in errors:
            print(f"[phase177-interaction-session-factory-wiring-consolidation-ac] ERROR {error}")
        return 1

    print(
        "[phase177-interaction-session-factory-wiring-consolidation-ac] OK: interaction session factory wiring "
        "consolidation AC Phase 177 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
