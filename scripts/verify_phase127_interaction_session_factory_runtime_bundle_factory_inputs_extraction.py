from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE127_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE127_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY_INPUTS = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputs.java"
)
RUNTIME_BUNDLE_FACTORY_INPUTS_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE127_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY_INPUTS,
        RUNTIME_BUNDLE_FACTORY_INPUTS_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase127-interaction-session-factory-runtime-bundle-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase127-interaction-session-factory-runtime-bundle-factory-inputs-extraction] ERROR {error}")
        return 1

    phase127_plan_text = _read(PHASE127_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_inputs_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS)
    runtime_bundle_factory_inputs_test_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS_TEST)

    if "## Phase 127 Slice Status" not in phase127_plan_text:
        errors.append("phase127_plan_missing_slice_status")
    if "`127.1` complete." not in phase127_plan_text:
        errors.append("phase127_plan_missing_127_1_complete")
    if "`127.2` complete." not in phase127_plan_text:
        errors.append("phase127_plan_missing_127_2_complete")
    if "`127.3` complete." not in phase127_plan_text:
        errors.append("phase127_plan_missing_127_3_complete")

    if "## Phase 127 (Interaction Session Factory Runtime Bundle Factory Inputs Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase127_section")

    if "PHASE 127 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase127_started")
    if "PHASE 127 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase127_complete")

    required_tasks = [
        "- [x] Define Phase 127 interaction-session factory runtime-bundle-factory inputs extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleFactoryInputs` ownership for interaction-session factory runtime-bundle-factory typed inputs.",
        "- [x] Run Phase 127 verification + guard pack and mark `PHASE 127 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase127_line:{task_line}")

    required_inputs_strings = [
        "final class InteractionSessionFactoryRuntimeBundleFactoryInputs",
        "final InteractionSessionFactoryInputs factoryInputs;",
        "final String sessionInteractionKey;",
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs fromFactoryInputs(",
        "InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs()",
    ]
    for required_string in required_inputs_strings:
        if required_string not in runtime_bundle_factory_inputs_text:
            errors.append(f"runtime_bundle_factory_inputs_missing_string:{required_string}")

    required_inputs_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleFactoryInputsTest",
        "fromFactoryInputsRetainsProvidedReferencesAndSessionInteractionKey",
        "fromFactoryInputsUsesPolicyDefaultSessionInteractionKey",
        "createAssemblyFactoryInputsBuildsAssemblyInputsFromStoredFactoryInputsAndSessionKey",
    ]
    for required_string in required_inputs_test_strings:
        if required_string not in runtime_bundle_factory_inputs_test_text:
            errors.append(f"runtime_bundle_factory_inputs_test_missing_string:{required_string}")

    if errors:
        print("[phase127-interaction-session-factory-runtime-bundle-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase127-interaction-session-factory-runtime-bundle-factory-inputs-extraction] ERROR {error}")
        return 1

    print(
        "[phase127-interaction-session-factory-runtime-bundle-factory-inputs-extraction] OK: interaction session "
        "factory runtime-bundle-factory inputs extraction Phase 127 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
