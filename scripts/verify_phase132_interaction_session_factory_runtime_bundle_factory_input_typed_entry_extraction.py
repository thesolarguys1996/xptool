from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE132_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE132_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ENTRY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)
RUNTIME_BUNDLE_FACTORY_INPUTS = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputs.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE132_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY,
        RUNTIME_BUNDLE_FACTORY_INPUTS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase132-interaction-session-factory-runtime-bundle-factory-input-typed-entry-extraction] FAILED")
        for error in errors:
            print(
                "[phase132-interaction-session-factory-runtime-bundle-factory-input-typed-entry-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase132_plan_text = _read(PHASE132_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    runtime_bundle_factory_inputs_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS)

    if "## Phase 132 Slice Status" not in phase132_plan_text:
        errors.append("phase132_plan_missing_slice_status")
    if "`132.1` complete." not in phase132_plan_text:
        errors.append("phase132_plan_missing_132_1_complete")
    if "`132.2` complete." not in phase132_plan_text:
        errors.append("phase132_plan_missing_132_2_complete")
    if "`132.3` complete." not in phase132_plan_text:
        errors.append("phase132_plan_missing_132_3_complete")

    if (
        "## Phase 132 (Interaction Session Factory Runtime Bundle Factory Input Typed Entry Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase132_section")

    if "PHASE 132 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase132_started")
    if "PHASE 132 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase132_complete")

    required_tasks = [
        "- [x] Define Phase 132 interaction-session factory runtime-bundle-factory input typed-entry extraction scope and completion evidence gates.",
        "- [x] Extract typed-entry runtime-bundle-factory input seam ownership through `InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory` routing.",
        "- [x] Run Phase 132 verification + guard pack and mark `PHASE 132 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase132_line:{task_line}")

    required_runtime_bundle_factory_strings = [
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs",
        "runtimeBundleFactoryInputs.createAssemblyFactoryInputs()",
        "InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    required_runtime_bundle_factory_inputs_strings = [
        "InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(",
    ]
    for required_string in required_runtime_bundle_factory_inputs_strings:
        if required_string not in runtime_bundle_factory_inputs_text:
            errors.append(f"runtime_bundle_factory_inputs_missing_string:{required_string}")

    if errors:
        print("[phase132-interaction-session-factory-runtime-bundle-factory-input-typed-entry-extraction] FAILED")
        for error in errors:
            print(
                "[phase132-interaction-session-factory-runtime-bundle-factory-input-typed-entry-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase132-interaction-session-factory-runtime-bundle-factory-input-typed-entry-extraction] OK: "
        "interaction session factory runtime-bundle-factory input typed-entry extraction Phase 132 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
