from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE194_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE194_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ASSEMBLY_RUNTIME_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE194_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ASSEMBLY_RUNTIME_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase194-interaction-session-factory-assembly-runtime-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase194-interaction-session-factory-assembly-runtime-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase194_plan_text = _read(PHASE194_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    assembly_runtime_session_factory_text = _read(ASSEMBLY_RUNTIME_SESSION_FACTORY)

    if "## Phase 194 Slice Status" not in phase194_plan_text:
        errors.append("phase194_plan_missing_slice_status")
    if "`194.1` complete." not in phase194_plan_text:
        errors.append("phase194_plan_missing_194_1_complete")
    if "`194.2` complete." not in phase194_plan_text:
        errors.append("phase194_plan_missing_194_2_complete")
    if "`194.3` complete." not in phase194_plan_text:
        errors.append("phase194_plan_missing_194_3_complete")

    if (
        "## Phase 194 (Interaction Session Factory Assembly Runtime Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase194_section")

    if "PHASE 194 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase194_started")
    if "PHASE 194 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase194_complete")

    required_tasks = [
        "- [x] Define Phase 194 interaction-session factory assembly-runtime typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromAssemblyFactoryInputs(...)` and `createFromRuntimeBundleFactoryInputs(...)` through focused assembly/runtime session routing ownership.",
        "- [x] Run Phase 194 verification + guard pack and mark `PHASE 194 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase194_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    required_assembly_runtime_strings = [
        "InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_assembly_runtime_strings:
        if required_string not in assembly_runtime_session_factory_text:
            errors.append(f"assembly_runtime_session_factory_missing_string:{required_string}")

    if errors:
        print("[phase194-interaction-session-factory-assembly-runtime-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase194-interaction-session-factory-assembly-runtime-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase194-interaction-session-factory-assembly-runtime-typed-routing-extraction] OK: "
        "interaction session factory assembly-runtime typed routing extraction Phase 194 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
