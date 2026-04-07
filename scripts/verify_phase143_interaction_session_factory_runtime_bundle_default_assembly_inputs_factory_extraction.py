from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE143_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE143_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_ASSEMBLY_INPUTS_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ASSEMBLY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory.java"
)
DEFAULT_ASSEMBLY_INPUTS_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE143_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ASSEMBLY_INPUTS_FACTORY,
        DEFAULT_ASSEMBLY_INPUTS_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase143-interaction-session-factory-runtime-bundle-default-assembly-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase143-interaction-session-factory-runtime-bundle-default-assembly-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase143_plan_text = _read(PHASE143_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_assembly_inputs_factory_text = _read(DEFAULT_ASSEMBLY_INPUTS_FACTORY)
    default_assembly_inputs_factory_test_text = _read(DEFAULT_ASSEMBLY_INPUTS_FACTORY_TEST)

    if "## Phase 143 Slice Status" not in phase143_plan_text:
        errors.append("phase143_plan_missing_slice_status")
    if "`143.1` complete." not in phase143_plan_text:
        errors.append("phase143_plan_missing_143_1_complete")
    if "`143.2` complete." not in phase143_plan_text:
        errors.append("phase143_plan_missing_143_2_complete")
    if "`143.3` complete." not in phase143_plan_text:
        errors.append("phase143_plan_missing_143_3_complete")

    if (
        "## Phase 143 (Interaction Session Factory Runtime Bundle Default Assembly Inputs Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase143_section")

    if "PHASE 143 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase143_started")
    if "PHASE 143 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase143_complete")

    required_tasks = [
        "- [x] Define Phase 143 interaction-session factory runtime-bundle default-assembly-inputs-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory` ownership for interaction-session runtime-bundle default assembly-input construction seams.",
        "- [x] Run Phase 143 verification + guard pack and mark `PHASE 143 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase143_line:{task_line}")

    required_default_assembly_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory",
        "static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(",
        "InteractionSessionFactoryInputs factoryInputs",
        "InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()",
        "InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(",
    ]
    for required_string in required_default_assembly_factory_strings:
        if required_string not in default_assembly_inputs_factory_text:
            errors.append(f"default_assembly_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactoryTest",
        "createDefaultAssemblyFactoryInputsBuildsAssemblyInputsWithPolicyDefaultSessionKey",
        "createDefaultAssemblyFactoryInputsBuildsAssemblyInputsWithProvidedSessionKey",
    ]
    for required_string in required_test_strings:
        if required_string not in default_assembly_inputs_factory_test_text:
            errors.append(f"default_assembly_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase143-interaction-session-factory-runtime-bundle-default-assembly-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase143-interaction-session-factory-runtime-bundle-default-assembly-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase143-interaction-session-factory-runtime-bundle-default-assembly-inputs-factory-extraction] OK: "
        "interaction session factory runtime-bundle default-assembly-inputs-factory extraction Phase 143 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
