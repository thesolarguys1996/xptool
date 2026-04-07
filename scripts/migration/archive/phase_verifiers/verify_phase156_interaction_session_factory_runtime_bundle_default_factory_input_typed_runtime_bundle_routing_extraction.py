from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE156_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE156_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_TYPED_RUNTIME_BUNDLE_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.java"
)
DEFAULT_RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.java"
)
DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE156_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ENTRY_FACTORY,
        DEFAULT_RUNTIME_BUNDLE_FACTORY,
        DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase156-interaction-session-factory-runtime-bundle-default-factory-input-typed-runtime-bundle-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase156-interaction-session-factory-runtime-bundle-default-factory-input-typed-runtime-bundle-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase156_plan_text = _read(PHASE156_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)
    default_runtime_bundle_factory_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY)
    default_factory_input_runtime_bundle_factory_text = _read(DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY)

    if "## Phase 156 Slice Status" not in phase156_plan_text:
        errors.append("phase156_plan_missing_slice_status")
    if "`156.1` complete." not in phase156_plan_text:
        errors.append("phase156_plan_missing_156_1_complete")
    if "`156.2` complete." not in phase156_plan_text:
        errors.append("phase156_plan_missing_156_2_complete")
    if "`156.3` complete." not in phase156_plan_text:
        errors.append("phase156_plan_missing_156_3_complete")

    if (
        "## Phase 156 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Runtime Bundle Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase156_section")

    if "PHASE 156 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase156_started")
    if "PHASE 156 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase156_complete")

    required_tasks = [
        "- [x] Define Phase 156 interaction-session factory runtime-bundle default-factory-input typed runtime-bundle routing extraction scope and completion evidence gates.",
        "- [x] Extract typed default-runtime-bundle-factory-input runtime-bundle routing ownership through `InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory`.",
        "- [x] Run Phase 156 verification + guard pack and mark `PHASE 156 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase156_line:{task_line}")

    required_default_entry_strings = [
        "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_entry_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    required_default_runtime_bundle_strings = [
        "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_runtime_bundle_strings:
        if required_string not in default_runtime_bundle_factory_text:
            errors.append(f"default_runtime_bundle_factory_missing_string:{required_string}")

    if "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(" not in default_factory_input_runtime_bundle_factory_text:
        errors.append("default_factory_input_runtime_bundle_factory_missing_runtime_bundle_factory_delegate")

    if errors:
        print("[phase156-interaction-session-factory-runtime-bundle-default-factory-input-typed-runtime-bundle-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase156-interaction-session-factory-runtime-bundle-default-factory-input-typed-runtime-bundle-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase156-interaction-session-factory-runtime-bundle-default-factory-input-typed-runtime-bundle-routing-extraction] OK: "
        "interaction session factory runtime-bundle default-factory-input typed runtime-bundle routing extraction Phase 156 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
