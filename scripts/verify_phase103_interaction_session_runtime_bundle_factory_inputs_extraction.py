from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE103_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE103_INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_INPUTS_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY_INPUTS = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryInputs.java"
)
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE103_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY_INPUTS,
        RUNTIME_BUNDLE_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase103-interaction-session-runtime-bundle-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase103-interaction-session-runtime-bundle-factory-inputs-extraction] ERROR {error}")
        return 1

    phase103_plan_text = _read(PHASE103_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_inputs_text = _read(RUNTIME_BUNDLE_FACTORY_INPUTS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)

    if "## Phase 103 Slice Status" not in phase103_plan_text:
        errors.append("phase103_plan_missing_slice_status")
    if "`103.1` complete." not in phase103_plan_text:
        errors.append("phase103_plan_missing_103_1_complete")
    if "`103.2` complete." not in phase103_plan_text:
        errors.append("phase103_plan_missing_103_2_complete")
    if "`103.3` complete." not in phase103_plan_text:
        errors.append("phase103_plan_missing_103_3_complete")

    if "## Phase 103 (Interaction Session Runtime Bundle Factory Inputs Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase103_section")

    if "PHASE 103 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase103_started")
    if "PHASE 103 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase103_complete")

    required_tasks = [
        "- [x] Define Phase 103 interaction-session runtime-bundle-factory inputs extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionRuntimeBundleFactoryInputs` ownership for runtime-bundle-factory typed inputs.",
        "- [x] Run Phase 103 verification + guard pack and mark `PHASE 103 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase103_line:{task_line}")

    required_inputs_strings = [
        "final class InteractionSessionRuntimeBundleFactoryInputs",
        "final InteractionSessionCommandRouter interactionSessionCommandRouter;",
        "final InteractionSessionRegistrationService interactionSessionRegistrationService;",
        "final InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService;",
        "final InteractionPostClickSettleService interactionPostClickSettleService;",
        "final InteractionSessionClickEventService interactionSessionClickEventService;",
        "final InteractionSessionOwnershipService interactionSessionOwnershipService;",
        "final InteractionSessionShutdownService interactionSessionShutdownService;",
        "static InteractionSessionRuntimeBundleFactoryInputs fromServices(",
        "InteractionSessionRuntimeOperationsBundle createRuntimeOperationsBundle()",
        "InteractionSessionRuntimeControlBundle createRuntimeControlBundle()",
    ]
    for required_string in required_inputs_strings:
        if required_string not in runtime_bundle_factory_inputs_text:
            errors.append(f"runtime_bundle_factory_inputs_missing_string:{required_string}")

    if "InteractionSessionRuntimeBundleFactoryInputs.fromServices(" not in runtime_bundle_factory_text:
        errors.append("runtime_bundle_factory_missing_typed_inputs_construction")

    if errors:
        print("[phase103-interaction-session-runtime-bundle-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase103-interaction-session-runtime-bundle-factory-inputs-extraction] ERROR {error}")
        return 1

    print(
        "[phase103-interaction-session-runtime-bundle-factory-inputs-extraction] OK: interaction session runtime bundle "
        "factory inputs extraction Phase 103 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
