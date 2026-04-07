from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE186_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE186_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
ENTRY_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryEntrySessionFactory.java"
)
DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE186_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        ENTRY_SESSION_FACTORY,
        DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase186-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase186-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase186_plan_text = _read(PHASE186_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    entry_session_factory_text = _read(ENTRY_SESSION_FACTORY)
    default_runtime_bundle_factory_inputs_session_factory_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY)

    if "## Phase 186 Slice Status" not in phase186_plan_text:
        errors.append("phase186_plan_missing_slice_status")
    if "`186.1` complete." not in phase186_plan_text:
        errors.append("phase186_plan_missing_186_1_complete")
    if "`186.2` complete." not in phase186_plan_text:
        errors.append("phase186_plan_missing_186_2_complete")
    if "`186.3` complete." not in phase186_plan_text:
        errors.append("phase186_plan_missing_186_3_complete")

    if (
        "## Phase 186 (Interaction Session Factory Default Runtime Bundle Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase186_section")

    if "PHASE 186 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase186_started")
    if "PHASE 186 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase186_complete")

    required_tasks = [
        "- [x] Define Phase 186 interaction-session factory default-runtime-bundle-factory-input typed routing extraction scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(...)` through focused default-runtime-bundle-factory-input session routing ownership.",
        "- [x] Run Phase 186 verification + guard pack and mark `PHASE 186 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase186_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryEntrySessionFactory.createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryDefaultEntryFactory.createFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    if "InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(" not in entry_session_factory_text:
        errors.append("entry_session_factory_missing_default_runtime_bundle_factory_inputs_session_factory_delegate")

    if "InteractionSessionFactoryDefaultEntryFactory.createFromDefaultRuntimeBundleFactoryInputs(" not in default_runtime_bundle_factory_inputs_session_factory_text:
        errors.append("default_runtime_bundle_factory_inputs_session_factory_missing_default_entry_delegate")

    if errors:
        print("[phase186-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase186-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase186-interaction-session-factory-default-runtime-bundle-factory-input-typed-routing-extraction] OK: "
        "interaction session factory default-runtime-bundle-factory-input typed routing extraction Phase 186 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
