import argparse
import unittest

from runelite_planner.activity_profiles import (
    COMBAT_PROFILE_DB_PARITY,
    FISHING_PROFILE_DB_PARITY,
    MINING_PROFILE_DB_PARITY,
    WOODCUTTING_PROFILE_DB_PARITY,
    resolve_fishing_behavior_profile,
)
from runelite_planner.combat import add_args as add_combat_args
from runelite_planner.combat import build_strategy as build_combat_strategy
from runelite_planner.fishing import add_args as add_fishing_args
from runelite_planner.fishing import build_strategy as build_fishing_strategy
from runelite_planner.mining import add_args as add_mining_args
from runelite_planner.mining import build_strategy as build_mining_strategy
from runelite_planner.woodcutting import add_args as add_woodcutting_args
from runelite_planner.woodcutting import build_strategy as build_woodcutting_strategy


class ActivityTuningProfileTests(unittest.TestCase):
    def test_mining_cli_builder_accepts_db_parity_profile(self) -> None:
        parser = argparse.ArgumentParser()
        add_mining_args(parser)
        args = parser.parse_args(["--mining-tuning-profile", MINING_PROFILE_DB_PARITY])
        strategy = build_mining_strategy(args)
        self.assertEqual(MINING_PROFILE_DB_PARITY, strategy._profile.profile_key)

    def test_woodcutting_cli_builder_accepts_db_parity_profile(self) -> None:
        parser = argparse.ArgumentParser()
        add_woodcutting_args(parser)
        args = parser.parse_args(["--woodcutting-tuning-profile", WOODCUTTING_PROFILE_DB_PARITY])
        strategy = build_woodcutting_strategy(args)
        self.assertEqual(WOODCUTTING_PROFILE_DB_PARITY, strategy._profile.profile_key)

    def test_combat_cli_builder_accepts_db_parity_profile(self) -> None:
        parser = argparse.ArgumentParser()
        add_combat_args(parser)
        args = parser.parse_args(["--combat-tuning-profile", COMBAT_PROFILE_DB_PARITY])
        strategy = build_combat_strategy(args)
        self.assertEqual(COMBAT_PROFILE_DB_PARITY, strategy._profile.profile_key)

    def test_fishing_cli_builder_accepts_db_parity_profile(self) -> None:
        parser = argparse.ArgumentParser()
        add_fishing_args(parser)
        args = parser.parse_args(["--fishing-tuning-profile", FISHING_PROFILE_DB_PARITY])
        strategy = build_fishing_strategy(args)
        self.assertEqual(FISHING_PROFILE_DB_PARITY, strategy._profile.profile_key)
        self.assertEqual(FISHING_PROFILE_DB_PARITY, strategy._profile.drop_cadence_profile_key)

    def test_fishing_cli_builder_accepts_fishing_only_drop_tuning(self) -> None:
        parser = argparse.ArgumentParser()
        add_fishing_args(parser)
        args = parser.parse_args(
            [
                "--fishing-tuning-profile",
                FISHING_PROFILE_DB_PARITY,
                "--fishing-drop-tuning",
                "localCooldownMinMs=35,localCooldownMaxMs=90,secondDispatchChancePercent=21",
            ]
        )
        strategy = build_fishing_strategy(args)
        self.assertEqual(35, strategy.cfg.fishing_drop_tuning.get("localCooldownMinMs"))
        self.assertEqual(90, strategy.cfg.fishing_drop_tuning.get("localCooldownMaxMs"))
        self.assertEqual(21, strategy.cfg.fishing_drop_tuning.get("secondDispatchChancePercent"))

    def test_fishing_cli_builder_accepts_fishing_only_motor_tuning(self) -> None:
        parser = argparse.ArgumentParser()
        add_fishing_args(parser)
        args = parser.parse_args(
            [
                "--fishing-tuning-profile",
                FISHING_PROFILE_DB_PARITY,
                "--fishing-motor-tuning",
                "moveAccelPercent=44,moveDecelPercent=70,terminalSlowdownRadiusPx=88",
            ]
        )
        strategy = build_fishing_strategy(args)
        self.assertEqual(44, strategy.cfg.fishing_motor_tuning.get("moveAccelPercent"))
        self.assertEqual(70, strategy.cfg.fishing_motor_tuning.get("moveDecelPercent"))
        self.assertEqual(88, strategy.cfg.fishing_motor_tuning.get("terminalSlowdownRadiusPx"))

    def test_fishing_profile_resolver_always_resolves_db_parity(self) -> None:
        for token in ("soft", "stable", "tuning", "metrics", None):
            resolved = resolve_fishing_behavior_profile(token)
            self.assertEqual(FISHING_PROFILE_DB_PARITY, resolved.profile_key)
            self.assertEqual(FISHING_PROFILE_DB_PARITY, resolved.drop_cadence_profile_key)

    def test_activity_cli_rejects_legacy_profile_token(self) -> None:
        parser = argparse.ArgumentParser()
        add_fishing_args(parser)
        with self.assertRaises(SystemExit):
            parser.parse_args(["--fishing-tuning-profile", "TUNING"])


if __name__ == "__main__":
    unittest.main()
