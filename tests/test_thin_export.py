from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SCRIPTS_ROOT = PROJECT_ROOT / "scripts"
if str(SCRIPTS_ROOT) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_ROOT))

from export_thin_client_layout import main as export_main


class ThinExportTests(unittest.TestCase):
    def test_export_thin_layout_excludes_local_strategy_and_runtime_core(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            out = Path(tmpdir) / "thin"
            exit_code = export_main(["--output", str(out)])
            self.assertEqual(0, exit_code)

            package_root = out / "src" / "runelite_planner"
            self.assertTrue((package_root / "main.py").exists())
            self.assertTrue((package_root / "command_bus_contract.py").exists())
            self.assertTrue((package_root / "protocol.py").exists())
            self.assertTrue((package_root / "schemas" / "bridge" / "auth_hello.schema.json").exists())
            self.assertTrue((out / "pyproject.toml").exists())
            self.assertFalse((package_root / "runtime_core").exists())
            self.assertFalse((package_root / "woodcutting.py").exists())
            self.assertFalse((package_root / "agility.py").exists())


if __name__ == "__main__":
    unittest.main()
