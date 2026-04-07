from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE17_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE17_MOTOR_TERMINAL_DECOMPOSITION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
MOTOR_PROGRAM_TERMINAL_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/MotorProgramTerminalService.java"
)
MOTOR_PROGRAM_TERMINAL_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/MotorProgramTerminalServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE17_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        MOTOR_PROGRAM_TERMINAL_SERVICE,
        MOTOR_PROGRAM_TERMINAL_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase17-motor-terminal-decomposition] FAILED")
        for error in errors:
            print(f"[phase17-motor-terminal-decomposition] ERROR {error}")
        return 1

    phase17_plan_text = _read(PHASE17_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)
    motor_terminal_text = _read(MOTOR_PROGRAM_TERMINAL_SERVICE)
    motor_terminal_test_text = _read(MOTOR_PROGRAM_TERMINAL_SERVICE_TEST)

    if "## Phase 17 Slice Status" not in phase17_plan_text:
        errors.append("phase17_plan_missing_slice_status")
    if "`17.1` complete." not in phase17_plan_text:
        errors.append("phase17_plan_missing_17_1_complete")
    if "`17.2` complete." not in phase17_plan_text:
        errors.append("phase17_plan_missing_17_2_complete")
    if "`17.3` complete." not in phase17_plan_text:
        errors.append("phase17_plan_missing_17_3_complete")

    if "## Phase 17 (Motor Terminal Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase17_section")

    if "PHASE 17 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase17_started")
    if "PHASE 17 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase17_complete")

    required_tasks = [
        "- [x] Define Phase 17 motor terminal decomposition scope and completion evidence gates.",
        "- [x] Extract motor terminal lifecycle ownership from `CommandExecutor` into focused runtime service.",
        "- [x] Run Phase 17 verification + guard pack and mark `PHASE 17 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase17_line:{task_line}")

    if "private final MotorProgramTerminalService motorProgramTerminalService;" not in command_executor_text:
        errors.append("command_executor_missing_motor_program_terminal_service_field")
    if "motorProgramTerminalService = new MotorProgramTerminalService(" not in command_executor_text:
        errors.append("command_executor_missing_motor_program_terminal_service_construction")
    if "motorProgramTerminalService.validateMotorProgramMenu(" not in command_executor_text:
        errors.append("command_executor_missing_validate_delegate")
    if "motorProgramTerminalService.completeMotorProgram(" not in command_executor_text:
        errors.append("command_executor_missing_complete_delegate")
    if "motorProgramTerminalService.cancelMotorProgram(" not in command_executor_text:
        errors.append("command_executor_missing_cancel_delegate")
    if "motorProgramTerminalService.failMotorProgram(" not in command_executor_text:
        errors.append("command_executor_missing_fail_delegate")
    if "motorProgramTerminalService::releaseIdleMotorOwnershipForRuntimeTeardown" not in command_executor_text:
        errors.append("command_executor_missing_runtime_teardown_delegate")
    if "motorProgramTerminalService::releaseIdleMotorOwnershipAfterSuppression" not in command_executor_text:
        errors.append("command_executor_missing_suppression_delegate")
    if "private void releaseIdleMotorOwnershipForTerminalProgram(" in command_executor_text:
        errors.append("command_executor_still_owns_terminal_idle_release_helper")
    if "private void releaseIdleMotorOwnershipAfterSuppression(" in command_executor_text:
        errors.append("command_executor_still_owns_suppression_idle_release_helper")
    if "private void releaseIdleMotorOwnershipForRuntimeTeardown(" in command_executor_text:
        errors.append("command_executor_still_owns_teardown_idle_release_helper")

    if "final class MotorProgramTerminalService" not in motor_terminal_text:
        errors.append("motor_program_terminal_service_missing_class")
    if "void releaseIdleMotorOwnershipAfterSuppression()" not in motor_terminal_text:
        errors.append("motor_program_terminal_service_missing_suppression_release")
    if "void releaseIdleMotorOwnershipForRuntimeTeardown()" not in motor_terminal_text:
        errors.append("motor_program_terminal_service_missing_runtime_teardown_release")
    if "completeMotorProgram(" not in motor_terminal_text:
        errors.append("motor_program_terminal_service_missing_complete")
    if "cancelMotorProgram(" not in motor_terminal_text:
        errors.append("motor_program_terminal_service_missing_cancel")
    if "failMotorProgram(" not in motor_terminal_text:
        errors.append("motor_program_terminal_service_missing_fail")

    if "completeCancelFailDelegateLifecycleAndApplyIdleOwnerRelease" not in motor_terminal_test_text:
        errors.append("motor_program_terminal_service_test_missing_terminal_lifecycle_case")
    if "validateMotorProgramMenuDelegatesToLifecycleEngine" not in motor_terminal_test_text:
        errors.append("motor_program_terminal_service_test_missing_menu_validation_case")

    if errors:
        print("[phase17-motor-terminal-decomposition] FAILED")
        for error in errors:
            print(f"[phase17-motor-terminal-decomposition] ERROR {error}")
        return 1

    print(
        "[phase17-motor-terminal-decomposition] OK: motor terminal decomposition Phase 17 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
