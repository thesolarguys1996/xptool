from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE136_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE136_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_ASSEMBLY_ENTRY_TYPED_ROUTING_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)
RUNTIME_BUNDLE_FACTORY_INPUTS_ASSEMBLY_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE136_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY,
        RUNTIME_BUNDLE_FACTORY_INPUTS_ASSEMBLY_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase136-interaction-session-factory-runtime-bundle-assembly-entry-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase136-interaction-session-factory-runtime-bundle-assembly-entry-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase136_plan_text = _read(PHASE136_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    runtime_bundle_factory_inputs_assembly_factory_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS_ASSEMBLY_FACTORY)

    if "## Phase 136 Slice Status" not in phase136_plan_text:
        errors.append("phase136_plan_missing_slice_status")
    if "`136.1` complete." not in phase136_plan_text:
        errors.append("phase136_plan_missing_136_1_complete")
    if "`136.2` complete." not in phase136_plan_text:
        errors.append("phase136_plan_missing_136_2_complete")
    if "`136.3` complete." not in phase136_plan_text:
        errors.append("phase136_plan_missing_136_3_complete")

    if (
        "## Phase 136 (Interaction Session Factory Runtime Bundle Assembly Entry Typed Routing Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase136_section")

    if "PHASE 136 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase136_started")
    if "PHASE 136 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase136_complete")

    required_tasks = [
        "- [x] Define Phase 136 interaction-session factory runtime-bundle assembly-entry typed-routing extraction scope and completion evidence gates.",
        "- [x] Extract typed assembly-entry runtime-bundle routing ownership through `InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory`.",
        "- [x] Run Phase 136 verification + guard pack and mark `PHASE 136 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase136_line:{task_line}")

    required_runtime_bundle_factory_strings = [
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromAssemblyFactoryInputs(",
        "return createRuntimeBundleFromInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(",
        "return InteractionSessionAssemblyFactory.createRuntimeBundleForSession(assemblyFactoryInputs);",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    required_runtime_bundle_factory_inputs_assembly_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory",
        "createRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_runtime_bundle_factory_inputs_assembly_factory_strings:
        if required_string not in runtime_bundle_factory_inputs_assembly_factory_text:
            errors.append(
                "runtime_bundle_factory_inputs_assembly_factory_missing_string:"
                f"{required_string}"
            )

    if errors:
        print("[phase136-interaction-session-factory-runtime-bundle-assembly-entry-typed-routing-extraction] FAILED")
        for error in errors:
            print(
                "[phase136-interaction-session-factory-runtime-bundle-assembly-entry-typed-routing-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase136-interaction-session-factory-runtime-bundle-assembly-entry-typed-routing-extraction] OK: "
        "interaction session factory runtime-bundle assembly-entry typed-routing extraction Phase 136 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
