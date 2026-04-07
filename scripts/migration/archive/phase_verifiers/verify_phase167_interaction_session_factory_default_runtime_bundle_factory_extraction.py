from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE167_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE167_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactory.java"
)
DEFAULT_RUNTIME_BUNDLE_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeBundleFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE167_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_RUNTIME_BUNDLE_FACTORY,
        DEFAULT_RUNTIME_BUNDLE_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase167-interaction-session-factory-default-runtime-bundle-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase167-interaction-session-factory-default-runtime-bundle-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase167_plan_text = _read(PHASE167_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_runtime_bundle_factory_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY)
    default_runtime_bundle_factory_test_text = _read(DEFAULT_RUNTIME_BUNDLE_FACTORY_TEST)

    if "## Phase 167 Slice Status" not in phase167_plan_text:
        errors.append("phase167_plan_missing_slice_status")
    if "`167.1` complete." not in phase167_plan_text:
        errors.append("phase167_plan_missing_167_1_complete")
    if "`167.2` complete." not in phase167_plan_text:
        errors.append("phase167_plan_missing_167_2_complete")
    if "`167.3` complete." not in phase167_plan_text:
        errors.append("phase167_plan_missing_167_3_complete")

    if "## Phase 167 (Interaction Session Factory Default Runtime Bundle Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase167_section")

    if "PHASE 167 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase167_started")
    if "PHASE 167 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase167_complete")

    required_tasks = [
        "- [x] Define Phase 167 interaction-session factory default-runtime-bundle-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultRuntimeBundleFactory` ownership for interaction-session default runtime-bundle creation seams.",
        "- [x] Run Phase 167 verification + guard pack and mark `PHASE 167 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase167_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryDefaultRuntimeBundleFactory",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_factory_strings:
        if required_string not in default_runtime_bundle_factory_text:
            errors.append(f"default_runtime_bundle_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryDefaultRuntimeBundleFactoryTest",
        "exposesDefaultRuntimeBundleFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in default_runtime_bundle_factory_test_text:
            errors.append(f"default_runtime_bundle_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase167-interaction-session-factory-default-runtime-bundle-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase167-interaction-session-factory-default-runtime-bundle-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase167-interaction-session-factory-default-runtime-bundle-factory-extraction] OK: "
        "interaction session factory default-runtime-bundle-factory extraction Phase 167 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
