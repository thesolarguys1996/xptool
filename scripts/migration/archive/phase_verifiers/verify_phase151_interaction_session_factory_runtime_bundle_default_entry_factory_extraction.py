from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE151_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE151_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_ENTRY_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.java"
)
DEFAULT_ENTRY_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE151_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_ENTRY_FACTORY,
        DEFAULT_ENTRY_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase151-interaction-session-factory-runtime-bundle-default-entry-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase151-interaction-session-factory-runtime-bundle-default-entry-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase151_plan_text = _read(PHASE151_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)
    default_entry_factory_test_text = _read(DEFAULT_ENTRY_FACTORY_TEST)

    if "## Phase 151 Slice Status" not in phase151_plan_text:
        errors.append("phase151_plan_missing_slice_status")
    if "`151.1` complete." not in phase151_plan_text:
        errors.append("phase151_plan_missing_151_1_complete")
    if "`151.2` complete." not in phase151_plan_text:
        errors.append("phase151_plan_missing_151_2_complete")
    if "`151.3` complete." not in phase151_plan_text:
        errors.append("phase151_plan_missing_151_3_complete")

    if (
        "## Phase 151 (Interaction Session Factory Runtime Bundle Default Entry Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase151_section")

    if "PHASE 151 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase151_started")
    if "PHASE 151 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase151_complete")

    required_tasks = [
        "- [x] Define Phase 151 interaction-session factory runtime-bundle default-entry-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleDefaultEntryFactory` ownership for interaction-session runtime-bundle default-entry creation seams.",
        "- [x] Run Phase 151 verification + guard pack and mark `PHASE 151 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase151_line:{task_line}")

    required_default_entry_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleDefaultEntryFactory",
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_entry_factory_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleDefaultEntryFactoryTest",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithPolicyDefaultSessionKey",
        "createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithProvidedSessionKey",
        "exposesRuntimeBundleDefaultEntryFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_entry_factory_test_text:
            errors.append(f"default_entry_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase151-interaction-session-factory-runtime-bundle-default-entry-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase151-interaction-session-factory-runtime-bundle-default-entry-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase151-interaction-session-factory-runtime-bundle-default-entry-factory-extraction] OK: "
        "interaction session factory runtime-bundle default-entry-factory extraction Phase 151 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
