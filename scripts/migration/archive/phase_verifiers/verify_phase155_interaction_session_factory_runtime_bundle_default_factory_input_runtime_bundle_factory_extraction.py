from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE155_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE155_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.java"
)
DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactoryTest.java"
)
DEFAULT_RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE155_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY,
        DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY_TEST,
        DEFAULT_RUNTIME_BUNDLE_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase155-interaction-session-factory-runtime-bundle-default-factory-input-runtime-bundle-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase155-interaction-session-factory-runtime-bundle-default-factory-input-runtime-bundle-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase155_plan_text = _read(PHASE155_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_factory_input_runtime_bundle_factory_text = _read(DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY)
    default_factory_input_runtime_bundle_factory_test_text = _read(DEFAULT_FACTORY_INPUT_RUNTIME_BUNDLE_FACTORY_TEST)
    default_runtime_bundle_factory_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY)

    if "## Phase 155 Slice Status" not in phase155_plan_text:
        errors.append("phase155_plan_missing_slice_status")
    if "`155.1` complete." not in phase155_plan_text:
        errors.append("phase155_plan_missing_155_1_complete")
    if "`155.2` complete." not in phase155_plan_text:
        errors.append("phase155_plan_missing_155_2_complete")
    if "`155.3` complete." not in phase155_plan_text:
        errors.append("phase155_plan_missing_155_3_complete")

    if (
        "## Phase 155 (Interaction Session Factory Runtime Bundle Default Factory Input Runtime Bundle Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase155_section")

    if "PHASE 155 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase155_started")
    if "PHASE 155 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase155_complete")

    required_tasks = [
        "- [x] Define Phase 155 interaction-session factory runtime-bundle default-factory-input runtime-bundle-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory` ownership for default runtime-bundle-factory-input runtime-bundle creation seams.",
        "- [x] Run Phase 155 verification + guard pack and mark `PHASE 155 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase155_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_factory_input_runtime_bundle_factory_text:
            errors.append(f"default_factory_input_runtime_bundle_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactoryTest",
        "exposesRuntimeBundleDefaultFactoryInputRuntimeBundleFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_factory_input_runtime_bundle_factory_test_text:
            errors.append(f"default_factory_input_runtime_bundle_factory_test_missing_string:{required_string}")

    if (
        "InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs("
        not in default_runtime_bundle_factory_text
    ):
        errors.append("default_runtime_bundle_factory_missing_focused_delegate")

    if errors:
        print("[phase155-interaction-session-factory-runtime-bundle-default-factory-input-runtime-bundle-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase155-interaction-session-factory-runtime-bundle-default-factory-input-runtime-bundle-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase155-interaction-session-factory-runtime-bundle-default-factory-input-runtime-bundle-factory-extraction] OK: "
        "interaction session factory runtime-bundle default-factory-input runtime-bundle-factory extraction Phase 155 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
