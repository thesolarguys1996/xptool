from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE206_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE206_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_RUNTIME_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeSessionFactory.java"
)
ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE206_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ASSEMBLY_RUNTIME_SESSION_FACTORY,
        ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase206-interaction-session-factory-assembly-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase206-interaction-session-factory-assembly-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase206_plan_text = _read(PHASE206_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_runtime_session_factory_text = _read(ASSEMBLY_RUNTIME_SESSION_FACTORY)
    assembly_runtime_bundle_factory_inputs_session_factory_text = _read(
        ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY
    )

    if "## Phase 206 Slice Status" not in phase206_plan_text:
        errors.append("phase206_plan_missing_slice_status")
    if "`206.1` complete." not in phase206_plan_text:
        errors.append("phase206_plan_missing_206_1_complete")
    if "`206.2` complete." not in phase206_plan_text:
        errors.append("phase206_plan_missing_206_2_complete")
    if "`206.3` complete." not in phase206_plan_text:
        errors.append("phase206_plan_missing_206_3_complete")

    if (
        "## Phase 206 (Interaction Session Factory Assembly Runtime Bundle Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase206_section")

    if "PHASE 206 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase206_started")
    if "PHASE 206 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase206_complete")

    required_tasks = [
        "- [x] Define Phase 206 interaction-session factory assembly-runtime bundle-factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(...)` through focused assembly-runtime bundle-factory-input session routing ownership.",
        "- [x] Run Phase 206 verification + guard pack and mark `PHASE 206 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase206_line:{task_line}")

    required_session_factory_strings = [
        "static InteractionSession createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_session_factory_strings:
        if required_string not in assembly_runtime_session_factory_text:
            errors.append(f"assembly_runtime_session_factory_missing_string:{required_string}")

    if "InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(" not in assembly_runtime_bundle_factory_inputs_session_factory_text:
        errors.append("assembly_runtime_bundle_factory_inputs_session_factory_missing_runtime_bundle_delegate")

    if errors:
        print("[phase206-interaction-session-factory-assembly-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase206-interaction-session-factory-assembly-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase206-interaction-session-factory-assembly-runtime-bundle-factory-input-typed-routing-extraction] OK: "
        "interaction session factory assembly-runtime bundle-factory-input typed routing extraction Phase 206 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
