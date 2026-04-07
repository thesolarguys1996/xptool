from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
MIGRATION_DOCS_ROOT = PROJECT_ROOT / "docs" / "migration"
ARCHIVE_DOCS_ROOT = MIGRATION_DOCS_ROOT / "archive"
ARCHIVE_SCRIPT_ROOT = PROJECT_ROOT / "scripts" / "migration" / "archive" / "phase_verifiers"
PHASE_STATUS_PATH = PROJECT_ROOT / "docs" / "NATIVE_CLIENT_PHASE_STATUS.md"
CURRENT_PHASE_PATH = MIGRATION_DOCS_ROOT / "current-phase.md"
TASKS_PATH = PROJECT_ROOT / "TASKS.md"
PHASE_REGISTRY_PATH = MIGRATION_DOCS_ROOT / "phase-registry.json"


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


def _load_phase_registry() -> dict[str, object]:
    if not PHASE_REGISTRY_PATH.exists():
        return {"phases": {}, "bundles": {}}
    payload = json.loads(_read(PHASE_REGISTRY_PATH))
    if not isinstance(payload, dict):
        return {"phases": {}, "bundles": {}}
    phases = payload.get("phases", {})
    bundles = payload.get("bundles", {})
    if not isinstance(phases, dict):
        phases = {}
    if not isinstance(bundles, dict):
        bundles = {}
    return {"phases": phases, "bundles": bundles}


def _max_completed_phase() -> int | None:
    text = _read(PHASE_STATUS_PATH)
    values: list[int] = []
    for match in re.finditer(r"PHASE\s+(\d+)(?:\.\d+)?\s+COMPLETE", text):
        try:
            values.append(int(match.group(1)))
        except ValueError:
            continue
    if not values:
        return None
    return max(values)


def _expand_bundle_spec(spec: str, registry: dict[str, object]) -> list[int]:
    spec_value = spec.strip()
    bundles = registry.get("bundles", {})
    if isinstance(bundles, dict) and spec_value in bundles:
        payload = bundles.get(spec_value)
        if isinstance(payload, dict):
            phases = payload.get("phases")
            if isinstance(phases, list):
                parsed = [int(x) for x in phases]
                return parsed
    range_match = re.fullmatch(r"(\d+)-(\d+)", spec_value)
    if range_match:
        start = int(range_match.group(1))
        end = int(range_match.group(2))
        if end < start:
            raise ValueError(f"invalid_bundle_range:{spec_value}")
        return list(range(start, end + 1))
    raise ValueError(f"unknown_bundle:{spec_value}")


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


def _check_phase_registry_exists() -> CheckResult:
    if not PHASE_REGISTRY_PATH.exists():
        return CheckResult("phase_registry_exists", False, f"missing={PHASE_REGISTRY_PATH}")
    try:
        payload = _load_phase_registry()
    except Exception as exc:  # pragma: no cover
        return CheckResult("phase_registry_exists", False, f"invalid_json:{exc}")
    phases = payload.get("phases", {})
    bundles = payload.get("bundles", {})
    phase_count = len(phases) if isinstance(phases, dict) else 0
    bundle_count = len(bundles) if isinstance(bundles, dict) else 0
    if phase_count == 0:
        return CheckResult(
            "phase_registry_exists",
            False,
            f"phase_count={phase_count} bundle_count={bundle_count}",
        )
    return CheckResult(
        "phase_registry_exists",
        True,
        f"phase_count={phase_count} bundle_count={bundle_count}",
    )


def _check_legacy_phase_verifier_archive() -> CheckResult:
    archived = len(list(ARCHIVE_SCRIPT_ROOT.glob("verify_phase*.py")))
    root = len(list((PROJECT_ROOT / "scripts").glob("verify_phase*.py")))
    ok = archived >= 100 and root == 0
    return CheckResult(
        "legacy_phase_verifier_archive",
        ok,
        f"archived={archived} root={root}",
    )


def _check_tasks_phase_command_consolidation() -> CheckResult:
    if not TASKS_PATH.exists():
        return CheckResult("tasks_phase_command_consolidation", False, f"missing={TASKS_PATH}")
    text = _read(TASKS_PATH)
    has_legacy = "scripts/verify_phase" in text
    has_new = "scripts/verify_migration.py --phase" in text
    ok = (not has_legacy) and has_new
    return CheckResult(
        "tasks_phase_command_consolidation",
        ok,
        f"legacy_refs={has_legacy} new_refs={has_new}",
    )


def _check_current_phase_snapshot() -> CheckResult:
    if not CURRENT_PHASE_PATH.exists():
        return CheckResult("current_phase_snapshot", False, f"missing={CURRENT_PHASE_PATH}")
    max_completed = _max_completed_phase()
    if max_completed is None:
        return CheckResult("current_phase_snapshot", False, "no_completed_phases_found")
    text = _read(CURRENT_PHASE_PATH)
    match = re.search(r"Latest completed phase:\s*`(\d+)`", text)
    if not match:
        return CheckResult("current_phase_snapshot", False, "missing_latest_completed_phase_line")
    current_completed = int(match.group(1))
    ok = current_completed == max_completed
    return CheckResult(
        "current_phase_snapshot",
        ok,
        f"current_completed={current_completed} status_max_completed={max_completed}",
    )


def _check_phase(phase: int, require_complete: bool) -> list[CheckResult]:
    phase_docs = _phase_archive_docs().get(phase, [])
    registry = _load_phase_registry()
    phase_config: dict[str, object] = {}
    phases = registry.get("phases", {})
    if isinstance(phases, dict):
        candidate = phases.get(str(phase))
        if isinstance(candidate, dict):
            phase_config = candidate

    evidence_docs: list[Path] = []
    raw_evidence_docs = phase_config.get("evidenceDocs")
    if isinstance(raw_evidence_docs, list):
        for value in raw_evidence_docs:
            if isinstance(value, str) and value.strip():
                evidence_docs.append(PROJECT_ROOT / value)

    evidence_present = [path for path in evidence_docs if path.exists()]
    evidence_missing = [str(path) for path in evidence_docs if not path.exists()]
    doc_ok = bool(phase_docs) or (len(evidence_docs) > 0 and not evidence_missing)
    if not phase_docs and not evidence_docs:
        # Backward compatibility for forward phases that only carry status markers.
        doc_ok = True

    doc_result = CheckResult(
        f"phase_{phase}_archive_doc",
        doc_ok,
        (
            f"docs={[str(path.relative_to(PROJECT_ROOT)) for path in phase_docs]} "
            f"evidence_docs={[str(path.relative_to(PROJECT_ROOT)) for path in evidence_present]} "
            f"missing_evidence={evidence_missing}"
        ),
    )
    started, complete = _phase_markers(phase)
    phase_ok = complete if require_complete else (started or complete)
    marker_result = CheckResult(
        f"phase_{phase}_status_marker",
        phase_ok,
        f"started={started} complete={complete} require_complete={require_complete}",
    )

    results = [doc_result, marker_result]
    raw_checks = phase_config.get("checks")
    extra_checks: list[str] = []
    if isinstance(raw_checks, list):
        for value in raw_checks:
            if isinstance(value, str) and value.strip():
                extra_checks.append(value.strip())

    if not extra_checks and phase in {119, 120}:
        extra_checks = ["runtime_bundle_factory"]

    for check_name in extra_checks:
        nested = _run_named_check(check_name)
        results.extend(nested)
    return results


def _run_named_check(name: str) -> list[CheckResult]:
    normalized = name.strip().lower()
    if normalized == "docs_hierarchy":
        return [_check_docs_hierarchy()]
    if normalized == "archive_integrity":
        return [_check_archive_integrity()]
    if normalized == "runtime_bundle_factory":
        return [_check_runtime_bundle_factory()]
    if normalized == "phase_registry_exists":
        return [_check_phase_registry_exists()]
    if normalized == "legacy_phase_verifier_archive":
        return [_check_legacy_phase_verifier_archive()]
    if normalized == "tasks_phase_command_consolidation":
        return [_check_tasks_phase_command_consolidation()]
    if normalized == "current_phase_snapshot":
        return [_check_current_phase_snapshot()]
    phase_match = re.fullmatch(r"phase_(\d+)", normalized)
    if phase_match:
        return _check_phase(int(phase_match.group(1)), require_complete=False)
    raise ValueError(f"unknown_check:{name}")


def _list_checks() -> int:
    docs_by_phase = _phase_archive_docs()
    registry = _load_phase_registry()
    bundles = registry.get("bundles", {})
    print("Named checks:")
    print("- docs_hierarchy")
    print("- archive_integrity")
    print("- runtime_bundle_factory")
    print("- phase_registry_exists")
    print("- legacy_phase_verifier_archive")
    print("- tasks_phase_command_consolidation")
    print("- current_phase_snapshot")
    print("- phase_<number> (dynamic)")
    print("- bundle support via --bundle <name-or-range>")
    print("")
    if docs_by_phase:
        phases = ", ".join(str(p) for p in sorted(docs_by_phase))
        print(f"Archived phases: {phases}")
    else:
        print("Archived phases: (none found)")
    if isinstance(bundles, dict) and bundles:
        print("")
        print("Registered bundles:")
        for key, value in bundles.items():
            label = ""
            if isinstance(value, dict):
                raw_label = value.get("label")
                if isinstance(raw_label, str):
                    label = raw_label
            print(f"- {key}" + (f" ({label})" if label else ""))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Consolidated migration verifier.")
    parser.add_argument("--phase", type=int, action="append", help="Run checks for phase number (repeatable).")
    parser.add_argument(
        "--bundle",
        action="append",
        help="Run a phase bundle by name from registry or numeric range (for example: 215-220).",
    )
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

    if not args.phase and not args.check and not args.bundle:
        parser.error("provide --phase and/or --bundle and/or --check, or use --list")

    results: list[CheckResult] = []
    phases_to_run: list[int] = []
    if args.phase:
        phases_to_run.extend(args.phase)
    if args.bundle:
        registry = _load_phase_registry()
        for bundle_spec in args.bundle:
            try:
                phases_to_run.extend(_expand_bundle_spec(bundle_spec, registry))
            except ValueError as exc:
                print(f"[migration-verify] ERROR {exc}")
                return 1
    if phases_to_run:
        seen: set[int] = set()
        ordered_phases: list[int] = []
        for phase in phases_to_run:
            if phase in seen:
                continue
            seen.add(phase)
            ordered_phases.append(phase)
        for phase in ordered_phases:
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
