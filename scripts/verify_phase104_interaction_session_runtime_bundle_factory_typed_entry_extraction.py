from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE104_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE104_INTERACTION_SESSION_RUNTIME_BUNDLE_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactory.java"
)
RUNTIME_BUNDLE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionRuntimeBundleFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE104_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        RUNTIME_BUNDLE_FACTORY,
        RUNTIME_BUNDLE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase104-interaction-session-runtime-bundle-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase104-interaction-session-runtime-bundle-factory-typed-entry-extraction] ERROR {error}")
        return 1

    phase104_plan_text = _read(PHASE104_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    runtime_bundle_factory_test_text = _read(RUNTIME_BUNDLE_FACTORY_TEST)

    if "## Phase 104 Slice Status" not in phase104_plan_text:
        errors.append("phase104_plan_missing_slice_status")
    if "`104.1` complete." not in phase104_plan_text:
        errors.append("phase104_plan_missing_104_1_complete")
    if "`104.2` complete." not in phase104_plan_text:
        errors.append("phase104_plan_missing_104_2_complete")
    if "`104.3` complete." not in phase104_plan_text:
        errors.append("phase104_plan_missing_104_3_complete")

    if "## Phase 104 (Interaction Session Runtime Bundle Factory Typed Entry Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase104_section")

    if "PHASE 104 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase104_started")
    if "PHASE 104 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase104_complete")

    required_tasks = [
        "- [x] Define Phase 104 interaction-session runtime-bundle-factory typed-entry extraction scope and completion evidence gates.",
        "- [x] Extract typed-entry runtime-bundle creation seam in `InteractionSessionRuntimeBundleFactory` ownership.",
        "- [x] Run Phase 104 verification + guard pack and mark `PHASE 104 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase104_line:{task_line}")

    required_factory_strings = [
        "static InteractionSessionRuntimeBundle createRuntimeBundle(",
        "InteractionSessionRuntimeBundleFactoryInputs runtimeBundleFactoryInputs",
        "return createRuntimeBundle(",
    ]
    for required_string in required_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    required_test_strings = [
        "InteractionSessionRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =",
        "InteractionSessionRuntimeBundleFactory.createRuntimeBundle(",
        "InteractionSessionRuntimeBundle typedInputBundle =",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_bundle_factory_test_text:
            errors.append(f"runtime_bundle_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase104-interaction-session-runtime-bundle-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase104-interaction-session-runtime-bundle-factory-typed-entry-extraction] ERROR {error}")
        return 1

    print(
        "[phase104-interaction-session-runtime-bundle-factory-typed-entry-extraction] OK: interaction session runtime "
        "bundle factory typed-entry extraction Phase 104 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
