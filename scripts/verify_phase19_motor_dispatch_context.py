from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE19_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE19_MOTOR_DISPATCH_CONTEXT_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
COMMAND_EXECUTOR = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java"
MOTOR_DISPATCH_CONTEXT_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/MotorDispatchContextService.java"
)
MOTOR_DISPATCH_CONTEXT_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/MotorDispatchContextServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE19_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        COMMAND_EXECUTOR,
        MOTOR_DISPATCH_CONTEXT_SERVICE,
        MOTOR_DISPATCH_CONTEXT_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase19-motor-dispatch-context] FAILED")
        for error in errors:
            print(f"[phase19-motor-dispatch-context] ERROR {error}")
        return 1

    phase19_plan_text = _read(PHASE19_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    command_executor_text = _read(COMMAND_EXECUTOR)
    context_service_text = _read(MOTOR_DISPATCH_CONTEXT_SERVICE)
    context_service_test_text = _read(MOTOR_DISPATCH_CONTEXT_SERVICE_TEST)

    if "## Phase 19 Slice Status" not in phase19_plan_text:
        errors.append("phase19_plan_missing_slice_status")
    if "`19.1` complete." not in phase19_plan_text:
        errors.append("phase19_plan_missing_19_1_complete")
    if "`19.2` complete." not in phase19_plan_text:
        errors.append("phase19_plan_missing_19_2_complete")
    if "`19.3` complete." not in phase19_plan_text:
        errors.append("phase19_plan_missing_19_3_complete")

    if "## Phase 19 (Motor Dispatch Context Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase19_section")

    if "PHASE 19 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase19_started")
    if "PHASE 19 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase19_complete")

    required_tasks = [
        "- [x] Define Phase 19 motor dispatch context decomposition scope and completion evidence gates.",
        "- [x] Extract motor owner/click-type context ownership from `CommandExecutor` into focused runtime service.",
        "- [x] Run Phase 19 verification + guard pack and mark `PHASE 19 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase19_line:{task_line}")

    if "private final MotorDispatchContextService motorDispatchContextService;" not in command_executor_text:
        errors.append("command_executor_missing_context_service_field")
    if "private String activeMotorOwnerContext = \"\";" in command_executor_text:
        errors.append("command_executor_still_owns_active_motor_owner_field")
    if "private String activeClickTypeContext = ExecutorMotorProfileCatalog.CLICK_TYPE_NONE;" in command_executor_text:
        errors.append("command_executor_still_owns_active_click_type_field")
    if "return motorDispatchContextService.pushMotorOwnerContext(owner);" not in command_executor_text:
        errors.append("command_executor_missing_push_owner_delegate")
    if "motorDispatchContextService.popMotorOwnerContext(previous);" not in command_executor_text:
        errors.append("command_executor_missing_pop_owner_delegate")
    if "return motorDispatchContextService.pushClickTypeContext(clickType);" not in command_executor_text:
        errors.append("command_executor_missing_push_click_type_delegate")
    if "motorDispatchContextService.popClickTypeContext(previous);" not in command_executor_text:
        errors.append("command_executor_missing_pop_click_type_delegate")
    if "return motorDispatchContextService.activeMotorOwnerContext();" not in command_executor_text:
        errors.append("command_executor_missing_active_owner_accessor")
    if "return motorDispatchContextService.activeClickTypeContext();" not in command_executor_text:
        errors.append("command_executor_missing_active_click_type_accessor")

    if "final class MotorDispatchContextService" not in context_service_text:
        errors.append("context_service_missing_class")
    if "String pushMotorOwnerContext(" not in context_service_text:
        errors.append("context_service_missing_push_owner")
    if "void popMotorOwnerContext(" not in context_service_text:
        errors.append("context_service_missing_pop_owner")
    if "String pushClickTypeContext(" not in context_service_text:
        errors.append("context_service_missing_push_click_type")
    if "void popClickTypeContext(" not in context_service_text:
        errors.append("context_service_missing_pop_click_type")
    if "String activeMotorOwnerContext()" not in context_service_text:
        errors.append("context_service_missing_active_owner_getter")
    if "String activeClickTypeContext()" not in context_service_text:
        errors.append("context_service_missing_active_click_getter")

    if "pushAndPopMotorOwnerContextNormalizesValues" not in context_service_test_text:
        errors.append("context_service_test_missing_owner_case")
    if "pushAndPopClickTypeContextTracksCurrentClickType" not in context_service_test_text:
        errors.append("context_service_test_missing_click_type_case")

    if errors:
        print("[phase19-motor-dispatch-context] FAILED")
        for error in errors:
            print(f"[phase19-motor-dispatch-context] ERROR {error}")
        return 1

    print("[phase19-motor-dispatch-context] OK: motor dispatch context Phase 19 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
