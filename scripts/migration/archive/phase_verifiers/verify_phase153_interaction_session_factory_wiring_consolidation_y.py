from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE153_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE153_INTERACTION_SESSION_FACTORY_WIRING_CONSOLIDATION_Y_PLAN.md"
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

    required_paths = [PHASE153_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase153-interaction-session-factory-wiring-consolidation-y] FAILED")
        for error in errors:
            print(f"[phase153-interaction-session-factory-wiring-consolidation-y] ERROR {error}")
        return 1

    phase153_plan_text = _read(PHASE153_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)

    if "## Phase 153 Slice Status" not in phase153_plan_text:
        errors.append("phase153_plan_missing_slice_status")
    if "`153.1` complete." not in phase153_plan_text:
        errors.append("phase153_plan_missing_153_1_complete")
    if "`153.2` complete." not in phase153_plan_text:
        errors.append("phase153_plan_missing_153_2_complete")
    if "`153.3` complete." not in phase153_plan_text:
        errors.append("phase153_plan_missing_153_3_complete")

    if "## Phase 153 (Interaction Session Factory Wiring Consolidation Y)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase153_section")

    if "PHASE 153 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase153_started")
    if "PHASE 153 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase153_complete")

    required_tasks = [
        "- [x] Define Phase 153 interaction-session factory wiring consolidation Y scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromFactoryInputs(...)` through runtime-bundle-factory default-entry runtime-bundle routing ownership.",
        "- [x] Run Phase 153 verification + guard pack and mark `PHASE 153 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase153_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "return createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(",
        "return createFromRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase153-interaction-session-factory-wiring-consolidation-y] FAILED")
        for error in errors:
            print(f"[phase153-interaction-session-factory-wiring-consolidation-y] ERROR {error}")
        return 1

    print(
        "[phase153-interaction-session-factory-wiring-consolidation-y] OK: interaction session factory wiring "
        "consolidation Y Phase 153 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
