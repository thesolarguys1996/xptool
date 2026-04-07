from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE181_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE181_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.java"
)
RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE181_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
        RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase181-interaction-session-factory-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase181-interaction-session-factory-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase181_plan_text = _read(PHASE181_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_inputs_session_factory_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY)
    runtime_bundle_factory_inputs_session_factory_test_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST)

    if "## Phase 181 Slice Status" not in phase181_plan_text:
        errors.append("phase181_plan_missing_slice_status")
    if "`181.1` complete." not in phase181_plan_text:
        errors.append("phase181_plan_missing_181_1_complete")
    if "`181.2` complete." not in phase181_plan_text:
        errors.append("phase181_plan_missing_181_2_complete")
    if "`181.3` complete." not in phase181_plan_text:
        errors.append("phase181_plan_missing_181_3_complete")

    if (
        "## Phase 181 (Interaction Session Factory Runtime Bundle Factory Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase181_section")

    if "PHASE 181 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase181_started")
    if "PHASE 181 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase181_complete")

    required_tasks = [
        "- [x] Define Phase 181 interaction-session factory runtime-bundle-factory-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory` ownership for interaction-session runtime-bundle-factory-input session creation seams.",
        "- [x] Run Phase 181 verification + guard pack and mark `PHASE 181 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase181_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory",
        "static InteractionSession createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in runtime_bundle_factory_inputs_session_factory_text:
            errors.append(f"runtime_bundle_factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest",
        "exposesRuntimeBundleFactoryInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_bundle_factory_inputs_session_factory_test_text:
            errors.append(f"runtime_bundle_factory_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase181-interaction-session-factory-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase181-interaction-session-factory-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase181-interaction-session-factory-runtime-bundle-factory-inputs-session-factory-extraction] OK: "
        "interaction session factory runtime-bundle-factory-inputs-session-factory extraction Phase 181 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
