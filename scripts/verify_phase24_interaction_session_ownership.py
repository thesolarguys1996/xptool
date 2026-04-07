from __future__ import annotations

from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE24_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE24_INTERACTION_SESSION_OWNERSHIP_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSession.java"
INTERACTION_SESSION_OWNERSHIP_SERVICE = (
    PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionOwnershipService.java"
)
INTERACTION_SESSION_OWNERSHIP_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/sessions/InteractionSessionOwnershipServiceTest.java"
)
INTERACTION_ANCHOR_RESOLVER_SERVICE_TEST = (
    PROJECT_ROOT / "runelite-plugin/src/test/java/com/xptool/executor/InteractionAnchorResolverServiceTest.java"
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [
        PHASE24_PLAN,
        MIGRATION_PLAN,
        PHASE_STATUS,
        TASKS,
        INTERACTION_SESSION,
        INTERACTION_SESSION_OWNERSHIP_SERVICE,
        INTERACTION_SESSION_OWNERSHIP_SERVICE_TEST,
        INTERACTION_ANCHOR_RESOLVER_SERVICE_TEST,
    ]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase24-interaction-session-ownership] FAILED")
        for error in errors:
            print(f"[phase24-interaction-session-ownership] ERROR {error}")
        return 1

    phase24_plan_text = _read(PHASE24_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_text = _read(INTERACTION_SESSION)
    ownership_service_text = _read(INTERACTION_SESSION_OWNERSHIP_SERVICE)
    ownership_service_test_text = _read(INTERACTION_SESSION_OWNERSHIP_SERVICE_TEST)
    anchor_resolver_test_text = _read(INTERACTION_ANCHOR_RESOLVER_SERVICE_TEST)

    if "## Phase 24 Slice Status" not in phase24_plan_text:
        errors.append("phase24_plan_missing_slice_status")
    if "`24.1` complete." not in phase24_plan_text:
        errors.append("phase24_plan_missing_24_1_complete")
    if "`24.2` complete." not in phase24_plan_text:
        errors.append("phase24_plan_missing_24_2_complete")
    if "`24.3` complete." not in phase24_plan_text:
        errors.append("phase24_plan_missing_24_3_complete")

    if "## Phase 24 (Interaction Session Ownership Decomposition)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase24_section")

    if "PHASE 24 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase24_started")
    if "PHASE 24 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase24_complete")

    required_tasks = [
        "- [x] Define Phase 24 interaction session ownership decomposition scope and completion evidence gates.",
        "- [x] Extract interaction session registration/motor-ownership orchestration from `InteractionSession` into focused runtime service.",
        "- [x] Run Phase 24 verification + guard pack and mark `PHASE 24 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase24_line:{task_line}")

    if "private final InteractionSessionOwnershipService interactionSessionOwnershipService;" not in interaction_session_text:
        errors.append("interaction_session_missing_ownership_service_field")
    if "interactionSessionOwnershipService.onGameTick();" not in interaction_session_text:
        errors.append("interaction_session_missing_ownership_delegate_call")
    if "boolean shouldOwnForInteraction = executor.shouldOwnInteractionSession();" in interaction_session_text:
        errors.append("interaction_session_still_owns_tick_orchestration_branching")

    if "final class InteractionSessionOwnershipService" not in ownership_service_text:
        errors.append("interaction_session_ownership_service_missing_class")
    if "interface Host" not in ownership_service_text:
        errors.append("interaction_session_ownership_service_missing_host_interface")
    if "void onGameTick()" not in ownership_service_text:
        errors.append("interaction_session_ownership_service_missing_on_game_tick")
    if "host.ensureRegistered();" not in ownership_service_text:
        errors.append("interaction_session_ownership_service_missing_registration_ownership")
    if "host.releaseInteractionMotorOwnership();" not in ownership_service_text:
        errors.append("interaction_session_ownership_service_missing_motor_release_ownership")

    if "onGameTickClearsOwnershipWhenSessionShouldNotOwnAndNoWork" not in ownership_service_test_text:
        errors.append("interaction_session_ownership_test_missing_clear_ownership_case")
    if "onGameTickRegistersAndReleasesWhenNotReadyAndNoActiveProgram" not in ownership_service_test_text:
        errors.append("interaction_session_ownership_test_missing_not_ready_case")
    if "onGameTickRunsSettleWhenMotorOwnershipIsAcquired" not in ownership_service_test_text:
        errors.append("interaction_session_ownership_test_missing_settle_run_case")
    if "onGameTickClearsWhenBlockedByOtherSessionWithoutActiveProgram" not in ownership_service_test_text:
        errors.append("interaction_session_ownership_test_missing_other_session_case")

    if "import java.awt.Shape;" in anchor_resolver_test_text:
        errors.append("interaction_anchor_resolver_test_still_has_unused_shape_import")

    if errors:
        print("[phase24-interaction-session-ownership] FAILED")
        for error in errors:
            print(f"[phase24-interaction-session-ownership] ERROR {error}")
        return 1

    print(
        "[phase24-interaction-session-ownership] OK: interaction session ownership Phase 24 baseline is enforced."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
