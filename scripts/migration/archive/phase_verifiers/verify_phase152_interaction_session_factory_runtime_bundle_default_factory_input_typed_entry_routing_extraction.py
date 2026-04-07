from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE152_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE152_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_TYPED_ENTRY_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE152_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY,
        DEFAULT_ENTRY_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase152-interaction-session-factory-runtime-bundle-default-factory-input-typed-entry-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase152-interaction-session-factory-runtime-bundle-default-factory-input-typed-entry-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase152_plan_text = _read(PHASE152_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)

    if "## Phase 152 Slice Status" not in phase152_plan_text:
        errors.append("phase152_plan_missing_slice_status")
    if "`152.1` complete." not in phase152_plan_text:
        errors.append("phase152_plan_missing_152_1_complete")
    if "`152.2` complete." not in phase152_plan_text:
        errors.append("phase152_plan_missing_152_2_complete")
    if "`152.3` complete." not in phase152_plan_text:
        errors.append("phase152_plan_missing_152_3_complete")

    if (
        "## Phase 152 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Entry Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase152_section")

    if "PHASE 152 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase152_started")
    if "PHASE 152 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase152_complete")

    required_tasks = [
        "- [x] Define Phase 152 interaction-session factory runtime-bundle default-factory-input typed-entry-routing extraction scope and completion evidence gates.",
        "- [x] Extract typed default-entry runtime-bundle routing ownership through `InteractionSessionFactoryRuntimeBundleDefaultEntryFactory`.",
        "- [x] Run Phase 152 verification + guard pack and mark `PHASE 152 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase152_line:{task_line}")

    required_runtime_bundle_factory_strings = [
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createRuntimeBundleFromFactoryInputs(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "return createRuntimeBundleFromInputs(defaultRuntimeBundleFactoryInputs);",
        "return createRuntimeBundleFromInputs(",
        "createDefaultRuntimeBundleFactoryInputs(",
        "return createRuntimeBundleFromAssemblyFactoryInputs(",
        "createDefaultAssemblyFactoryInputs(",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    required_default_entry_factory_strings = [
        "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_entry_factory_strings:
        if required_string not in default_entry_factory_text:
            errors.append(f"default_entry_factory_missing_string:{required_string}")

    if errors:
        print("[phase152-interaction-session-factory-runtime-bundle-default-factory-input-typed-entry-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase152-interaction-session-factory-runtime-bundle-default-factory-input-typed-entry-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase152-interaction-session-factory-runtime-bundle-default-factory-input-typed-entry-routing-extraction] OK: "
        "interaction session factory runtime-bundle default-factory-input typed-entry-routing extraction Phase 152 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
