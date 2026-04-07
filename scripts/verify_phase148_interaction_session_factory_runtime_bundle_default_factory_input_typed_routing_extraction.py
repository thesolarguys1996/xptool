from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE148_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE148_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_DEFAULT_FACTORY_INPUT_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)
DEFAULT_FACTORY_INPUTS_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE148_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY,
        DEFAULT_FACTORY_INPUTS_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase148-interaction-session-factory-runtime-bundle-default-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase148-interaction-session-factory-runtime-bundle-default-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase148_plan_text = _read(PHASE148_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    default_factory_inputs_factory_text = _read(DEFAULT_FACTORY_INPUTS_FACTORY)

    if "## Phase 148 Slice Status" not in phase148_plan_text:
        errors.append("phase148_plan_missing_slice_status")
    if "`148.1` complete." not in phase148_plan_text:
        errors.append("phase148_plan_missing_148_1_complete")
    if "`148.2` complete." not in phase148_plan_text:
        errors.append("phase148_plan_missing_148_2_complete")
    if "`148.3` complete." not in phase148_plan_text:
        errors.append("phase148_plan_missing_148_3_complete")

    if (
        "## Phase 148 (Interaction Session Factory Runtime Bundle Default Factory Input Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase148_section")

    if "PHASE 148 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase148_started")
    if "PHASE 148 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase148_complete")

    required_tasks = [
        "- [x] Define Phase 148 interaction-session factory runtime-bundle default-factory-input typed-routing extraction scope and completion evidence gates.",
        "- [x] Extract typed default-entry runtime-bundle routing ownership through `InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory`.",
        "- [x] Run Phase 148 verification + guard pack and mark `PHASE 148 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase148_line:{task_line}")

    required_runtime_bundle_factory_strings = [
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(",
        "return createRuntimeBundleFromInputs(",
        "createDefaultRuntimeBundleFactoryInputs(",
        "return createRuntimeBundleFromAssemblyFactoryInputs(",
        "createDefaultAssemblyFactoryInputs(",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    if "final class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory" not in default_factory_inputs_factory_text:
        errors.append("default_factory_inputs_factory_missing_class")

    if errors:
        print("[phase148-interaction-session-factory-runtime-bundle-default-factory-input-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase148-interaction-session-factory-runtime-bundle-default-factory-input-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase148-interaction-session-factory-runtime-bundle-default-factory-input-typed-routing-extraction] OK: "
        "interaction session factory runtime-bundle default-factory-input typed-routing extraction Phase 148 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
