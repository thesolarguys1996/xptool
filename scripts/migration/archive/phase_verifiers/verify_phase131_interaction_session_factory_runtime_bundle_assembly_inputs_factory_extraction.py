from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE131_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE131_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_ASSEMBLY_INPUTS_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.java"
)
ASSEMBLY_INPUTS_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleAssemblyInputsFactoryTest.java"
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
        PHASE131_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ASSEMBLY_INPUTS_FACTORY,
        ASSEMBLY_INPUTS_FACTORY_TEST,
        RUNTIME_BUNDLE_FACTORY_INPUTS,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase131-interaction-session-factory-runtime-bundle-assembly-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase131-interaction-session-factory-runtime-bundle-assembly-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase131_plan_text = _read(PHASE131_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_inputs_factory_text = _read(ASSEMBLY_INPUTS_FACTORY)
    assembly_inputs_factory_test_text = _read(ASSEMBLY_INPUTS_FACTORY_TEST)
    runtime_bundle_factory_inputs_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS)

    if "## Phase 131 Slice Status" not in phase131_plan_text:
        errors.append("phase131_plan_missing_slice_status")
    if "`131.1` complete." not in phase131_plan_text:
        errors.append("phase131_plan_missing_131_1_complete")
    if "`131.2` complete." not in phase131_plan_text:
        errors.append("phase131_plan_missing_131_2_complete")
    if "`131.3` complete." not in phase131_plan_text:
        errors.append("phase131_plan_missing_131_3_complete")

    if (
        "## Phase 131 (Interaction Session Factory Runtime Bundle Assembly Inputs Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase131_section")

    if "PHASE 131 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase131_started")
    if "PHASE 131 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase131_complete")

    required_tasks = [
        "- [x] Define Phase 131 interaction-session factory runtime-bundle assembly-inputs-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory` ownership for interaction-session runtime-bundle-factory assembly-input seams.",
        "- [x] Run Phase 131 verification + guard pack and mark `PHASE 131 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase131_line:{task_line}")

    required_assembly_inputs_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory",
        "static InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs",
        "return factoryInputs.createAssemblyFactoryInputs(sessionInteractionKey);",
    ]
    for required_string in required_assembly_inputs_factory_strings:
        if required_string not in assembly_inputs_factory_text:
            errors.append(f"assembly_inputs_factory_missing_string:{required_string}")

    required_assembly_inputs_factory_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleAssemblyInputsFactoryTest",
        "createAssemblyFactoryInputsBuildsAssemblyInputsFromRuntimeBundleFactoryInputs",
        "createAssemblyFactoryInputsBuildsAssemblyInputsFromFactoryInputsAndSessionInteractionKey",
    ]
    for required_string in required_assembly_inputs_factory_test_strings:
        if required_string not in assembly_inputs_factory_test_text:
            errors.append(f"assembly_inputs_factory_test_missing_string:{required_string}")

    required_runtime_bundle_factory_inputs_strings = [
        "InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(",
        "factoryInputs,",
        "sessionInteractionKey",
    ]
    for required_string in required_runtime_bundle_factory_inputs_strings:
        if required_string not in runtime_bundle_factory_inputs_text:
            errors.append(f"runtime_bundle_factory_inputs_missing_string:{required_string}")

    if errors:
        print("[phase131-interaction-session-factory-runtime-bundle-assembly-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase131-interaction-session-factory-runtime-bundle-assembly-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase131-interaction-session-factory-runtime-bundle-assembly-inputs-factory-extraction] OK: "
        "interaction session factory runtime-bundle assembly-inputs-factory extraction Phase 131 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
