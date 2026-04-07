from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE119_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE119_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
RUNTIME_BUNDLE_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
)
RUNTIME_BUNDLE_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE119_PLAN,
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
        print("[phase119-interaction-session-factory-runtime-bundle-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase119-interaction-session-factory-runtime-bundle-factory-extraction] ERROR {error}")
        return 1

    phase119_plan_text = _read(PHASE119_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    runtime_bundle_factory_text = _read(RUNTIME_BUNDLE_FACTORY)
    runtime_bundle_factory_test_text = _read(RUNTIME_BUNDLE_FACTORY_TEST)

    if "## Phase 119 Slice Status" not in phase119_plan_text:
        errors.append("phase119_plan_missing_slice_status")
    if "`119.1` complete." not in phase119_plan_text:
        errors.append("phase119_plan_missing_119_1_complete")
    if "`119.2` complete." not in phase119_plan_text:
        errors.append("phase119_plan_missing_119_2_complete")
    if "`119.3` complete." not in phase119_plan_text:
        errors.append("phase119_plan_missing_119_3_complete")

    if "## Phase 119 (Interaction Session Factory Runtime Bundle Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase119_section")

    if "PHASE 119 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase119_started")
    if "PHASE 119 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase119_complete")

    required_tasks = [
        "- [x] Define Phase 119 interaction-session factory runtime-bundle-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleFactory` ownership for interaction-session factory runtime-bundle routing seams.",
        "- [x] Run Phase 119 verification + guard pack and mark `PHASE 119 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase119_line:{task_line}")

    required_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleFactory",
        "static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(",
        "static InteractionSessionRuntimeBundle createRuntimeBundleFromAssemblyFactoryInputs(",
        "InteractionSessionAssemblyFactory.createRuntimeBundleForSession(assemblyFactoryInputs)",
    ]
    for required_string in required_factory_strings:
        if required_string not in runtime_bundle_factory_text:
            errors.append(f"runtime_bundle_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleFactoryTest",
        "createDefaultAssemblyFactoryInputsBuildsAssemblyFactoryInputsWithProvidedDefaultSessionKey",
        "exposesRuntimeBundleRoutingEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_bundle_factory_test_text:
            errors.append(f"runtime_bundle_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase119-interaction-session-factory-runtime-bundle-factory-extraction] FAILED")
        for error in errors:
            print(f"[phase119-interaction-session-factory-runtime-bundle-factory-extraction] ERROR {error}")
        return 1

    print(
        "[phase119-interaction-session-factory-runtime-bundle-factory-extraction] OK: interaction session "
        "factory runtime-bundle-factory extraction Phase 119 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
