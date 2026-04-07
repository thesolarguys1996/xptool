from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE145_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE145_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_W_PLAN.md"
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

    required_paths = [PHASE145_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase145-interaction-session-factory-wiring-consolidation-w] FAILED")
        for error in errors:
            print(f"[phase145-interaction-session-factory-wiring-consolidation-w] ERROR {error}")
        return 1

    phase145_plan_text = _read(PHASE145_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 145 Slice Status" not in phase145_plan_text:
        errors.append("phase145_plan_missing_slice_status")
    if "`145.1` complete." not in phase145_plan_text:
        errors.append("phase145_plan_missing_145_1_complete")
    if "`145.2` complete." not in phase145_plan_text:
        errors.append("phase145_plan_missing_145_2_complete")
    if "`145.3` complete." not in phase145_plan_text:
        errors.append("phase145_plan_missing_145_3_complete")

    if "## Phase 145 (Interaction Session Factory Wiring Consolidation W)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase145_section")

    if "PHASE 145 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase145_started")
    if "PHASE 145 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase145_complete")

    required_tasks = [
        "- [x] Define Phase 145 interaction-session factory wiring consolidation W scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromFactoryInputs(...)` through runtime-bundle-factory default assembly-input routing ownership.",
        "- [x] Run Phase 145 verification + guard pack and mark `PHASE 145 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase145_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "return createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(factoryInputs)",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(factoryInputs)",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(factoryInputs)",
        "static InteractionSession createFromAssemblyFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase145-interaction-session-factory-wiring-consolidation-w] FAILED")
        for error in errors:
            print(f"[phase145-interaction-session-factory-wiring-consolidation-w] ERROR {error}")
        return 1

    print(
        "[phase145-interaction-session-factory-wiring-consolidation-w] OK: interaction session factory wiring "
        "consolidation W Phase 145 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
