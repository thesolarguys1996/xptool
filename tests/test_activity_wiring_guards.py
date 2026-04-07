from __future__ import annotations

import ast
from pathlib import Path
import unittest


class ActivityWiringGuardTests(unittest.TestCase):
    def test_main_activity_constants_are_registry_driven(self) -> None:
        module = ast.parse(
            Path("src/runelite_planner/main.py").read_text(encoding="utf-8")
        )
        assignments: dict[str, ast.AST] = {}
        for node in module.body:
            if not isinstance(node, ast.Assign):
                continue
            if len(node.targets) != 1 or not isinstance(node.targets[0], ast.Name):
                continue
            assignments[node.targets[0].id] = node.value

        choices_expr = assignments.get("ACTIVITY_CHOICES")
        self.assertIsNotNone(choices_expr, "ACTIVITY_CHOICES assignment missing")
        self.assertIsInstance(choices_expr, ast.Call)
        self.assertIsInstance(choices_expr.func, ast.Name)
        self.assertEqual("list", choices_expr.func.id)
        self.assertEqual(1, len(choices_expr.args))
        self.assertIsInstance(choices_expr.args[0], ast.Call)
        self.assertIsInstance(choices_expr.args[0].func, ast.Name)
        self.assertEqual("supported_headless_activities", choices_expr.args[0].func.id)

        default_expr = assignments.get("DEFAULT_ACTIVITY")
        self.assertIsNotNone(default_expr, "DEFAULT_ACTIVITY assignment missing")
        self.assertIsInstance(default_expr, ast.Call)
        self.assertIsInstance(default_expr.func, ast.Name)
        self.assertEqual("default_headless_activity", default_expr.func.id)

    def test_gui_start_uses_builder_and_has_no_activity_literal_branches(self) -> None:
        module = ast.parse(
            Path("src/runelite_planner/gui.py").read_text(encoding="utf-8")
        )
        start_method = self._find_method(module, class_name="RuntimeGUI", method_name="start")

        has_builder_call = False
        for node in ast.walk(start_method):
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Name):
                if node.func.id == "build_activity_strategy":
                    has_builder_call = True

            if isinstance(node, ast.Compare):
                sides = [node.left, *node.comparators]
                references_activity = any(
                    isinstance(side, ast.Name) and side.id == "activity" for side in sides
                )
                has_string_literal = any(
                    isinstance(side, ast.Constant) and isinstance(side.value, str)
                    for side in sides
                )
                if references_activity and has_string_literal:
                    self.fail(
                        "RuntimeGUI.start() should not hardcode activity literal branching; "
                        "use build_activity_strategy() dispatch."
                    )

        self.assertTrue(
            has_builder_call,
            "RuntimeGUI.start() must call build_activity_strategy() for activity dispatch",
        )

    @staticmethod
    def _find_method(module: ast.Module, *, class_name: str, method_name: str) -> ast.FunctionDef:
        for node in module.body:
            if not isinstance(node, ast.ClassDef) or node.name != class_name:
                continue
            for member in node.body:
                if isinstance(member, ast.FunctionDef) and member.name == method_name:
                    return member
        raise AssertionError(f"Unable to find {class_name}.{method_name}")


if __name__ == "__main__":
    unittest.main()
