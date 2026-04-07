from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE121_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE121_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_Q_PLAN.md"
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

    required_paths = [PHASE121_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase121-interaction-session-factory-wiring-consolidation-q] FAILED")
        for error in errors:
            print(f"[phase121-interaction-session-factory-wiring-consolidation-q] ERROR {error}")
        return 1

    phase121_plan_text = _read(PHASE121_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 121 Slice Status" not in phase121_plan_text:
        errors.append("phase121_plan_missing_slice_status")
    if "`121.1` complete." not in phase121_plan_text:
        errors.append("phase121_plan_missing_121_1_complete")
    if "`121.2` complete." not in phase121_plan_text:
        errors.append("phase121_plan_missing_121_2_complete")
    if "`121.3` complete." not in phase121_plan_text:
        errors.append("phase121_plan_missing_121_3_complete")

    if "## Phase 121 (Interaction Session Factory Wiring Consolidation Q)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase121_section")

    if "PHASE 121 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase121_started")
    if "PHASE 121 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase121_complete")

    required_tasks = [
        "- [x] Define Phase 121 interaction-session factory wiring consolidation Q scope and completion evidence gates.",
        "- [x] Consolidate public `InteractionSessionFactory.create(...)` seam through typed `InteractionSessionFactoryRuntimeBundleFactory` ownership.",
        "- [x] Run Phase 121 verification + guard pack and mark `PHASE 121 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase121_line:{task_line}")

    required_factory_strings = [
        "public static InteractionSession create(",
        "return createFromFactoryInputs(",
        "InteractionSessionFactoryInputs.fromServices(",
        "return createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(",
        "InteractionSessionAssemblyFactory.createRuntimeBundle(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase121-interaction-session-factory-wiring-consolidation-q] FAILED")
        for error in errors:
            print(f"[phase121-interaction-session-factory-wiring-consolidation-q] ERROR {error}")
        return 1

    print(
        "[phase121-interaction-session-factory-wiring-consolidation-q] OK: interaction session factory wiring "
        "consolidation Q Phase 121 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
