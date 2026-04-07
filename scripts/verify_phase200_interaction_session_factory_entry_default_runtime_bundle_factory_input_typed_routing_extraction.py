from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE200_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE200_INTERACTION_SESSION_FACTORY_ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ENTRY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java"
)
ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE200_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ENTRY_SESSION_FACTORY,
        ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase200-interaction-session-factory-entry-default-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase200-interaction-session-factory-entry-default-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase200_plan_text = _read(PHASE200_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    entry_session_factory_text = _read(ENTRY_SESSION_FACTORY)
    entry_default_runtime_bundle_factory_inputs_session_factory_text = _read(
        ENTRY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY
    )

    if "## Phase 200 Slice Status" not in phase200_plan_text:
        errors.append("phase200_plan_missing_slice_status")
    if "`200.1` complete." not in phase200_plan_text:
        errors.append("phase200_plan_missing_200_1_complete")
    if "`200.2` complete." not in phase200_plan_text:
        errors.append("phase200_plan_missing_200_2_complete")
    if "`200.3` complete." not in phase200_plan_text:
        errors.append("phase200_plan_missing_200_3_complete")

    if (
        "## Phase 200 (Interaction Session Factory Entry Default Runtime Bundle Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase200_section")

    if "PHASE 200 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase200_started")
    if "PHASE 200 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase200_complete")

    required_tasks = [
        "- [x] Define Phase 200 interaction-session factory entry default-runtime-bundle-factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryEntrySessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` through focused entry default-runtime-bundle-factory-input session routing ownership.",
        "- [x] Run Phase 200 verification + guard pack and mark `PHASE 200 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase200_line:{task_line}")

    required_entry_session_strings = [
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_entry_session_strings:
        if required_string not in entry_session_factory_text:
            errors.append(f"entry_session_factory_missing_string:{required_string}")

    if "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(" not in entry_default_runtime_bundle_factory_inputs_session_factory_text:
        errors.append("entry_default_runtime_bundle_factory_inputs_session_factory_missing_default_runtime_bundle_delegate")

    if errors:
        print("[phase200-interaction-session-factory-entry-default-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase200-interaction-session-factory-entry-default-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase200-interaction-session-factory-entry-default-runtime-bundle-factory-input-typed-routing-extraction] OK: "
        "interaction session factory entry default-runtime-bundle-factory-input typed routing extraction Phase 200 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
