from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE99_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE99_INTERACTION_SESSION_WIRING_CONSOLIDATION_M_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
INTERACTION_SESSION_FACTORY = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
EXECUTOR_SERVICE_WIRING = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/ExecutorServiceWiring.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE99_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, INTERACTION_SESSION_FACTORY, EXECUTOR_SERVICE_WIRING]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase99-interaction-session-wiring-consolidation-m] FAILED")
        for error in errors:
            print(f"[phase99-interaction-session-wiring-consolidation-m] ERROR {error}")
        return 1

    phase99_plan_text = _read(PHASE99_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    interaction_session_factory_text = _read(INTERACTION_SESSION_FACTORY)
    wiring_text = _read(EXECUTOR_SERVICE_WIRING)

    if "## Phase 99 Slice Status" not in phase99_plan_text:
        errors.append("phase99_plan_missing_slice_status")
    if "`99.1` complete." not in phase99_plan_text:
        errors.append("phase99_plan_missing_99_1_complete")
    if "`99.2` complete." not in phase99_plan_text:
        errors.append("phase99_plan_missing_99_2_complete")
    if "`99.3` complete." not in phase99_plan_text:
        errors.append("phase99_plan_missing_99_3_complete")

    if "## Phase 99 (Interaction Session Wiring Consolidation M)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase99_section")

    if "PHASE 99 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase99_started")
    if "PHASE 99 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase99_complete")

    required_tasks = [
        "- [x] Define Phase 99 interaction-session wiring consolidation M scope and completion evidence gates.",
        "- [x] Consolidate interaction-session runtime-bundle wiring seam through focused `InteractionSessionRuntimeOperationsFactory` ownership.",
        "- [x] Run Phase 99 verification + guard pack and mark `PHASE 99 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase99_line:{task_line}")

    required_factory_strings = [
        "static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle)",
        "InteractionSessionRuntimeOperationsFactory.createFromRuntimeBundle(runtimeBundle)",
        "static InteractionSession createFromRuntimeOperations(InteractionSessionRuntimeOperations runtimeOperations)",
        "return new InteractionSession(runtimeOperations);",
    ]
    for required_string in required_factory_strings:
        if required_string not in interaction_session_factory_text:
            errors.append(f"interaction_session_factory_missing_string:{required_string}")

    if "return new InteractionSession(runtimeBundle);" in interaction_session_factory_text:
        errors.append("interaction_session_factory_still_contains_direct_runtime_bundle_construction")

    required_wiring_strings = [
        "import com.xptool.sessions.InteractionSessionFactory;",
        "InteractionSessionFactory.create(",
    ]
    for required_string in required_wiring_strings:
        if required_string not in wiring_text:
            errors.append(f"executor_service_wiring_missing_string:{required_string}")

    if "new InteractionSession(executor, sessionManager, sessionCommandFacade);" in wiring_text:
        errors.append("executor_service_wiring_still_contains_direct_interaction_session_construction")

    if errors:
        print("[phase99-interaction-session-wiring-consolidation-m] FAILED")
        for error in errors:
            print(f"[phase99-interaction-session-wiring-consolidation-m] ERROR {error}")
        return 1

    print("[phase99-interaction-session-wiring-consolidation-m] OK: interaction session wiring consolidation M Phase 99 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
