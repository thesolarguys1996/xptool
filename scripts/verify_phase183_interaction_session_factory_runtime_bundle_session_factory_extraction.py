from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE183_PLAN = (
    PROJECT_ROOT
    / "docs/NATIVE_CLIENT_PHASE183_INTERACTION_SESSION_FACTORY_RUNTIME_BUNDLE_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
RUNTIME_BUNDLE_SESSION_FACTORY = (
    PROJECT_ROOT
    / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleSessionFactory.java"
)
RUNTIME_BUNDLE_SESSION_FACTORY_TEST = (
    PROJECT_ROOT
    / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleSessionFactoryTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE183_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        FACTORY,
        RUNTIME_BUNDLE_SESSION_FACTORY,
        RUNTIME_BUNDLE_SESSION_FACTORY_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase183-interaction-session-factory-runtime-bundle-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase183-interaction-session-factory-runtime-bundle-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase183_plan_text = _read(PHASE183_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_text = _read(FACTORY)
    runtime_bundle_session_factory_text = _read(RUNTIME_BUNDLE_SESSION_FACTORY)
    runtime_bundle_session_factory_test_text = _read(RUNTIME_BUNDLE_SESSION_FACTORY_TEST)

    if "## Phase 183 Slice Status" not in phase183_plan_text:
        errors.append("phase183_plan_missing_slice_status")
    if "`183.1` complete." not in phase183_plan_text:
        errors.append("phase183_plan_missing_183_1_complete")
    if "`183.2` complete." not in phase183_plan_text:
        errors.append("phase183_plan_missing_183_2_complete")
    if "`183.3` complete." not in phase183_plan_text:
        errors.append("phase183_plan_missing_183_3_complete")

    if (
        "## Phase 183 (Interaction Session Factory Runtime Bundle Session Factory Extraction)"
        not in migration_plan_text
    ):
        errors.append("migration_plan_missing_phase183_section")

    if "PHASE 183 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase183_started")
    if "PHASE 183 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase183_complete")

    required_tasks = [
        "- [x] Define Phase 183 interaction-session factory runtime-bundle-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryRuntimeBundleSessionFactory` ownership for interaction-session runtime-bundle/runtime-operations session routing seams.",
        "- [x] Run Phase 183 verification + guard pack and mark `PHASE 183 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase183_line:{task_line}")

    required_factory_strings = [
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_factory_strings:
        if required_string not in factory_text:
            errors.append(f"factory_missing_string:{required_string}")

    required_runtime_bundle_session_factory_strings = [
        "final class InteractionSessionFactoryRuntimeBundleSessionFactory",
        "static InteractionSession createFromRuntimeBundle(",
        "InteractionSessionRuntimeSessionFactory.createFromRuntimeBundle(",
        "static InteractionSession createFromRuntimeOperations(",
        "InteractionSessionRuntimeSessionFactory.createFromRuntimeOperations(",
    ]
    for required_string in required_runtime_bundle_session_factory_strings:
        if required_string not in runtime_bundle_session_factory_text:
            errors.append(f"runtime_bundle_session_factory_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryRuntimeBundleSessionFactoryTest",
        "exposesRuntimeBundleSessionFactoryEntryPoints",
    ]
    for required_string in required_test_strings:
        if required_string not in runtime_bundle_session_factory_test_text:
            errors.append(f"runtime_bundle_session_factory_test_missing_string:{required_string}")

    if errors:
        print("[phase183-interaction-session-factory-runtime-bundle-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase183-interaction-session-factory-runtime-bundle-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase183-interaction-session-factory-runtime-bundle-session-factory-extraction] OK: "
        "interaction session factory runtime-bundle-session-factory extraction Phase 183 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
