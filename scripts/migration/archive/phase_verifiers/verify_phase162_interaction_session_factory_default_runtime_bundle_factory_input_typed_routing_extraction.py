from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE162_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE162_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java"
)
DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE162_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ENTRY_FACTORY,
        DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase162-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase162-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase162_plan_text = _read(PHASE162_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)
    default_runtime_bundle_factory_inputs_factory_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY)

    if "## Phase 162 Slice Status" not in phase162_plan_text:
        errors.append("phase162_plan_missing_slice_status")
    if "`162.1` complete." not in phase162_plan_text:
        errors.append("phase162_plan_missing_162_1_complete")
    if "`162.2` complete." not in phase162_plan_text:
        errors.append("phase162_plan_missing_162_2_complete")
    if "`162.3` complete." not in phase162_plan_text:
        errors.append("phase162_plan_missing_162_3_complete")

    if (
        "## Phase 162 (Interaction Session Factory Default Runtime Bundle Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase162_section")

    if "PHASE 162 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase162_started")
    if "PHASE 162 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase162_complete")

    required_tasks = [
        "- [x] Define Phase 162 interaction-session factory default runtime-bundle-factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Extract typed default runtime-bundle-factory-input routing ownership through `InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory`.",
        "- [x] Run Phase 162 verification + guard pack and mark `PHASE 162 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase162_line:{task_line}")

    required_default_entry_strings = [
        "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_entry_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    required_default_inputs_factory_strings = [
        "createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_inputs_factory_strings:
        if required_string not in default_runtime_bundle_factory_inputs_factory_text:
            errors.append(f"default_runtime_bundle_factory_inputs_factory_missing_string:{required_string}")

    if errors:
        print("[phase162-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase162-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase162-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] OK: "
        "interaction session factory default runtime-bundle-factory-input typed routing extraction Phase 162 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
