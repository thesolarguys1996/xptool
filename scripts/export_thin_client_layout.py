from __future__ import annotations

import argparse
from pathlib import Path
import shutil
import textwrap


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src" / "runelite_planner"
DEFAULT_OUTPUT = PROJECT_ROOT / "build" / "thin-client"

THIN_MODULES = (
    "__init__.py",
    "activities/registry.py",
    "bridge.py",
    "command_bus_contract.py",
    "command_policy.py",
    "control_plane.py",
    "main.py",
    "models.py",
    "paths.py",
    "protocol.py",
    "remote_planner.py",
    "runner.py",
    "runtime_strategy.py",
)

THIN_RESOURCE_DIRS = (
    "schemas",
)

THIN_SYNTHETIC_MODULES = {
    "activities/__init__.py": (
        "# Thin-client activities package stub.\n"
    ),
}


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="export_thin_client_layout",
        description="Export thin-client runtime layout without local strategy/runtime modules.",
    )
    parser.add_argument(
        "--output",
        default=str(DEFAULT_OUTPUT),
        help="Output directory for exported thin-client layout",
    )
    return parser.parse_args(argv)


def _copy_thin_modules(output_root: Path) -> None:
    package_root = output_root / "src" / "runelite_planner"
    package_root.mkdir(parents=True, exist_ok=True)
    for rel_name in THIN_MODULES:
        src = SRC_ROOT / rel_name
        if not src.exists():
            raise FileNotFoundError(f"missing_thin_source_module:{src}")
        dst = package_root / rel_name
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
    for rel_dir in THIN_RESOURCE_DIRS:
        src_dir = SRC_ROOT / rel_dir
        if not src_dir.exists():
            raise FileNotFoundError(f"missing_thin_source_resource_dir:{src_dir}")
        dst_dir = package_root / rel_dir
        shutil.copytree(src_dir, dst_dir, dirs_exist_ok=True)
    for rel_name, module_text in THIN_SYNTHETIC_MODULES.items():
        dst = package_root / rel_name
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_text(module_text, encoding="utf-8")


def _write_pyproject(output_root: Path) -> None:
    pyproject = textwrap.dedent(
        """
        [build-system]
        requires = ["setuptools>=68", "wheel"]
        build-backend = "setuptools.build_meta"

        [project]
        name = "xptool-thin-client"
        version = "0.1.0"
        description = "XPTool thin client runtime (remote-planner only)."
        requires-python = ">=3.10"
        dependencies = []

        [project.scripts]
        xptool = "runelite_planner.main:main"

        [tool.setuptools]
        package-dir = {"" = "src"}

        [tool.setuptools.packages.find]
        where = ["src"]
        """
    ).strip() + "\n"
    (output_root / "pyproject.toml").write_text(pyproject, encoding="utf-8")


def _write_manifest(output_root: Path) -> None:
    manifest = textwrap.dedent(
        """
        # Thin Client Export

        This layout intentionally includes only thin-client runtime modules.
        Local strategy modules (`agility.py`, `woodcutting.py`, etc.) and
        `runtime_core/` are excluded from this export.
        """
    ).strip() + "\n"
    (output_root / "THIN_LAYOUT.md").write_text(manifest, encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    output_root = Path(str(args.output)).resolve()
    if output_root.exists():
        shutil.rmtree(output_root)
    output_root.mkdir(parents=True, exist_ok=True)
    _copy_thin_modules(output_root)
    _write_pyproject(output_root)
    _write_manifest(output_root)
    print(f"exported_thin_client_layout:{output_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
