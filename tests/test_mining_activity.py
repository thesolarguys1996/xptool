import unittest

from runelite_planner.runtime_core.core import RuntimeCore
from runelite_planner.runtime_core.models import IntentKind
from runelite_planner.mining import MiningConfig, MiningStrategy
from runelite_planner.models import Snapshot


class MiningActivityTests(unittest.TestCase):
    def _snapshot(
        self,
        *,
        tick: int,
        logged_in: bool = True,
        bank_open: bool = False,
        inventory_slots_used: int | None = None,
        player_animation: int = 0,
    ) -> Snapshot:
        return Snapshot(
            tick=tick,
            logged_in=logged_in,
            bank_open=bank_open,
            inventory_counts={},
            bank_counts={},
            inventory_slots_used=inventory_slots_used,
            player_animation=player_animation,
            raw={},
        )

    def test_mining_strategy_emits_selected_mine_intent_when_idle(self) -> None:
        strategy = MiningStrategy(cfg=MiningConfig(stop_when_inventory_full=True))
        intents = list(strategy.intents(self._snapshot(tick=1, inventory_slots_used=20, player_animation=0)))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.MINE_ROCK, intents[0].kind)
        self.assertEqual("SELECTED", intents[0].params.get("targetCategory"))

    def test_mining_strategy_holds_when_full(self) -> None:
        strategy = MiningStrategy(cfg=MiningConfig(stop_when_inventory_full=True))
        intents = list(strategy.intents(self._snapshot(tick=2, inventory_slots_used=28, player_animation=0)))
        self.assertEqual([], intents)

    def test_core_maps_mining_intent_to_mining_command(self) -> None:
        strategy = MiningStrategy(cfg=MiningConfig(stop_when_inventory_full=False))
        intents = list(strategy.intents(self._snapshot(tick=3, inventory_slots_used=18, player_animation=0)))
        self.assertEqual(1, len(intents))

        core = RuntimeCore()
        dispatches = core.on_snapshot(self._snapshot(tick=3, inventory_slots_used=18, player_animation=0), intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual("MINE_NEAREST_ROCK_SAFE", dispatches[0].command.command_type)


if __name__ == "__main__":
    unittest.main()

