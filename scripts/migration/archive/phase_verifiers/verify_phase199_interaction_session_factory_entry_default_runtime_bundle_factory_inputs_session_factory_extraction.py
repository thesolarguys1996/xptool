from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE199_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE199_INTERACTION_SESSION_FACTORY_ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory.java"
)
ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE199_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
        ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase199-interaction-session-factory-entry-default-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase199-interaction-session-factory-entry-default-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase199_plan_text = _read(PHASE199_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    entry_default_runtime_bundle_factory_inputs_session_factory_text = _read(
        ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY
    )
    entry_default_runtime_bundle_factory_inputs_session_factory_test_text = _read(
        ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST
    )

    if "## Phase 199 Slice Status" not in phase199_plan_text:
        errors.append("phase199_plan_missing_slice_status")
    if "`199.1` complete." not in phase199_plan_text:
        errors.append("phase199_plan_missing_199_1_complete")
    if "`199.2` complete." not in phase199_plan_text:
        errors.append("phase199_plan_missing_199_2_complete")
    if "`199.3` complete." not in phase199_plan_text:
        errors.append("phase199_plan_missing_199_3_complete")

    if (
        "## Phase 199 (Interaction Session Factory Entry Default Runtime Bundle Factory Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase199_section")

    if "PHASE 199 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase199_started")
    if "PHASE 199 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase199_complete")

    required_tasks = [
        "- [x] Define Phase 199 interaction-session factory entry-default-runtime-bundle-factory-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory` ownership for interaction-session entry default-runtime-bundle-factory-input session routing seams.",
        "- [x] Run Phase 199 verification + guard pack and mark `PHASE 199 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase199_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory",
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in entry_default_runtime_bundle_factory_inputs_session_factory_text:
            errors.append(f"entry_default_runtime_bundle_factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactoryTest",
        "exposesEntryDefaultRuntimeBundleFactoryInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in entry_default_runtime_bundle_factory_inputs_session_factory_test_text:
            errors.append(f"entry_default_runtime_bundle_factory_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase199-interaction-session-factory-entry-default-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase199-interaction-session-factory-entry-default-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase199-interaction-session-factory-entry-default-runtime-bundle-factory-inputs-session-factory-extraction] OK: "
        "interaction session factory entry-default-runtime-bundle-factory-inputs-session-factory extraction Phase 199 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
