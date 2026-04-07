from __future__ import annotations

import os
from pathlib import Path
import shutil
import subprocess
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]


def _run(
    name: str,
    cmd: list[str],
    *,
    env_overrides: dict[str, str] | None = None,
    expected_codes: set[int] | None = None,
    must_contain: str | None = None,
) -> None:
    if expected_codes is None:
        expected_codes = {0}
    env = os.environ.copy()
    if env_overrides:
        env.update(env_overrides)
    print(f"[CHECK] {name}")
    print(f"[CMD] {' '.join(cmd)}")
    proc = subprocess.run(
        cmd,
        cwd=PROJECT_ROOT,
        env=env,
        text=True,
        capture_output=True,
    )
    if proc.stdout:
        print(proc.stdout.rstrip())
    if proc.stderr:
        print(proc.stderr.rstrip())
    if proc.returncode not in expected_codes:
        raise RuntimeError(
            f"{name}_failed_unexpected_exit: got={proc.returncode} expected={sorted(expected_codes)}"
        )
    if must_contain is not None:
        combined = f"{proc.stdout}\n{proc.stderr}"
        if must_contain not in combined:
            raise RuntimeError(f"{name}_failed_missing_text:{must_contain}")


def _pythonpath(path: Path) -> str:
    existing = os.environ.get("PYTHONPATH", "")
    if not existing:
        return str(path)
    return f"{str(path)}{os.pathsep}{existing}"


def _verify_thin_layout(layout_root: Path) -> None:
    package_root = layout_root / "src" / "runelite_planner"
    required = (
        package_root / "main.py",
        package_root / "runner.py",
        package_root / "remote_planner.py",
        layout_root / "pyproject.toml",
    )
    for path in required:
        if not path.exists():
            raise RuntimeError(f"thin_layout_missing_required:{path}")
    disallowed = (
        package_root / "runtime_core",
        package_root / "woodcutting.py",
        package_root / "agility.py",
        package_root / "fishing.py",
        package_root / "mining.py",
    )
    for path in disallowed:
        if path.exists():
            raise RuntimeError(f"thin_layout_contains_disallowed:{path}")


def main() -> int:
    src_path = PROJECT_ROOT / "src"
    thin_output = PROJECT_ROOT / "build" / "thin-client-ci"

    # Current Priority #1
    _run(
        "full_test_suite",
        [sys.executable, "-m", "unittest", "discover", "-s", "tests"],
        env_overrides={"PYTHONPATH": _pythonpath(src_path)},
    )

    # Current Priority #2
    _run(
        "strict_remote_policy_lock",
        [sys.executable, "-m", "runelite_planner.main", "--dry-run"],
        env_overrides={"PYTHONPATH": _pythonpath(src_path)},
        expected_codes={2},
        must_contain="strict_remote_policy_violation remote_planner_url_required",
    )

    # Current Priority #3
    if thin_output.exists():
        shutil.rmtree(thin_output)
    _run(
        "thin_export_generate",
        [
            sys.executable,
            "scripts/export_thin_client_layout.py",
            "--output",
            str(thin_output),
        ],
    )
    _verify_thin_layout(thin_output)
    _run(
        "thin_export_boot",
        [sys.executable, "-m", "runelite_planner.main", "--help"],
        env_overrides={"PYTHONPATH": _pythonpath(thin_output / "src")},
    )

    print("[OK] TASKS current-priority checks passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
