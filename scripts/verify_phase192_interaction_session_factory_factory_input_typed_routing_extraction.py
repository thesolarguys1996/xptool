from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE192_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE192_INTERACTION_SESSION_FACTORY_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryFactoryInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE192_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        FACTORY_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase192-interaction-session-factory-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase192-interaction-session-factory-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase192_plan_text = _read(PHASE192_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    factory_inputs_session_factory_text = _read(FACTORY_INPUTS_SESSION_FACTORY)

    if "## Phase 192 Slice Status" not in phase192_plan_text:
        errors.append("phase192_plan_missing_slice_status")
    if "`192.1` complete." not in phase192_plan_text:
        errors.append("phase192_plan_missing_192_1_complete")
    if "`192.2` complete." not in phase192_plan_text:
        errors.append("phase192_plan_missing_192_2_complete")
    if "`192.3` complete." not in phase192_plan_text:
        errors.append("phase192_plan_missing_192_3_complete")

    if (
        "## Phase 192 (Interaction Session Factory Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase192_section")

    if "PHASE 192 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase192_started")
    if "PHASE 192 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase192_complete")

    required_tasks = [
        "- [x] Define Phase 192 interaction-session factory factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromFactoryInputs(...)` through focused factory-input session routing ownership.",
        "- [x] Run Phase 192 verification + guard pack and mark `PHASE 192 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase192_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromFactoryInputs(",
        "InteractionSessionFactoryFactoryInputsSessionFactory.createFromFactoryInputs(",
        "InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    if "InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(" not in factory_inputs_session_factory_text:
        errors.append("factory_inputs_session_factory_missing_default_factory_inputs_delegate")

    if errors:
        print("[phase192-interaction-session-factory-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase192-interaction-session-factory-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase192-interaction-session-factory-factory-input-typed-routing-extraction] OK: "
        "interaction session factory factory-input typed routing extraction Phase 192 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
