from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE18_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE18_MOTOR_DISPATCH_ADMISSION_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
MOTOR_DISPATCH_ADMISSION_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/MotorDispatchAdmissionService.java"
)
MOTOR_DISPATCH_ADMISSION_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchAdmissionServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE18_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        MOTOR_DISPATCH_ADMISSION_SERVICE,
        MOTOR_DISPATCH_ADMISSION_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase18-motor-dispatch-admission] FAILED")
        for error in errors:
            print(f"[phase18-motor-dispatch-admission] ERROR {error}")
        return 1

    phase18_plan_text = _read(PHASE18_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)
    service_text = _read(MOTOR_DISPATCH_ADMISSION_SERVICE)
    service_test_text = _read(MOTOR_DISPATCH_ADMISSION_SERVICE_TEST)

    if "## Phase 18 Slice Status" not in phase18_plan_text:
        errors.append("phase18_plan_missing_slice_status")
    if "`18.1` complete." not in phase18_plan_text:
        errors.append("phase18_plan_missing_18_1_complete")
    if "`18.2` complete." not in phase18_plan_text:
        errors.append("phase18_plan_missing_18_2_complete")
    if "`18.3` complete." not in phase18_plan_text:
        errors.append("phase18_plan_missing_18_3_complete")

    if "## Phase 18 (Motor Dispatch Admission Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase18_section")

    if "PHASE 18 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase18_started")
    if "PHASE 18 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase18_complete")

    required_tasks = [
        "- [x] Define Phase 18 motor dispatch admission decomposition scope and completion evidence gates.",
        "- [x] Extract motor dispatch admission/cooldown ownership from `CommandExecutor` into focused runtime service.",
        "- [x] Run Phase 18 verification + guard pack and mark `PHASE 18 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase18_line:{task_line}")

    if "private final MotorDispatchAdmissionService motorDispatchAdmissionService;" not in command_executor_text:
        errors.append("command_executor_missing_motor_dispatch_admission_service_field")
    if "private final MotorActionGate motorActionGate" in command_executor_text:
        errors.append("command_executor_still_owns_motor_action_gate_field")
    if "return motorDispatchAdmissionService.scheduleMotorGesture(target, type, profile);" not in command_executor_text:
        errors.append("command_executor_missing_schedule_delegate")
    if (
        "return motorDispatchAdmissionService.canPerformMotorActionNow(activeMotorOwnerContext);" not in command_executor_text
        and "return motorDispatchAdmissionService.canPerformMotorActionNow(activeMotorOwnerContext());" not in command_executor_text
    ):
        errors.append("command_executor_missing_can_perform_delegate")
    if "return motorDispatchAdmissionService.isMotorActionReadyNow();" not in command_executor_text:
        errors.append("command_executor_missing_ready_delegate")
    if "motorDispatchAdmissionService.reserveMotorCooldown(delayMs);" not in command_executor_text:
        errors.append("command_executor_missing_reserve_cooldown_delegate")
    if "motorDispatchAdmissionService.noteMotorAction();" not in command_executor_text:
        errors.append("command_executor_missing_note_action_delegate")
    if "return motorDispatchAdmissionService.actionSerial();" not in command_executor_text:
        errors.append("command_executor_missing_action_serial_delegate")
    if "return motorDispatchAdmissionService.tryConsumeMouseMutationBudget();" not in command_executor_text:
        errors.append("command_executor_missing_mutation_budget_delegate")
    if "motorDispatchAdmissionService.resetMouseMutationBudget();" not in command_executor_text:
        errors.append("command_executor_missing_mutation_budget_reset_delegate")

    if "final class MotorDispatchAdmissionService" not in service_text:
        errors.append("service_missing_class_declaration")
    if "boolean canPerformMotorActionNow(" not in service_text:
        errors.append("service_missing_can_perform")
    if "MotorHandle scheduleMotorGesture(" not in service_text:
        errors.append("service_missing_schedule_method")
    if "void reserveMotorCooldown(" not in service_text:
        errors.append("service_missing_reserve_cooldown")
    if "boolean tryConsumeMouseMutationBudget(" not in service_text:
        errors.append("service_missing_mutation_budget_method")

    if "canPerformMotorActionNowRequiresOwnerLeaseAndCooldownReadiness" not in service_test_text:
        errors.append("service_test_missing_cooldown_case")
    if "scheduleMotorGestureReusesMatchingProgramAndRejectsBusyDifferentProgram" not in service_test_text:
        errors.append("service_test_missing_busy_case")
    if "noteActionAndMutationBudgetAreTrackedByAdmissionService" not in service_test_text:
        errors.append("service_test_missing_serial_budget_case")

    if errors:
        print("[phase18-motor-dispatch-admission] FAILED")
        for error in errors:
            print(f"[phase18-motor-dispatch-admission] ERROR {error}")
        return 1

    print(
        "[phase18-motor-dispatch-admission] OK: motor dispatch admission Phase 18 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
