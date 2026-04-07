from __future__ import annotations

from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
PHASE95_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE95_INTERACTION_SESSION_WIRING_CONSOLIDATION_L_PLAN.md"
MIGRATION_PLAN = PROJECT_ROOT / "docs/NATIVE_CLIENT_MIGRATION_PLAN.md"
PHASE_STATUS = PROJECT_ROOT / "docs/NATIVE_CLIENT_PHASE_STATUS.md"
TASKS = PROJECT_ROOT / "TASKS.md"
EXECUTOR_SERVICE_WIRING = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/executor/ExecutorServiceWiring.java"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    required_paths = [PHASE95_PLAN, MIGRATION_PLAN, PHASE_STATUS, TASKS, EXECUTOR_SERVICE_WIRING]
    for path in required_paths:
        if not path.exists():
            errors.append(f"missing_required_path:{path}")

    if errors:
        print("[phase95-interaction-session-wiring-consolidation-l] FAILED")
        for error in errors:
            print(f"[phase95-interaction-session-wiring-consolidation-l] ERROR {error}")
        return 1

    phase95_plan_text = _read(PHASE95_PLAN)
    migration_plan_text = _read(MIGRATION_PLAN)
    phase_status_text = _read(PHASE_STATUS)
    tasks_text = _read(TASKS)
    wiring_text = _read(EXECUTOR_SERVICE_WIRING)

    if "## Phase 95 Slice Status" not in phase95_plan_text:
        errors.append("phase95_plan_missing_slice_status")
    if "`95.1` complete." not in phase95_plan_text:
        errors.append("phase95_plan_missing_95_1_complete")
    if "`95.2` complete." not in phase95_plan_text:
        errors.append("phase95_plan_missing_95_2_complete")
    if "`95.3` complete." not in phase95_plan_text:
        errors.append("phase95_plan_missing_95_3_complete")

    if "## Phase 95 (Interaction Session Wiring Consolidation L)" not in migration_plan_text:
        errors.append("migration_plan_missing_phase95_section")

    if "PHASE 95 STARTED" not in phase_status_text:
        errors.append("phase_status_missing_phase95_started")
    if "PHASE 95 COMPLETE" not in phase_status_text:
        errors.append("phase_status_missing_phase95_complete")

    required_tasks = [
        "- [x] Define Phase 95 interaction session wiring consolidation L scope and completion evidence gates.",
        "- [x] Consolidate executor/session wiring interaction-session seam through focused `InteractionSessionFactory` ownership.",
        "- [x] Run Phase 95 verification + guard pack and mark `PHASE 95 COMPLETE`.",
    ]
    for task_line in required_tasks:
        if task_line not in tasks_text:
            errors.append(f"tasks_missing_phase95_line:{task_line}")

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
        print("[phase95-interaction-session-wiring-consolidation-l] FAILED")
        for error in errors:
            print(f"[phase95-interaction-session-wiring-consolidation-l] ERROR {error}")
        return 1

    print("[phase95-interaction-session-wiring-consolidation-l] OK: interaction session wiring consolidation L Phase 95 baseline is enforced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
