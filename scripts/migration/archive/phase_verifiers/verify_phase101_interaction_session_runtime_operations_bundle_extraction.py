from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE101_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE101_INTERACTION_SESSION_RUNTIME_OPERATIONS_BUNDLE_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_RUNTIME_OPERATIONS_BUNDLE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsBundle.java"
)
INTERACTION_SESSION_RUNTIME_BUNDLE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundle.java"
)
INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeOperationsFactory.java"
)
INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java"
)
INTERACTION_SESSION_ASSEMBLY_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionAssemblyFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE101_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION_RUNTIME_OPERATIONS_BUNDLE,
        INTERACTION_SESSION_RUNTIME_BUNDLE,
        INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY,
        INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_TEST,
        INTERACTION_SESSION_ASSEMBLY_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase101-interaction-session-runtime-operations-bundle-extraction] FAILED")
        for error in errors:
            print(f"[phase101-interaction-session-runtime-operations-bundle-extraction] ERROR {error}")
        return 1

    phase101_plan_text = _read(PHASE101_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_operations_bundle_text = _read(INTERACTION_SESSION_RUNTIME_OPERATIONS_BUNDLE)
    runtime_bundle_text = _read(INTERACTION_SESSION_RUNTIME_BUNDLE)
    runtime_operations_factory_text = _read(INTERACTION_SESSION_RUNTIME_OPERATIONS_FACTORY)
    runtime_bundle_factory_test_text = _read(INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_TEST)
    assembly_factory_test_text = _read(INTERACTION_SESSION_ASSEMBLY_FACTORY_TEST)

    if "## Phase 101 Slice Status" not in phase101_plan_text:
        errors.append("phase101_plan_missing_slice_status")
    if "`101.1` complete." not in phase101_plan_text:
        errors.append("phase101_plan_missing_101_1_complete")
    if "`101.2` complete." not in phase101_plan_text:
        errors.append("phase101_plan_missing_101_2_complete")
    if "`101.3` complete." not in phase101_plan_text:
        errors.append("phase101_plan_missing_101_3_complete")

    if "## Phase 101 (Interaction Session Runtime Operations Bundle Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase101_section")

    if "PHASE 101 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase101_started")
    if "PHASE 101 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase101_complete")

    required_tasks = [
        "- [x] Define Phase 101 interaction-session runtime-operations bundle extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionRuntimeOperationsBundle` ownership and route runtime-operations factory delegation through that seam.",
        "- [x] Run Phase 101 verification + guard pack and mark `PHASE 101 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase101_line:{task_line}")

    required_runtime_operations_bundle_strings = [
        "final class InteractionSessionRuntimeOperationsBundle",
        "final InteractionSessionCommandRouter interactionSessionCommandRouter;",
        "final InteractionSessionClickEventService interactionSessionClickEventService;",
        "final InteractionSessionOwnershipService interactionSessionOwnershipService;",
        "final InteractionSessionShutdownService interactionSessionShutdownService;",
    ]
    for required_string in required_runtime_operations_bundle_strings:
        if required_string not in runtime_operations_bundle_text:
            errors.append(f"runtime_operations_bundle_missing_string:{required_string}")

    required_runtime_bundle_strings = [
        "private final InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle;",
        "this.interactionSessionRuntimeOperationsBundle = new InteractionSessionRuntimeOperationsBundle(",
        "InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle()",
    ]
    for required_string in required_runtime_bundle_strings:
        if required_string not in runtime_bundle_text:
            errors.append(f"runtime_bundle_missing_string:{required_string}")

    required_runtime_operations_factory_strings = [
        "return createFromRuntimeOperationsBundle(runtimeBundle.interactionSessionRuntimeOperationsBundle());",
        "static InteractionSessionRuntimeOperations createFromRuntimeOperationsBundle(",
    ]
    for required_string in required_runtime_operations_factory_strings:
        if required_string not in runtime_operations_factory_text:
            errors.append(f"runtime_operations_factory_missing_string:{required_string}")

    required_test_strings = [
        "InteractionSessionRuntimeOperationsBundle runtimeOperationsBundle =",
        "bundle.interactionSessionRuntimeOperationsBundle();",
        "assertSame(router, runtimeOperationsBundle.interactionSessionCommandRouter);",
        "assertSame(clickEventService, runtimeOperationsBundle.interactionSessionClickEventService);",
        "assertSame(ownershipService, runtimeOperationsBundle.interactionSessionOwnershipService);",
        "assertSame(shutdownService, runtimeOperationsBundle.interactionSessionShutdownService);",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_bundle_factory_test_text:
            errors.append(f"runtime_bundle_factory_test_missing_string:{required_string}")
        if required_string not in assembly_factory_test_text:
            errors.append(f"assembly_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase101-interaction-session-runtime-operations-bundle-extraction] FAILED")
        for error in errors:
            print(f"[phase101-interaction-session-runtime-operations-bundle-extraction] ERROR {error}")
        return 1

    print(
        "[phase101-interaction-session-runtime-operations-bundle-extraction] OK: interaction session runtime operations "
        "bundle extraction Phase 101 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
