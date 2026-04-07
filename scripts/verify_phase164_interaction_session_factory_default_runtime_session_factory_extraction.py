from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE164_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE164_INTERACTION_SESSION_FACTORY_DEFAULT_RUNTIME_SESSION_FACTORY_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
DEFAULT_RUNTIME_SESSION_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeSessionFactory.java"
)
DEFAULT_RUNTIME_SESSION_FACTORY_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryDefaultRuntimeSessionFactoryTest.java"
)
DEFAULT_ENTRY_FACTORY = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryDefaultEntryFactory.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE164_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        DEFAULT_RUNTIME_SESSION_FACTORY,
        DEFAULT_RUNTIME_SESSION_FACTORY_TEST,
        DEFAULT_ENTRY_FACTORY,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase164-interaction-session-factory-default-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase164-interaction-session-factory-default-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    phase164_plan_text = _read(PHASE164_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    default_runtime_session_factory_text = _read(DEFAULT_RUNTIME_SESSION_FACTORY)
    default_runtime_session_factory_test_text = _read(DEFAULT_RUNTIME_SESSION_FACTORY_TEST)
    default_entry_factory_text = _read(DEFAULT_ENTRY_FACTORY)

    if "## Phase 164 Slice Status" not in phase164_plan_text:
        errors.append("phase164_plan_missing_slice_status")
    if "`164.1` complete." not in phase164_plan_text:
        errors.append("phase164_plan_missing_164_1_complete")
    if "`164.2` complete." not in phase164_plan_text:
        errors.append("phase164_plan_missing_164_2_complete")
    if "`164.3` complete." not in phase164_plan_text:
        errors.append("phase164_plan_missing_164_3_complete")

    if "## Phase 164 (Interaction Session Factory Default Runtime Session Factory Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase164_section")

    if "PHASE 164 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase164_started")
    if "PHASE 164 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase164_complete")

    required_tasks = [
        "- [x] Define Phase 164 interaction-session factory default-runtime-session-factory extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryDefaultRuntimeSessionFactory` ownership for interaction-session default runtime session creation seams.",
        "- [x] Run Phase 164 verification + guard pack and mark `PHASE 164 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase164_line:{task_line}")

    required_default_runtime_session_factory_strings = [
        "final class InteractionSessionFactoryDefaultRuntimeSessionFactory",
        "static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(",
        "InteractionSessionRuntimeSessionFactory.createFromRuntimeBundle(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(",
    ]
    for required_string in required_default_runtime_session_factory_strings:
        if required_string not in default_runtime_session_factory_text:
            errors.append(f"default_runtime_session_factory_missing_string:{required_string}")

    required_default_runtime_session_factory_test_strings = [
        "class InteractionSessionFactoryDefaultRuntimeSessionFactoryTest",
        "exposesDefaultRuntimeSessionFactoryEntryPoints",
    ]
    for required_string in required_default_runtime_session_factory_test_strings:
        if required_string not in default_runtime_session_factory_test_text:
            errors.append(f"default_runtime_session_factory_test_missing_string:{required_string}")

    if (
        "InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs("
        not in default_entry_factory_text
    ):
        errors.append("default_entry_factory_missing_default_runtime_session_factory_delegate")

    if errors:
        print("[phase164-interaction-session-factory-default-runtime-session-factory-extraction] FAILED")
        for error in errors:
            print(
                "[phase164-interaction-session-factory-default-runtime-session-factory-extraction] "
                f"ERROR {error}"
            )
        return 1

    print(
        "[phase164-interaction-session-factory-default-runtime-session-factory-extraction] OK: "
        "interaction session factory default-runtime-session-factory extraction Phase 164 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
