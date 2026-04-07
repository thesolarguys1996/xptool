from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
MIGRATION_DOCS_ROOT = PROJECT_ROOT / "docs" / "migration"
ARCHIVE_DOCS_ROOT = MIGRATION_DOCS_ROOT / "archive"
ARCHIVE_SCRIPT_ROOT = PROJECT_ROOT / "scripts" / "migration" / "archive" / "phase_verifiers"
PHASE_STATUS_PATH = PROJECT_ROOT / "docs" / "NATIVE_CLIENT_PHASE_STATUS.md"


@dataclass(frozen=True)
class CheckResult:
    name: str
    passed: bool
    details: str


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _phase_archive_docs() -> dict[int, list[Path]]:
    docs_by_phase: dict[int, list[Path]] = {}
    for path in sorted(ARCHIVE_DOCS_ROOT.glob("NATIVE_CLIENT_PHASE*_PLAN.md")):
        match = re.match(r"NATIVE_CLIENT_PHASE(\d+).*_PLAN\.md$", path.name)
        if not match:
            continue
        phase = int(match.group(1))
        docs_by_phase.setdefault(phase, []).append(path)
    return docs_by_phase


def _phase_markers(phase: int) -> tuple[bool, bool]:
    text = _read(PHASE_STATUS_PATH)
    started = f"PHASE {phase} STARTED" in text
    complete = f"PHASE {phase} COMPLETE" in text
    return started, complete


def _check_docs_hierarchy() -> CheckResult:
    required = [
        MIGRATION_DOCS_ROOT / "overview.md",
        MIGRATION_DOCS_ROOT / "current-phase.md",
        MIGRATION_DOCS_ROOT / "checklists.md",
        ARCHIVE_DOCS_ROOT / "README.md",
    ]
    missing = [str(path) for path in required if not path.exists()]
    if missing:
        return CheckResult("docs_hierarchy", False, f"missing={missing}")
    return CheckResult("docs_hierarchy", True, "migration doc hierarchy present")


def _check_archive_integrity() -> CheckResult:
    phase_doc_count = len(list(ARCHIVE_DOCS_ROOT.glob("NATIVE_CLIENT_PHASE*_PLAN.md")))
    phase_script_count = len(list(ARCHIVE_SCRIPT_ROOT.glob("verify_phase*.py")))
    if phase_doc_count < 50 or phase_script_count < 50:
        return CheckResult(
            "archive_integrity",
            False,
            f"phase_doc_count={phase_doc_count} phase_script_count={phase_script_count}",
        )
    return CheckResult(
        "archive_integrity",
        True,
        f"phase_doc_count={phase_doc_count} phase_script_count={phase_script_count}",
    )


def _check_runtime_bundle_factory() -> CheckResult:
    factory = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactoryRuntimeBundleFactory.java"
    session_factory = PROJECT_ROOT / "runelite-plugin/src/main/java/com/xptool/sessions/InteractionSessionFactory.java"
    if not factory.exists() or not session_factory.exists():
        return CheckResult(
            "runtime_bundle_factory",
            False,
            f"missing={[str(p) for p in [factory, session_factory] if not p.exists()]}",
        )

    factory_text = _read(factory)
    session_factory_text = _read(session_factory)
    required_factory_markers = [
        "final class InteractionSessionFactoryRuntimeBundleFactory",
        "createDefaultAssemblyFactoryInputs(",
        "createRuntimeBundleFromFactoryInputs(",
        "createRuntimeBundleFromAssemblyFactoryInputs(",
    ]
    required_session_factory_markers = [
        "createFromAssemblyFactoryInputs(",
        "InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromAssemblyFactoryInputs(",
    ]
    missing: list[str] = []
    for marker in required_factory_markers:
        if marker not in factory_text:
            missing.append(f"factory:{marker}")
    for marker in required_session_factory_markers:
        if marker not in session_factory_text:
            missing.append(f"session_factory:{marker}")

    if missing:
        return CheckResult("runtime_bundle_factory", False, f"missing_markers={missing}")
    return CheckResult("runtime_bundle_factory", True, "runtime bundle factory markers present")


def _check_phase(phase: int, require_complete: bool) -> list[CheckResult]:
    phase_docs = _phase_archive_docs().get(phase, [])
    doc_result = CheckResult(
        f"phase_{phase}_archive_doc",
        bool(phase_docs),
        f"docs={[str(path.relative_to(PROJECT_ROOT)) for path in phase_docs]}",
    )
    started, complete = _phase_markers(phase)
    phase_ok = complete if require_complete else (started or complete)
    marker_result = CheckResult(
        f"phase_{phase}_status_marker",
        phase_ok,
        f"started={started} complete={complete} require_complete={require_complete}",
    )

    results = [doc_result, marker_result]
    if phase in {119, 120}:
        results.append(_check_runtime_bundle_factory())
    return results


def _run_named_check(name: str) -> list[CheckResult]:
    normalized = name.strip().lower()
    if normalized == "docs_hierarchy":
        return [_check_docs_hierarchy()]
    if normalized == "archive_integrity":
        return [_check_archive_integrity()]
    if normalized == "runtime_bundle_factory":
        return [_check_runtime_bundle_factory()]
    phase_match = re.fullmatch(r"phase_(\d+)", normalized)
    if phase_match:
        return _check_phase(int(phase_match.group(1)), require_complete=False)
    raise ValueError(f"unknown_check:{name}")


def _list_checks() -> int:
    docs_by_phase = _phase_archive_docs()
    print("Named checks:")
    print("- docs_hierarchy")
    print("- archive_integrity")
    print("- runtime_bundle_factory")
    print("- phase_<number> (dynamic)")
    print("")
    if docs_by_phase:
        phases = ", ".join(str(p) for p in sorted(docs_by_phase))
        print(f"Archived phases: {phases}")
    else:
        print("Archived phases: (none found)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Consolidated migration verifier.")
    parser.add_argument("--phase", type=int, action="append", help="Run checks for phase number (repeatable).")
    parser.add_argument("--check", action="append", help="Run named check (repeatable).")
    parser.add_argument(
        "--require-complete",
        action="store_true",
        help="When used with --phase, require both STARTED and COMPLETE markers.",
    )
    parser.add_argument("--list", action="store_true", help="List available checks and phases.")
    args = parser.parse_args()

    if args.list:
        return _list_checks()

    if not args.phase and not args.check:
        parser.error("provide --phase and/or --check, or use --list")

    results: list[CheckResult] = []
    if args.phase:
        for phase in args.phase:
            results.extend(_check_phase(phase, require_complete=args.require_complete))
    if args.check:
        for check_name in args.check:
            try:
                results.extend(_run_named_check(check_name))
            except ValueError as exc:
                print(f"[migration-verify] ERROR {exc}")
                return 1

    any_failed = False
    for result in results:
        status = "OK" if result.passed else "FAILED"
        print(f"[migration-verify] {status} {result.name} :: {result.details}")
        if not result.passed:
            any_failed = True

    if any_failed:
        print("[migration-verify] FAILED")
        return 1
    print("[migration-verify] OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
