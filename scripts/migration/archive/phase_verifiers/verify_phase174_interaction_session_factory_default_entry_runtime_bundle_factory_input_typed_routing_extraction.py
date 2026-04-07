from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE174_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE174_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java"
)
DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE174_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ENTRY_FACTORY,
        DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase174-interaction-session-factory-default-entry-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase174-interaction-session-factory-default-entry-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase174_plan_text = _read(PHASE174_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)
    default_entry_runtime_bundle_factory_inputs_factory_text = _read(DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY)

    if "## Phase 174 Slice Status" not in phase174_plan_text:
        errors.append("phase174_plan_missing_slice_status")
    if "`174.1` complete." not in phase174_plan_text:
        errors.append("phase174_plan_missing_174_1_complete")
    if "`174.2` complete." not in phase174_plan_text:
        errors.append("phase174_plan_missing_174_2_complete")
    if "`174.3` complete." not in phase174_plan_text:
        errors.append("phase174_plan_missing_174_3_complete")

    if (
        "## Phase 174 (Interaction Session Factory Default Entry Runtime Bundle Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase174_section")

    if "PHASE 174 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase174_started")
    if "PHASE 174 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase174_complete")

    required_tasks = [
        "- [x] Define Phase 174 interaction-session factory default-entry-runtime-bundle-factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Extract typed default-entry runtime-bundle-factory-input routing ownership through `InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory`.",
        "- [x] Run Phase 174 verification + guard pack and mark `PHASE 174 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase174_line:{task_line}")

    required_default_entry_strings = [
        "InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_entry_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    if "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(" not in default_entry_runtime_bundle_factory_inputs_factory_text:
        errors.append("default_entry_runtime_bundle_factory_inputs_factory_missing_default_runtime_bundle_factory_inputs_delegate")

    if errors:
        print("[phase174-interaction-session-factory-default-entry-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase174-interaction-session-factory-default-entry-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase174-interaction-session-factory-default-entry-runtime-bundle-factory-input-typed-routing-extraction] OK: "
        "interaction session factory default-entry-runtime-bundle-factory-input typed routing extraction Phase 174 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
