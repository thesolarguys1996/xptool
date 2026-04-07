from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE147_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE147_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUTS_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_FACTORY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.java"
)
DEFAULT_FACTORY_INPUTS_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE147_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_FACTORY_INPUTS_FACTORY,
        DEFAULT_FACTORY_INPUTS_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase147-interaction-session-factory-runtime-bundle-default-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase147-interaction-session-factory-runtime-bundle-default-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase147_plan_text = _read(PHASE147_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_factory_inputs_factory_text = _read(DEFAULT_FACTORY_INPUTS_FACTORY)
    default_factory_inputs_factory_test_text = _read(DEFAULT_FACTORY_INPUTS_FACTORY_TEST)

    if "## Phase 147 Slice Status" not in phase147_plan_text:
        errors.append("phase147_plan_missing_slice_status")
    if "`147.1` complete." not in phase147_plan_text:
        errors.append("phase147_plan_missing_147_1_complete")
    if "`147.2` complete." not in phase147_plan_text:
        errors.append("phase147_plan_missing_147_2_complete")
    if "`147.3` complete." not in phase147_plan_text:
        errors.append("phase147_plan_missing_147_3_complete")

    if (
        "## Phase 147 (Interaction Session Factory Runtime Bundle Default Factory Inputs Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase147_section")

    if "PHASE 147 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase147_started")
    if "PHASE 147 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase147_complete")

    required_tasks = [
        "- [x] Define Phase 147 interaction-session factory runtime-bundle default-factory-inputs-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory` ownership for interaction-session runtime-bundle default factory-input construction seams.",
        "- [x] Run Phase 147 verification + guard pack and mark `PHASE 147 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase147_line:{task_line}")

    required_default_factory_inputs_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory",
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryInputs factoryInputs",
        "InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_factory_inputs_factory_strings:
        if required_string not in default_factory_inputs_factory_text:
            errors.append(f"default_factory_inputs_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactoryTest",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithPolicyDefaultSessionKey",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithProvidedSessionKey",
    ]
    for required_string in required_test_strings:
        if required_string not in default_factory_inputs_factory_test_text:
            errors.append(f"default_factory_inputs_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase147-interaction-session-factory-runtime-bundle-default-factory-inputs-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase147-interaction-session-factory-runtime-bundle-default-factory-inputs-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase147-interaction-session-factory-runtime-bundle-default-factory-inputs-factory-extraction] OK: "
        "interaction session factory runtime-bundle default-factory-inputs-factory extraction Phase 147 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
