from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE180_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE180_INTERACTION_SESSION_FACTORY_ASSEMBLY_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE180_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase180-interaction-session-factory-assembly-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase180-interaction-session-factory-assembly-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase180_plan_text = _read(PHASE180_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    assembly_factory_inputs_session_factory_text = _read(ASSEMBLY_FACTORY_INPUTS_SESSION_FACTORY)

    if "## Phase 180 Slice Status" not in phase180_plan_text:
        errors.append("phase180_plan_missing_slice_status")
    if "`180.1` complete." not in phase180_plan_text:
        errors.append("phase180_plan_missing_180_1_complete")
    if "`180.2` complete." not in phase180_plan_text:
        errors.append("phase180_plan_missing_180_2_complete")
    if "`180.3` complete." not in phase180_plan_text:
        errors.append("phase180_plan_missing_180_3_complete")

    if (
        "## Phase 180 (Interaction Session Factory Assembly Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase180_section")

    if "PHASE 180 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase180_started")
    if "PHASE 180 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase180_complete")

    required_tasks = [
        "- [x] Define Phase 180 interaction-session factory assembly-factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` through focused assembly-factory-input session routing ownership.",
        "- [x] Run Phase 180 verification + guard pack and mark `PHASE 180 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase180_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    if "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(" not in assembly_factory_inputs_session_factory_text:
        errors.append("assembly_factory_inputs_session_factory_missing_runtime_bundle_factory_inputs_delegate")

    if errors:
        print("[phase180-interaction-session-factory-assembly-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase180-interaction-session-factory-assembly-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase180-interaction-session-factory-assembly-factory-input-typed-routing-extraction] OK: "
        "interaction session factory assembly-factory-input typed routing extraction Phase 180 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
