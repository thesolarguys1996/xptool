import unittest

from runelite_planner.runtime_core.core import RuntimeCore
from runelite_planner.runtime_core.models import Intent, IntentKind
from runelite_planner.bank_probe import BankProbeConfig, BankProbePhase, BankProbeStrategy
from runelite_planner.models import Snapshot


class BankProbeActivityTests(unittest.TestCase):
    def _snapshot(
        self,
        *,
        tick: int,
        bank_open: bool,
        inventory_counts: dict[int, int] | None = None,
        bank_counts: dict[int, int] | None = None,
    ) -> Snapshot:
        return Snapshot(
            tick=tick,
            logged_in=True,
            bank_open=bank_open,
            inventory_counts=inventory_counts or {},
            bank_counts=bank_counts or {},
            inventory_slots_used=None,
            player_animation=0,
            raw={},
        )

    def test_bank_probe_phase_sequence(self) -> None:
        strategy = BankProbeStrategy(
            cfg=BankProbeConfig(
                target_world_x=3185,
                target_world_y=3436,
                item_id=1511,
                withdraw_quantity="1",
                deposit_quantity="ALL",
            )
        )

        open_intents = list(strategy.intents(self._snapshot(tick=1, bank_open=False)))
        self.assertEqual(1, len(open_intents))
        self.assertEqual(IntentKind.OPEN_BANK, open_intents[0].kind)

        # Bank opens; strategy advances phase and waits one tick.
        self.assertEqual([], list(strategy.intents(self._snapshot(tick=2, bank_open=True))))
        self.assertEqual(BankProbePhase.WITHDRAW, strategy._phase)

        withdraw_intents = list(
            strategy.intents(self._snapshot(tick=3, bank_open=True, inventory_counts={1511: 0}, bank_counts={1511: 50}))
        )
        self.assertEqual(1, len(withdraw_intents))
        self.assertEqual(IntentKind.WITHDRAW_ITEM, withdraw_intents[0].kind)

        # Item appears in inventory; advance to deposit phase.
        self.assertEqual(
            [],
            list(strategy.intents(self._snapshot(tick=4, bank_open=True, inventory_counts={1511: 1}, bank_counts={1511: 49}))),
        )
        self.assertEqual(BankProbePhase.DEPOSIT, strategy._phase)

        deposit_intents = list(
            strategy.intents(self._snapshot(tick=5, bank_open=True, inventory_counts={1511: 1}, bank_counts={1511: 49}))
        )
        self.assertEqual(1, len(deposit_intents))
        self.assertEqual(IntentKind.DEPOSIT_ITEM, deposit_intents[0].kind)

        # Inventory emptied; advance to close phase.
        self.assertEqual(
            [],
            list(strategy.intents(self._snapshot(tick=6, bank_open=True, inventory_counts={1511: 0}, bank_counts={1511: 50}))),
        )
        self.assertEqual(BankProbePhase.CLOSE, strategy._phase)

        close_intents = list(strategy.intents(self._snapshot(tick=7, bank_open=True)))
        self.assertEqual(1, len(close_intents))
        self.assertEqual(IntentKind.CLOSE_BANK, close_intents[0].kind)

    def test_core_maps_bank_probe_intents_to_commands(self) -> None:
        core = RuntimeCore()
        snapshot = self._snapshot(tick=10, bank_open=False)
        open_intent = Intent(
            intent_key="bank_probe:OPEN_BANK:3185:3436",
            activity="bank_probe",
            kind=IntentKind.OPEN_BANK,
            target={"targetWorldX": 3185, "targetWorldY": 3436},
            params={"targetWorldX": 3185, "targetWorldY": 3436},
            policy_key="bank_probe_open",
            reason="bank_probe_open_bank",
        )
        dispatches = core.on_snapshot(snapshot, [open_intent])
        self.assertEqual(1, len(dispatches))
        self.assertEqual("BANK_OPEN_SAFE", dispatches[0].command.command_type)

        core = RuntimeCore()
        withdraw_intent = Intent(
            intent_key="bank_probe:WITHDRAW_ITEM:1511",
            activity="bank_probe",
            kind=IntentKind.WITHDRAW_ITEM,
            target={"itemId": 1511},
            params={"itemId": 1511, "quantity": "1"},
            policy_key="bank_probe_withdraw",
            reason="bank_probe_withdraw_item",
        )
        dispatches = core.on_snapshot(self._snapshot(tick=11, bank_open=True, bank_counts={1511: 20}), [withdraw_intent])
        self.assertEqual(1, len(dispatches))
        self.assertEqual("WITHDRAW_ITEM", dispatches[0].command.command_type)

        core = RuntimeCore()
        deposit_intent = Intent(
            intent_key="bank_probe:DEPOSIT_ITEM:1511",
            activity="bank_probe",
            kind=IntentKind.DEPOSIT_ITEM,
            target={"itemId": 1511},
            params={"itemId": 1511, "quantity": "ALL"},
            policy_key="bank_probe_deposit",
            reason="bank_probe_deposit_item",
        )
        dispatches = core.on_snapshot(self._snapshot(tick=12, bank_open=True, inventory_counts={1511: 2}), [deposit_intent])
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DEPOSIT_ITEM", dispatches[0].command.command_type)

        core = RuntimeCore()
        close_intent = Intent(
            intent_key="bank_probe:CLOSE_BANK",
            activity="bank_probe",
            kind=IntentKind.CLOSE_BANK,
            target={},
            params={},
            policy_key="bank_probe_close",
            reason="bank_probe_close_bank",
        )
        dispatches = core.on_snapshot(self._snapshot(tick=13, bank_open=True), [close_intent])
        self.assertEqual(1, len(dispatches))
        self.assertEqual("CLOSE_BANK", dispatches[0].command.command_type)


if __name__ == "__main__":
    unittest.main()

