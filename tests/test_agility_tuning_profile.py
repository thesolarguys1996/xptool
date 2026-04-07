import unittest

from runelite_planner.agility import (
    AGILITY_TUNING_PROFILE_DB_PARITY,
    AgilityConfig,
    AgilityStrategy,
    ObstacleStep,
)
from runelite_planner.models import Snapshot
from runelite_planner.runtime_core.models import IntentKind


class AgilityTuningProfileTests(unittest.TestCase):
    @staticmethod
    def _snapshot(*, tick: int, world_x: int, world_y: int, plane: int = 0) -> Snapshot:
        return Snapshot(
            tick=tick,
            logged_in=True,
            bank_open=False,
            inventory_counts={},
            bank_counts={},
            player_animation=0,
            raw={
                "player": {
                    "worldX": int(world_x),
                    "worldY": int(world_y),
                    "plane": int(plane),
                }
            },
        )

    def _build_strategy(self, tuning_profile: str) -> AgilityStrategy:
        step = ObstacleStep(world_x=3103, world_y=3279, plane=0, option_keywords=("climb",))
        return AgilityStrategy(
            cfg=AgilityConfig(
                start_world_x=3103,
                start_world_y=3279,
                start_plane=0,
                course_world_x=3103,
                course_world_y=3279,
                course_plane=0,
                obstacle_route=(step,),
                tuning_profile=tuning_profile,
            )
        )

    def test_db_parity_profile_blocks_immediate_step_retry(self) -> None:
        strategy = self._build_strategy(AGILITY_TUNING_PROFILE_DB_PARITY)
        first = list(strategy.intents(self._snapshot(tick=1, world_x=3103, world_y=3279)))
        second = list(strategy.intents(self._snapshot(tick=2, world_x=3103, world_y=3279)))
        resumed = list(strategy.intents(self._snapshot(tick=8, world_x=3103, world_y=3279)))
        self.assertEqual(1, len(first))
        self.assertEqual([], second)
        self.assertEqual(1, len(resumed))
        self.assertEqual(IntentKind.SCENE_OBJECT_ACTION, resumed[0].kind)

    def test_profile_token_is_normalized_to_db_parity(self) -> None:
        strategy = self._build_strategy("TUNING")
        self.assertEqual(AGILITY_TUNING_PROFILE_DB_PARITY, strategy._tuning.profile_key)


if __name__ == "__main__":
    unittest.main()
