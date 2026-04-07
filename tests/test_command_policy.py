import unittest

from runelite_planner.command_policy import (
    is_supported_command_type,
    normalize_command_type,
)


class CommandPolicyTests(unittest.TestCase):
    def test_normalize_command_type(self) -> None:
        self.assertEqual("DROP_ITEM_SAFE", normalize_command_type(" drop_item_safe "))

    def test_supported_command_type_allowlist(self) -> None:
        self.assertTrue(is_supported_command_type("DROP_ITEM_SAFE"))
        self.assertTrue(is_supported_command_type("logout_safe"))
        self.assertFalse(is_supported_command_type("UNSUPPORTED_FAKE_COMMAND"))


if __name__ == "__main__":
    unittest.main()
