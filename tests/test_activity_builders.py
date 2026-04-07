import unittest

from runelite_planner.activities.builders import (
    ActivityBuildError,
    GuiActivityInputs,
    build_activity_strategy,
)


def _inputs() -> GuiActivityInputs:
    return GuiActivityInputs(
        woodcut_target_category="SELECTED",
        woodcut_target_x="3200",
        woodcut_target_y="3201",
        woodcut_target_max_distance="12",
        woodcut_tuning_profile="DB_PARITY",
        mining_tuning_profile="DB_PARITY",
        combat_npc_id="-1",
        combat_target_x="-1",
        combat_target_y="-1",
        combat_target_max_distance="8",
        combat_max_chase_distance="8",
        combat_eat_hp="-1",
        combat_eat_randomized_pct="0",
        combat_food_item_id="-1",
        combat_encounter_label="None",
        combat_tuning_profile="DB_PARITY",
        bank_probe_target_x="3200",
        bank_probe_target_y="3201",
        bank_probe_item_id="1511",
        bank_probe_withdraw_qty="1",
        bank_probe_deposit_qty="ALL",
    )


class ActivityBuilderTests(unittest.TestCase):
    def test_builds_woodcutting_strategy(self) -> None:
        outcome = build_activity_strategy("woodcutting", inputs=_inputs())
        self.assertEqual("DB_PARITY", outcome.profile)
        self.assertTrue(outcome.info_messages)

    def test_rejects_invalid_profile(self) -> None:
        values = _inputs()
        values = GuiActivityInputs(**{**values.__dict__, "combat_tuning_profile": "TUNING"})
        with self.assertRaises(ActivityBuildError):
            build_activity_strategy("combat", inputs=values)

    def test_builds_bank_probe_strategy(self) -> None:
        outcome = build_activity_strategy("bank_probe", inputs=_inputs())
        self.assertIsNone(outcome.profile)


if __name__ == "__main__":
    unittest.main()
