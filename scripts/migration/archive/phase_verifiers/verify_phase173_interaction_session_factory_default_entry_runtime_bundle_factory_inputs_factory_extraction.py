from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE173_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE173_INTERACTION_SESSION_FACTORY_DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.java"
)
DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE173_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY,
        DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase173-interaction-session-factory-default-entry-runtime-bundle-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase173-interaction-session-factory-default-entry-runtime-bundle-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase173_plan_text = _read(PHASE173_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_runtime_bundle_factory_inputs_factory_text = _read(DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY)
    default_entry_runtime_bundle_factory_inputs_factory_test_text = _read(DEFAULT_ENTRY_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_TEST)

    if "## Phase 173 Slice Status" not in phase173_plan_text:
        errors.append("phase173_plan_missing_slice_status")
    if "`173.1` complete." not in phase173_plan_text:
        errors.append("phase173_plan_missing_173_1_complete")
    if "`173.2` complete." not in phase173_plan_text:
        errors.append("phase173_plan_missing_173_2_complete")
    if "`173.3` complete." not in phase173_plan_text:
        errors.append("phase173_plan_missing_173_3_complete")

    if (
        "## Phase 173 (Interaction Session Factory Default Entry Runtime Bundle Factory Inputs Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase173_section")

    if "PHASE 173 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase173_started")
    if "PHASE 173 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase173_complete")

    required_tasks = [
        "- [x] Define Phase 173 interaction-session factory default-entry-runtime-bundle-factory-inputs-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory` ownership for interaction-session default-entry runtime-bundle-factory-input construction seams.",
        "- [x] Run Phase 173 verification + guard pack and mark `PHASE 173 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase173_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory",
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_entry_runtime_bundle_factory_inputs_factory_text:
            errors.append(f"default_entry_runtime_bundle_factory_inputs_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactoryTest",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithPolicyDefaultSessionKey",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithProvidedSessionKey",
        "exposesDefaultEntryRuntimeBundleFactoryInputsFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_entry_runtime_bundle_factory_inputs_factory_test_text:
            errors.append(f"default_entry_runtime_bundle_factory_inputs_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase173-interaction-session-factory-default-entry-runtime-bundle-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase173-interaction-session-factory-default-entry-runtime-bundle-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase173-interaction-session-factory-default-entry-runtime-bundle-factory-inputs-factory-extraction] OK: "
        "interaction session factory default-entry-runtime-bundle-factory-inputs-factory extraction Phase 173 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
