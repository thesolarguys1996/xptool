from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
CUTOVER_REPORT = PROJECT_ROOT / "runtime" / "native-cutover" / "phase7-cutover-report.json"
SOAK_DIR = PROJECT_ROOT / "runtime" / "native-soak"
SOAK_ITERATIONS_DIR = SOAK_DIR / "iterations"
SOAK_REPORT = SOAK_DIR / "soak-report.json"


def _utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _run_cutover_gate() -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, "scripts/verify_native_cutover.py"],
        cwd=PROJECT_ROOT,
        text=True,
        capture_output=True,
    )


def _parse_report(path: Path) -> dict[str, object] | None:
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser(description="Run native cutover gate repeatedly for soak signoff.")
    parser.add_argument("--iterations", type=int, default=6, help="Number of soak iterations to run.")
    parser.add_argument("--pause-ms", type=int, default=250, help="Delay between iterations in milliseconds.")
    args = parser.parse_args()

    if args.iterations <= 0:
        raise SystemExit("iterations must be > 0")
    if args.pause_ms < 0:
        raise SystemExit("pause-ms must be >= 0")

    SOAK_ITERATIONS_DIR.mkdir(parents=True, exist_ok=True)

    run_started_at = _utc_now()
    iteration_results: list[dict[str, object]] = []
    check_failure_counts: dict[str, int] = {}
    failed_iterations = 0

    for iteration in range(1, args.iterations + 1):
        started = time.monotonic()
        proc = _run_cutover_gate()
        duration_ms = int((time.monotonic() - started) * 1000.0)

        report = _parse_report(CUTOVER_REPORT)
        report_copy_path = SOAK_ITERATIONS_DIR / f"iteration-{iteration:03d}-cutover-report.json"
        if CUTOVER_REPORT.exists():
            shutil.copyfile(CUTOVER_REPORT, report_copy_path)

        check_statuses: dict[str, bool] = {}
        failed_checks: list[str] = []
        if isinstance(report, dict):
            checks = report.get("checks", [])
            if isinstance(checks, list):
                for check in checks:
                    if not isinstance(check, dict):
                        continue
                    name = check.get("name")
                    passed = bool(check.get("passed"))
                    if isinstance(name, str):
                        check_statuses[name] = passed
                        if not passed:
                            failed_checks.append(name)
                            check_failure_counts[name] = check_failure_counts.get(name, 0) + 1

        passed = proc.returncode == 0 and isinstance(report, dict) and bool(report.get("passed", False))
        if not passed:
            failed_iterations += 1

        iteration_row = {
            "iteration": iteration,
            "startedAtUtc": _utc_now(),
            "durationMs": duration_ms,
            "returnCode": proc.returncode,
            "passed": passed,
            "failedChecks": failed_checks,
            "cutoverReportPath": str(report_copy_path if report_copy_path.exists() else CUTOVER_REPORT),
            "stdoutTail": (proc.stdout or "").strip().splitlines()[-8:],
            "stderrTail": (proc.stderr or "").strip().splitlines()[-8:],
            "checkStatuses": check_statuses,
        }
        iteration_results.append(iteration_row)

        summary = "PASSED" if passed else "FAILED"
        print(f"[native-soak] iteration={iteration}/{args.iterations} status={summary} durationMs={duration_ms}")
        if failed_checks:
            print(f"[native-soak] iteration={iteration} failed_checks={','.join(failed_checks)}")

        if iteration < args.iterations and args.pause_ms > 0:
            time.sleep(args.pause_ms / 1000.0)

    run_finished_at = _utc_now()
    all_passed = failed_iterations == 0
    report = {
        "generatedAtUtc": run_finished_at,
        "runStartedAtUtc": run_started_at,
        "runFinishedAtUtc": run_finished_at,
        "projectRoot": str(PROJECT_ROOT),
        "iterationsRequested": args.iterations,
        "pauseMs": args.pause_ms,
        "failedIterations": failed_iterations,
        "passedIterations": args.iterations - failed_iterations,
        "allPassed": all_passed,
        "checkFailureCounts": check_failure_counts,
        "iterations": iteration_results,
    }
    SOAK_REPORT.write_text(json.dumps(report, indent=2), encoding="utf-8")

    if all_passed:
        print(f"[native-soak] OK report={SOAK_REPORT}")
        return 0

    print(f"[native-soak] FAILED failedIterations={failed_iterations} report={SOAK_REPORT}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
