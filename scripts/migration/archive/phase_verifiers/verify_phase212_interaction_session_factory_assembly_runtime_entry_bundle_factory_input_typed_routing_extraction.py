from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE212_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE212_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE212_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase212-interaction-session-factory-assembly-runtime-entry-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase212-interaction-session-factory-assembly-runtime-entry-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase212_plan_text = _read(PHASE212_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    assembly_runtime_entry_bundle_factory_inputs_session_factory_text = _read(
        ASSEMBLY_RUNTIME_ENTRY_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY
    )

    if "## Phase 212 Slice Status" not in phase212_plan_text:
        errors.append("phase212_plan_missing_slice_status")
    if "`212.1` complete." not in phase212_plan_text:
        errors.append("phase212_plan_missing_212_1_complete")
    if "`212.2` complete." not in phase212_plan_text:
        errors.append("phase212_plan_missing_212_2_complete")
    if "`212.3` complete." not in phase212_plan_text:
        errors.append("phase212_plan_missing_212_3_complete")

    if (
        "## Phase 212 (Interaction Session Factory Assembly Runtime Entry Bundle Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase212_section")

    if "PHASE 212 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase212_started")
    if "PHASE 212 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase212_complete")

    required_tasks = [
        "- [x] Define Phase 212 interaction-session factory assembly-runtime entry bundle-factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromRuntimeBundleFactoryInputs(...)` through focused assembly-runtime entry bundle-factory-input session routing ownership.",
        "- [x] Run Phase 212 verification + guard pack and mark `PHASE 212 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase212_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    if "InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(" not in assembly_runtime_entry_bundle_factory_inputs_session_factory_text:
        errors.append("assembly_runtime_entry_bundle_factory_inputs_session_factory_missing_assembly_runtime_delegate")

    if errors:
        print("[phase212-interaction-session-factory-assembly-runtime-entry-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase212-interaction-session-factory-assembly-runtime-entry-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase212-interaction-session-factory-assembly-runtime-entry-bundle-factory-input-typed-routing-extraction] OK: "
        "interaction session factory assembly-runtime entry bundle-factory-input typed routing extraction Phase 212 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
