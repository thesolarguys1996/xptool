from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE157_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE157_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_WIRING_CONSOLIDATION_Z_PLAN.md"
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

    required_paths = [PHASE157_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, RUNTIME_BUNDLE_FACTORY]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase157-interaction-session-factory-runtime-bundle-factory-wiring-consolidation-z] FAILED")
        for error in errors:
            print(f"[phase157-interaction-session-factory-runtime-bundle-factory-wiring-consolidation-z] ERROR {error}")
        return 1

    phase157_plan_text = _read(PHASE157_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)

    if "## Phase 157 Slice Status" not in phase157_plan_text:
        errors.append("phase157_plan_missing_slice_status")
    if "`157.1` complete." not in phase157_plan_text:
        errors.append("phase157_plan_missing_157_1_complete")
    if "`157.2` complete." not in phase157_plan_text:
        errors.append("phase157_plan_missing_157_2_complete")
    if "`157.3` complete." not in phase157_plan_text:
        errors.append("phase157_plan_missing_157_3_complete")

    if "## Phase 157 (Interaction Session Factory Runtime Bundle Factory Wiring Consolidation Z)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase157_section")

    if "PHASE 157 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase157_started")
    if "PHASE 157 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase157_complete")

    required_tasks = [
        "- [x] Define Phase 157 interaction-session factory runtime-bundle-factory wiring consolidation Z scope and completion evidence gates.",
        "- [x] Consolidate `InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(...)` through focused default-entry routing ownership.",
        "- [x] Run Phase 157 verification + guard pack and mark `PHASE 157 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase157_line:{task_line}")

    required_factory_strings = [
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "return createRuntimeBundleFromInputs(defaultRuntimeBundleFactoryInputs);",
    ]
    for required_string in required_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    if errors:
        print("[phase157-interaction-session-factory-runtime-bundle-factory-wiring-consolidation-z] FAILED")
        for error in errors:
            print(f"[phase157-interaction-session-factory-runtime-bundle-factory-wiring-consolidation-z] ERROR {error}")
        return 1

    print(
        "[phase157-interaction-session-factory-runtime-bundle-factory-wiring-consolidation-z] OK: "
        "interaction session factory runtime-bundle-factory wiring consolidation Z Phase 157 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
