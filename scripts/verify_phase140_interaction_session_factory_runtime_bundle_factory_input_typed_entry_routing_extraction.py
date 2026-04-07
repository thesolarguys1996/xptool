from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE140_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE140_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_INPUT_TYPED_ENTRY_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)
INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE140_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY,
        INPUTS_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase140-interaction-session-factory-runtime-bundle-factory-input-typed-entry-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase140-interaction-session-factory-runtime-bundle-factory-input-typed-entry-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase140_plan_text = _read(PHASE140_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    inputs_factory_text = _read(INPUTS_FACTORY)

    if "## Phase 140 Slice Status" not in phase140_plan_text:
        errors.append("phase140_plan_missing_slice_status")
    if "`140.1` complete." not in phase140_plan_text:
        errors.append("phase140_plan_missing_140_1_complete")
    if "`140.2` complete." not in phase140_plan_text:
        errors.append("phase140_plan_missing_140_2_complete")
    if "`140.3` complete." not in phase140_plan_text:
        errors.append("phase140_plan_missing_140_3_complete")

    if (
        "## Phase 140 (Interaction Session Factory Runtime Bundle Factory Input Typed Entry Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase140_section")

    if "PHASE 140 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase140_started")
    if "PHASE 140 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase140_complete")

    required_tasks = [
        "- [x] Define Phase 140 interaction-session factory runtime-bundle-factory input typed-entry routing extraction scope and completion evidence gates.",
        "- [x] Extract typed runtime-bundle-factory-input entry routing ownership through `InteractionSessionFactoryRuntimeBundleFactoryInputsFactory`.",
        "- [x] Run Phase 140 verification + guard pack and mark `PHASE 140 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase140_line:{task_line}")

    required_runtime_bundle_factory_strings = [
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryInputs factoryInputs",
        "InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(",
        "return createRuntimeBundleFromInputs(",
        "createRuntimeBundleFactoryInputs(assemblyFactoryInputs)",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    if "final class InteractionSessionFactoryRuntimeBundleFactoryInputsFactory" not in inputs_factory_text:
        errors.append("inputs_factory_missing_class")

    if errors:
        print("[phase140-interaction-session-factory-runtime-bundle-factory-input-typed-entry-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase140-interaction-session-factory-runtime-bundle-factory-input-typed-entry-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase140-interaction-session-factory-runtime-bundle-factory-input-typed-entry-routing-extraction] OK: "
        "interaction session factory runtime-bundle-factory input typed-entry routing extraction Phase 140 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
