from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE185_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE185_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.java"
)
DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE185_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
        DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase185-interaction-session-factory-default-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase185-interaction-session-factory-default-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase185_plan_text = _read(PHASE185_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_runtime_bundle_factory_inputs_session_factory_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY)
    default_runtime_bundle_factory_inputs_session_factory_test_text = _read(
        DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST
    )

    if "## Phase 185 Slice Status" not in phase185_plan_text:
        errors.append("phase185_plan_missing_slice_status")
    if "`185.1` complete." not in phase185_plan_text:
        errors.append("phase185_plan_missing_185_1_complete")
    if "`185.2` complete." not in phase185_plan_text:
        errors.append("phase185_plan_missing_185_2_complete")
    if "`185.3` complete." not in phase185_plan_text:
        errors.append("phase185_plan_missing_185_3_complete")

    if (
        "## Phase 185 (Interaction Session Factory Default Runtime Bundle Factory Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase185_section")

    if "PHASE 185 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase185_started")
    if "PHASE 185 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase185_complete")

    required_tasks = [
        "- [x] Define Phase 185 interaction-session factory default-runtime-bundle-factory-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory` ownership for interaction-session default-runtime-bundle-factory-input session creation seams.",
        "- [x] Run Phase 185 verification + guard pack and mark `PHASE 185 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase185_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory",
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultEntryFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_runtime_bundle_factory_inputs_session_factory_text:
            errors.append(f"default_runtime_bundle_factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactoryTest",
        "exposesDefaultRuntimeBundleFactoryInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_runtime_bundle_factory_inputs_session_factory_test_text:
            errors.append(f"default_runtime_bundle_factory_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase185-interaction-session-factory-default-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase185-interaction-session-factory-default-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase185-interaction-session-factory-default-runtime-bundle-factory-inputs-session-factory-extraction] OK: "
        "interaction session factory default-runtime-bundle-factory-inputs-session-factory extraction Phase 185 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
