from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE111_PLAN = (
    PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE115_INTERACTION_SESSION_FACTORY_INPUTS_EXTRACTION_PLAN.md"
)
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
FACTORY_INPUTS = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryInputs.java"
FACTORY_INPUTS_TEST = PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionFactoryInputsTest.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE111_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, FACTORY_INPUTS, FACTORY_INPUTS_TEST]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase115-interaction-session-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase115-interaction-session-factory-inputs-extraction] ERROR {error}")
        return 1

    phase115_plan_text = _read(PHASE111_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    factory_inputs_text = _read(FACTORY_INPUTS)
    factory_inputs_test_text = _read(FACTORY_INPUTS_TEST)

    if "## Phase 115 Slice Status" not in phase115_plan_text:
        errors.append("phase115_plan_missing_slice_status")
    if "`115.1` complete." not in phase115_plan_text:
        errors.append("phase115_plan_missing_111_1_complete")
    if "`115.2` complete." not in phase115_plan_text:
        errors.append("phase115_plan_missing_111_2_complete")
    if "`115.3` complete." not in phase115_plan_text:
        errors.append("phase115_plan_missing_111_3_complete")

    if "## Phase 115 (Interaction Session Factory Inputs Extraction)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase115_section")

    if "PHASE 115 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase115_started")
    if "PHASE 115 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase115_complete")

    required_tasks = [
        "- [x] Define Phase 115 interaction-session factory inputs extraction scope and completion evidence gates.",
        "- [x] Extract focused `InteractionSessionFactoryInputs` ownership for interaction-session factory typed inputs.",
        "- [x] Run Phase 115 verification + guard pack and mark `PHASE 115 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase115_line:{task_line}")

    required_inputs_strings = [
        "final class InteractionSessionFactoryInputs",
        "final CommandExecutor executor;",
        "final SessionManager sessionManager;",
        "final SessionCommandFacade commandFacade;",
        "static InteractionSessionFactoryInputs fromServices(",
        "InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs(String sessionInteractionKey)",
    ]
    for required_string in required_inputs_strings:
        if required_string not in factory_inputs_text:
            errors.append(f"factory_inputs_missing_string:{required_string}")

    required_test_strings = [
        "class InteractionSessionFactoryInputsTest",
        "fromServicesRetainsProvidedReferences",
        "createAssemblyFactoryInputsBuildsDefaultSessionInputs",
    ]
    for required_string in required_test_strings:
        if required_string not in factory_inputs_test_text:
            errors.append(f"factory_inputs_test_missing_string:{required_string}")

    if errors:
        print("[phase115-interaction-session-factory-inputs-extraction] FAILED")
        for error in errors:
            print(f"[phase115-interaction-session-factory-inputs-extraction] ERROR {error}")
        return 1

    print(
        "[phase115-interaction-session-factory-inputs-extraction] OK: interaction session factory inputs extraction "
        "Phase 115 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
