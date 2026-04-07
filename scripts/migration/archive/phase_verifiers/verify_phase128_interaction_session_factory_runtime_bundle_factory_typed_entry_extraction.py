from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE128_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE128_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_TYPED_ENTRY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE128_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, RUNTIME_BUNDLE_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase128-interaction-session-factory-runtime-bundle-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase128-interaction-session-factory-runtime-bundle-factory-typed-entry-extraction] ERROR {error}")
        return 1

    phase128_plan_text = _read(PHASE128_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)

    if "## Phase 128 Slice Status" not in phase128_plan_text:
        errors.append("phase128_plan_missing_slice_status")
    if "`128.1` complete." not in phase128_plan_text:
        errors.append("phase128_plan_missing_128_1_complete")
    if "`128.2` complete." not in phase128_plan_text:
        errors.append("phase128_plan_missing_128_2_complete")
    if "`128.3` complete." not in phase128_plan_text:
        errors.append("phase128_plan_missing_128_3_complete")

    if (
        "## Phase 128 (Interaction Session Factory Runtime Bundle Factory Typed Entry Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase128_section")

    if "PHASE 128 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase128_started")
    if "PHASE 128 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase128_complete")

    required_tasks = [
        "- [x] Define Phase 128 interaction-session factory runtime-bundle-factory typed-entry extraction scope and completion evidence gates.",
        "- [x] Extract typed-entry runtime-bundle factory seam ownership through `InteractionSessionFactoryRuntimeBundleFactoryInputs`.",
        "- [x] Run Phase 128 verification + guard pack and mark `PHASE 128 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase128_line:{task_line}")

    required_runtime_bundle_factory_strings = [
        "static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(",
        "return createRuntimeBundleFromInputs(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromInputs(",
        "InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs",
        "runtimeBundleFactoryInputs.createAssemblyFactoryInputs()",
    ]
    for required_string in required_runtime_bundle_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    if errors:
        print("[phase128-interaction-session-factory-runtime-bundle-factory-typed-entry-extraction] FAILED")
        for error in errors:
            print(f"[phase128-interaction-session-factory-runtime-bundle-factory-typed-entry-extraction] ERROR {error}")
        return 1

    print(
        "[phase128-interaction-session-factory-runtime-bundle-factory-typed-entry-extraction] OK: interaction "
        "session factory runtime-bundle-factory typed-entry extraction Phase 128 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
