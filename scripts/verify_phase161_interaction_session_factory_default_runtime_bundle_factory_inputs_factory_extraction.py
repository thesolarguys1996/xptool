from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE161_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE161_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.java"
)
DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE161_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY,
        DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase161-interaction-session-factory-default-runtime-bundle-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase161-interaction-session-factory-default-runtime-bundle-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase161_plan_text = _read(PHASE161_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_runtime_bundle_factory_inputs_factory_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY)
    default_runtime_bundle_factory_inputs_factory_test_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_FACTORY_TEST)

    if "## Phase 161 Slice Status" not in phase161_plan_text:
        errors.append("phase161_plan_missing_slice_status")
    if "`161.1` complete." not in phase161_plan_text:
        errors.append("phase161_plan_missing_161_1_complete")
    if "`161.2` complete." not in phase161_plan_text:
        errors.append("phase161_plan_missing_161_2_complete")
    if "`161.3` complete." not in phase161_plan_text:
        errors.append("phase161_plan_missing_161_3_complete")

    if (
        "## Phase 161 (Interaction Session Factory Default Runtime Bundle Factory Inputs Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase161_section")

    if "PHASE 161 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase161_started")
    if "PHASE 161 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase161_complete")

    required_tasks = [
        "- [x] Define Phase 161 interaction-session factory default runtime-bundle-factory-inputs-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory` ownership for interaction-session default runtime-bundle-factory-input construction seams.",
        "- [x] Run Phase 161 verification + guard pack and mark `PHASE 161 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase161_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory",
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_runtime_bundle_factory_inputs_factory_text:
            errors.append(f"default_runtime_bundle_factory_inputs_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactoryTest",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithPolicyDefaultSessionKey",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithProvidedSessionKey",
        "exposesDefaultRuntimeBundleFactoryInputsFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_runtime_bundle_factory_inputs_factory_test_text:
            errors.append(f"default_runtime_bundle_factory_inputs_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase161-interaction-session-factory-default-runtime-bundle-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase161-interaction-session-factory-default-runtime-bundle-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase161-interaction-session-factory-default-runtime-bundle-factory-inputs-factory-extraction] OK: "
        "interaction session factory default runtime-bundle-factory-inputs-factory extraction Phase 161 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
