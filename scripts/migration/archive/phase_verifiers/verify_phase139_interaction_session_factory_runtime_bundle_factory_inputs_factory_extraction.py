from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE139_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE139_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.java"
)
INPUTS_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactoryTest.java"
)
INPUTS_ASSEMBLY_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE139_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INPUTS_FACTORY,
        INPUTS_FACTORY_TEST,
        INPUTS_ASSEMBLY_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase139-interaction-session-factory-runtime-bundle-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase139-interaction-session-factory-runtime-bundle-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase139_plan_text = _read(PHASE139_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    inputs_factory_text = _read(INPUTS_FACTORY)
    inputs_factory_test_text = _read(INPUTS_FACTORY_TEST)
    inputs_assembly_factory_text = _read(INPUTS_ASSEMBLY_FACTORY)

    if "## Phase 139 Slice Status" not in phase139_plan_text:
        errors.append("phase139_plan_missing_slice_status")
    if "`139.1` complete." not in phase139_plan_text:
        errors.append("phase139_plan_missing_139_1_complete")
    if "`139.2` complete." not in phase139_plan_text:
        errors.append("phase139_plan_missing_139_2_complete")
    if "`139.3` complete." not in phase139_plan_text:
        errors.append("phase139_plan_missing_139_3_complete")

    if (
        "## Phase 139 (Interaction Session Factory Runtime Bundle Factory Inputs Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase139_section")

    if "PHASE 139 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase139_started")
    if "PHASE 139 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase139_complete")

    required_tasks = [
        "- [x] Define Phase 139 interaction-session factory runtime-bundle-factory-inputs factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleFactoryInputsFactory` ownership for interaction-session runtime-bundle-factory typed-input construction seams.",
        "- [x] Run Phase 139 verification + guard pack and mark `PHASE 139 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase139_line:{task_line}")

    required_inputs_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleFactoryInputsFactory",
        "createRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryInputs factoryInputs",
        "InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs",
        "InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()",
    ]
    for required_string in required_inputs_factory_strings:
        if required_string not in inputs_factory_text:
            errors.append(f"inputs_factory_missing_string:{required_string}")

    required_inputs_factory_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleFactoryInputsFactoryTest",
        "createRuntimeBundleFactoryInputsBuildsTypedInputsFromFactoryInputsWithDefaultKey",
        "createRuntimeBundleFactoryInputsBuildsTypedInputsFromFactoryInputsWithProvidedKey",
        "createRuntimeBundleFactoryInputsBuildsTypedInputsFromAssemblyInputs",
    ]
    for required_string in required_inputs_factory_test_strings:
        if required_string not in inputs_factory_test_text:
            errors.append(f"inputs_factory_test_missing_string:{required_string}")

    required_inputs_assembly_factory_strings = [
        "InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(",
        "assemblyFactoryInputs",
    ]
    for required_string in required_inputs_assembly_factory_strings:
        if required_string not in inputs_assembly_factory_text:
            errors.append(f"inputs_assembly_factory_missing_string:{required_string}")

    if errors:
        print("[phase139-interaction-session-factory-runtime-bundle-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase139-interaction-session-factory-runtime-bundle-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase139-interaction-session-factory-runtime-bundle-factory-inputs-factory-extraction] OK: "
        "interaction session factory runtime-bundle-factory-inputs factory extraction Phase 139 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
