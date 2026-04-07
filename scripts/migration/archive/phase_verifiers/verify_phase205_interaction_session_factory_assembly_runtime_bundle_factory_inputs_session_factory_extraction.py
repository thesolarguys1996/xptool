from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE205_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE205_INTERACTION_SESSION_FACTORY_ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.java"
)
ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE205_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY,
        ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase205-interaction-session-factory-assembly-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase205-interaction-session-factory-assembly-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase205_plan_text = _read(PHASE205_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    assembly_runtime_bundle_factory_inputs_session_factory_text = _read(
        ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY
    )
    assembly_runtime_bundle_factory_inputs_session_factory_test_text = _read(
        ASSEMBLY_RUNTIME_BUNDLE_FACTORY_INPUTS_SESSION_FACTORY_TEST
    )

    if "## Phase 205 Slice Status" not in phase205_plan_text:
        errors.append("phase205_plan_missing_slice_status")
    if "`205.1` complete." not in phase205_plan_text:
        errors.append("phase205_plan_missing_205_1_complete")
    if "`205.2` complete." not in phase205_plan_text:
        errors.append("phase205_plan_missing_205_2_complete")
    if "`205.3` complete." not in phase205_plan_text:
        errors.append("phase205_plan_missing_205_3_complete")

    if (
        "## Phase 205 (Interaction Session Factory Assembly Runtime Bundle Factory Inputs Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase205_section")

    if "PHASE 205 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase205_started")
    if "PHASE 205 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase205_complete")

    required_tasks = [
        "- [x] Define Phase 205 interaction-session factory assembly-runtime-bundle-factory-inputs-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory` ownership for interaction-session assembly-runtime bundle-factory-input session routing seams.",
        "- [x] Run Phase 205 verification + guard pack and mark `PHASE 205 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase205_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory",
        "static InteractionSession createFromRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in assembly_runtime_bundle_factory_inputs_session_factory_text:
            errors.append(f"assembly_runtime_bundle_factory_inputs_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest",
        "exposesAssemblyRuntimeBundleFactoryInputsSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in assembly_runtime_bundle_factory_inputs_session_factory_test_text:
            errors.append(f"assembly_runtime_bundle_factory_inputs_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase205-interaction-session-factory-assembly-runtime-bundle-factory-inputs-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase205-interaction-session-factory-assembly-runtime-bundle-factory-inputs-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase205-interaction-session-factory-assembly-runtime-bundle-factory-inputs-session-factory-extraction] OK: "
        "interaction session factory assembly-runtime-bundle-factory-inputs-session-factory extraction Phase 205 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
