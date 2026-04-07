import unittest

from runelite_planner.activities.registry import (
    DB_PARITY_PROFILE,
    default_headless_activity,
    default_profile_for_activity,
    supported_gui_activities,
    supported_headless_activities,
)


class ActivityRegistryTests(unittest.TestCase):
    def test_headless_activity_defaults(self) -> None:
        self.assertEqual("woodcutting", default_headless_activity())
        self.assertIn("woodcutting", supported_headless_activities())
        self.assertIn("drop_probe", supported_headless_activities())

    def test_gui_activity_subset(self) -> None:
        gui_activities = supported_gui_activities()
        self.assertIn("woodcutting", gui_activities)
        self.assertIn("mining", gui_activities)
        self.assertIn("combat", gui_activities)
        self.assertIn("bank_probe", gui_activities)

    def test_profile_defaults(self) -> None:
        for activity in ("woodcutting", "mining", "fishing", "agility", "combat", "store_bank"):
            self.assertEqual(DB_PARITY_PROFILE, default_profile_for_activity(activity))
        self.assertIsNone(default_profile_for_activity("bank_probe"))
        self.assertIsNone(default_profile_for_activity("drop_probe"))
        self.assertIsNone(default_profile_for_activity("unknown"))


if __name__ == "__main__":
    unittest.main()
