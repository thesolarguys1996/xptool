from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE102_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE102_INTERACTION_SESSION_RUNTIME_CONTROL_BUNDLE_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_CONTROL_BUNDLE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeControlBundle.java"
)
RUNTIME_BUNDLE = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java"
)
RUNTIME_BUNDLE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java"
)
ASSEMBLY_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE102_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_CONTROL_BUNDLE,
        RUNTIME_BUNDLE,
        RUNTIME_BUNDLE_FACTORY,
        RUNTIME_BUNDLE_FACTORY_TEST,
        ASSEMBLY_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase102-interaction-session-runtime-control-bundle-extraction] FAILED")
        for error in errors:
            print(f"[phase102-interaction-session-runtime-control-bundle-extraction] ERROR {error}")
        return 1

    phase102_plan_text = _read(PHASE102_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_control_bundle_text = _read(RUNTIME_CONTROL_BUNDLE)
    runtime_bundle_text = _read(RUNTIME_BUNDLE)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    runtime_bundle_factory_test_text = _read(RUNTIME_BUNDLE_FACTORY_TEST)
    assembly_factory_test_text = _read(ASSEMBLY_FACTORY_TEST)

    if "## Phase 102 Slice Status" not in phase102_plan_text:
        errors.append("phase102_plan_missing_slice_status")
    if "`102.1` complete." not in phase102_plan_text:
        errors.append("phase102_plan_missing_102_1_complete")
    if "`102.2` complete." not in phase102_plan_text:
        errors.append("phase102_plan_missing_102_2_complete")
    if "`102.3` complete." not in phase102_plan_text:
        errors.append("phase102_plan_missing_102_3_complete")

    if "## Phase 102 (Interaction Session Runtime Control Bundle Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase102_section")

    if "PHASE 102 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase102_started")
    if "PHASE 102 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase102_complete")

    required_tasks = [
        "- [x] Define Phase 102 interaction-session runtime-control bundle extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionRuntimeControlBundle` ownership and route runtime-bundle composition through runtime-ops + runtime-control bundle seams.",
        "- [x] Run Phase 102 verification + guard pack and mark `PHASE 102 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase102_line:{task_line}")

    required_runtime_control_bundle_strings = [
        "final class InteractionSessionRuntimeControlBundle",
        "final InteractionSessionRegistrationService interactionSessionRegistrationService;",
        "final InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService;",
        "final InteractionPostClickSettleService interactionPostClickSettleService;",
    ]
    for required_string in required_runtime_control_bundle_strings:
        if required_string not in runtime_control_bundle_text:
            errors.append(f"runtime_control_bundle_missing_string:{required_string}")

    required_runtime_bundle_strings = [
        "private final InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle;",
        "private final InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle;",
        "InteractionSessionRuntimeBundle(",
        "InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle,",
        "InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle",
        "InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle()",
        "this.interactionSessionRuntimeOperationsBundle = new InteractionSessionRuntimeOperationsBundle(",
    ]
    for required_string in required_runtime_bundle_strings:
        if required_string not in runtime_bundle_text:
            errors.append(f"runtime_bundle_missing_string:{required_string}")

    removed_runtime_bundle_fields = [
        "final InteractionSessionRegistrationService interactionSessionRegistrationService;",
        "final InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService;",
        "final InteractionPostClickSettleService interactionPostClickSettleService;",
    ]
    for removed_field in removed_runtime_bundle_fields:
        if removed_field in runtime_bundle_text:
            errors.append(f"runtime_bundle_still_contains_removed_field:{removed_field}")

    required_runtime_bundle_factory_strings = [
        "InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle =",
        "InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle =",
        "return new InteractionSessionRuntimeBundle(",
        "interactionSessionRuntimeOperationsBundle,",
        "interactionSessionRuntimeControlBundle",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    required_test_strings = [
        "InteractionSessionRuntimeControlBundle runtimeControlBundle = bundle.interactionSessionRuntimeControlBundle();",
        "assertSame(registrationService, runtimeControlBundle.interactionSessionRegistrationService);",
        "assertSame(motorOwnershipService, runtimeControlBundle.interactionSessionMotorOwnershipService);",
        "assertSame(postClickSettleService, runtimeControlBundle.interactionPostClickSettleService);",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_bundle_factory_test_text:
            errors.append(f"runtime_bundle_factory_test_missing_string:{required_string}")
        if required_string not in assembly_factory_test_text:
            errors.append(f"assembly_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase102-interaction-session-runtime-control-bundle-extraction] FAILED")
        for error in errors:
            print(f"[phase102-interaction-session-runtime-control-bundle-extraction] ERROR {error}")
        return 1

    print(
        "[phase102-interaction-session-runtime-control-bundle-extraction] OK: interaction session runtime control "
        "bundle extraction Phase 102 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
