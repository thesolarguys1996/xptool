import unittest
from unittest.mock import patch

from runelite_planner.runtime_core.core import RuntimeCore
from runelite_planner.runtime_core.models import Intent, IntentKind
from runelite_planner.drop_probe import DropProbeConfig, DropProbeStrategy
from runelite_planner.models import Snapshot
from runelite_planner.woodcutting import (
    WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS,
    WoodcuttingConfig,
    WoodcuttingStrategy,
    apply_woodcutting_drop_speed_bias,
)


class DropProbeActivityTests(unittest.TestCase):
    @staticmethod
    def _enable_drop_tuning(strategy: WoodcuttingStrategy) -> None:
        strategy._drop_cadence_tuning_payload = {
            "localCooldownMinMs": 16,
            "localCooldownMaxMs": 48,
            "secondDispatchChancePercent": 32,
            "rhythmPauseChanceMinPercent": 0,
            "rhythmPauseChanceMaxPercent": 4,
            "rhythmPauseRampStartDispatches": 9,
            "sessionCooldownBiasMs": 4,
            "targetCycleClicksMedian": 25,
            "targetCycleDurationMsMedian": 11900,
        }

    def _snapshot(
        self,
        *,
        tick: int,
        bank_open: bool = False,
        logged_in: bool = True,
        inventory_counts: dict[int, int] | None = None,
        inventory_slots_used: int | None = None,
    ) -> Snapshot:
        return Snapshot(
            tick=tick,
            logged_in=logged_in,
            bank_open=bank_open,
            inventory_counts=inventory_counts or {},
            bank_counts={},
            inventory_slots_used=inventory_slots_used,
            player_animation=0,
            raw={},
        )

    def test_drop_probe_starts_and_stops_session(self) -> None:
        strategy = DropProbeStrategy(cfg=DropProbeConfig(item_id=1519, start_when_full=False))

        start_intents = list(strategy.intents(self._snapshot(tick=1, inventory_counts={1519: 10}, inventory_slots_used=24)))
        self.assertEqual(1, len(start_intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, start_intents[0].kind)

        # Active session should continue without spamming new commands.
        self.assertEqual(
            [],
            list(strategy.intents(self._snapshot(tick=2, inventory_counts={1519: 9}, inventory_slots_used=23))),
        )

        stop_intents = list(strategy.intents(self._snapshot(tick=3, inventory_counts={}, inventory_slots_used=10)))
        self.assertEqual(1, len(stop_intents))
        self.assertEqual(IntentKind.STOP_DROP_SESSION, stop_intents[0].kind)

    def test_drop_probe_start_when_full_gate(self) -> None:
        strategy = DropProbeStrategy(cfg=DropProbeConfig(item_id=1519, start_when_full=True))
        self.assertEqual(
            [],
            list(strategy.intents(self._snapshot(tick=10, inventory_counts={1519: 3}, inventory_slots_used=27))),
        )
        intents = list(strategy.intents(self._snapshot(tick=11, inventory_counts={1519: 3}, inventory_slots_used=28)))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)

    def test_core_maps_drop_session_intents_to_commands(self) -> None:
        core = RuntimeCore()
        start_intent = Intent(
            intent_key="drop_probe:START_DROP_SESSION:1519",
            activity="drop_probe",
            kind=IntentKind.START_DROP_SESSION,
            target={"itemId": 1519},
            params={"itemId": 1519},
            policy_key="drop_probe_session",
            reason="drop_probe_start_drop_session",
        )
        dispatches = core.on_snapshot(self._snapshot(tick=20, inventory_counts={1519: 10}, inventory_slots_used=28), [start_intent])
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DROP_START_SESSION", dispatches[0].command.command_type)

        core = RuntimeCore()
        stop_intent = Intent(
            intent_key="drop_probe:STOP_DROP_SESSION",
            activity="drop_probe",
            kind=IntentKind.STOP_DROP_SESSION,
            target={"itemId": 1519},
            params={"itemId": 1519},
            policy_key="drop_probe_session",
            reason="drop_probe_stop_drop_session_inventory_drained",
        )
        dispatches = core.on_snapshot(self._snapshot(tick=21), [stop_intent])
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DROP_STOP_SESSION", dispatches[0].command.command_type)

    def test_core_blocks_fishing_and_woodcutting_drop_start_without_tuning(self) -> None:
        core = RuntimeCore()
        fishing_start = Intent(
            intent_key="fishing:START_DROP_SESSION:335",
            activity="fishing",
            kind=IntentKind.START_DROP_SESSION,
            target={"itemId": 335},
            params={"itemId": 335},
            policy_key="fishing_drop_session",
            reason="fishing_start_drop_session",
        )
        woodcutting_start = Intent(
            intent_key="woodcutting:START_DROP_SESSION:1519",
            activity="woodcutting",
            kind=IntentKind.START_DROP_SESSION,
            target={"itemId": 1519},
            params={"itemId": 1519},
            policy_key="woodcut_drop_session",
            reason="woodcutting_start_drop_session",
        )
        dispatches = core.on_snapshot(self._snapshot(tick=25, inventory_counts={335: 10}, inventory_slots_used=28), [fishing_start])
        self.assertEqual([], dispatches)
        dispatches = core.on_snapshot(
            self._snapshot(tick=26, inventory_counts={1519: 10}, inventory_slots_used=28),
            [woodcutting_start],
        )
        self.assertEqual([], dispatches)

    def test_woodcutting_uses_shared_drop_controller(self) -> None:
        strategy = WoodcuttingStrategy(cfg=WoodcuttingConfig(auto_drop_when_full=True, log_item_id=1519))
        self._enable_drop_tuning(strategy)

        start = list(strategy.intents(self._snapshot(tick=30, inventory_counts={1519: 28}, inventory_slots_used=28)))
        self.assertEqual(1, len(start))
        self.assertEqual(IntentKind.START_DROP_SESSION, start[0].kind)

        self.assertEqual(
            [],
            list(strategy.intents(self._snapshot(tick=31, inventory_counts={1519: 20}, inventory_slots_used=20))),
        )

        for offset in range(WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS - 1):
            stop_probe = list(
                strategy.intents(
                    self._snapshot(
                        tick=32 + offset,
                        inventory_counts={},
                        inventory_slots_used=5,
                    )
                )
            )
            self.assertEqual([], stop_probe)
        stop = list(
            strategy.intents(
                self._snapshot(
                    tick=32 + WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS - 1,
                    inventory_counts={},
                    inventory_slots_used=5,
                )
            )
        )
        self.assertEqual(1, len(stop))
        self.assertEqual(IntentKind.STOP_DROP_SESSION, stop[0].kind)

    def test_woodcutting_transient_missing_item_counts_delays_stop(self) -> None:
        strategy = WoodcuttingStrategy(cfg=WoodcuttingConfig(auto_drop_when_full=True, log_item_id=1519))
        self._enable_drop_tuning(strategy)

        start = list(strategy.intents(self._snapshot(tick=34, inventory_counts={1519: 28}, inventory_slots_used=28)))
        self.assertEqual(1, len(start))
        self.assertEqual(IntentKind.START_DROP_SESSION, start[0].kind)

        for offset in range(1, WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS):
            intents = list(
                strategy.intents(
                    self._snapshot(
                        tick=34 + offset,
                        inventory_counts={},
                        inventory_slots_used=28,
                    )
                )
            )
            self.assertEqual([], intents)

        stop = list(
            strategy.intents(
                self._snapshot(
                    tick=34 + WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS,
                    inventory_counts={},
                    inventory_slots_used=28,
                )
            )
        )
        self.assertEqual(1, len(stop))
        self.assertEqual(IntentKind.STOP_DROP_SESSION, stop[0].kind)

    def test_woodcutting_start_drop_session_includes_detected_log_item_ids(self) -> None:
        strategy = WoodcuttingStrategy(cfg=WoodcuttingConfig(auto_drop_when_full=True, log_item_id=1519))
        self._enable_drop_tuning(strategy)
        snapshot = self._snapshot(
            tick=40,
            inventory_counts={1519: 12, 1521: 8},
            inventory_slots_used=28,
        )
        intents = list(strategy.intents(snapshot))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)

        core = RuntimeCore()
        dispatches = core.on_snapshot(snapshot, intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DROP_START_SESSION", dispatches[0].command.command_type)
        self.assertEqual([1519, 1521], dispatches[0].command.payload.get("itemIds"))

    def test_woodcutting_session_switches_to_secondary_log_without_restart(self) -> None:
        strategy = WoodcuttingStrategy(cfg=WoodcuttingConfig(auto_drop_when_full=True, log_item_id=1519))
        self._enable_drop_tuning(strategy)

        start = list(strategy.intents(self._snapshot(tick=50, inventory_counts={1519: 14, 1521: 14}, inventory_slots_used=28)))
        self.assertEqual(1, len(start))
        self.assertEqual(IntentKind.START_DROP_SESSION, start[0].kind)
        self.assertEqual(1519, int(strategy._drop.session_item_id or -1))

        # Preferred log drains first; keep active session and retarget to the secondary log.
        follow_up = list(strategy.intents(self._snapshot(tick=51, inventory_counts={1521: 14}, inventory_slots_used=14)))
        self.assertEqual([], follow_up)
        self.assertEqual(1521, int(strategy._drop.session_item_id or -1))

        for offset in range(WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS - 1):
            stop_probe = list(
                strategy.intents(
                    self._snapshot(
                        tick=52 + offset,
                        inventory_counts={},
                        inventory_slots_used=0,
                    )
                )
            )
            self.assertEqual([], stop_probe)
        stop = list(
            strategy.intents(
                self._snapshot(
                    tick=52 + WOODCUTTING_DROP_MISSING_ITEM_STOP_CONFIRM_TICKS - 1,
                    inventory_counts={},
                    inventory_slots_used=0,
                )
            )
        )
        self.assertEqual(1, len(stop))
        self.assertEqual(IntentKind.STOP_DROP_SESSION, stop[0].kind)

    def test_woodcutting_db_parity_profile_includes_drop_and_idle_cadence_tuning(self) -> None:
        drop_tuning_payload = {
            "localCooldownMinMs": 18,
            "localCooldownMaxMs": 60,
            "secondDispatchChancePercent": 33,
            "rhythmPauseChanceMinPercent": 0,
            "rhythmPauseChanceMaxPercent": 4,
            "rhythmPauseRampStartDispatches": 9,
            "sessionTickSkipChancePercent": 0,
            "sessionBurstPauseThreshold": 10,
            "sessionBurstPauseChancePercent": 5,
            "sessionCooldownBiasMs": 4,
            "targetCycleClicksMedian": 26,
            "targetCycleDurationMsMedian": 11800,
        }
        idle_tuning_payload = {
            "offscreenWindowMarginMinPx": 18,
            "offscreenWindowMarginMaxPx": 42,
            "offscreenNearTargetMaxGapPx": 205,
            "offscreenFarTargetMinGapPx": 97,
            "offscreenFarTargetMaxGapPx": 411,
            "profileHoverChancePercent": 8,
            "profileDriftChancePercent": 47,
            "profileCameraChancePercent": 10,
            "profileNoopChancePercent": 35,
            "profileParkAfterBurstMinActions": 3,
            "profileParkAfterBurstChancePercent": 28,
        }
        with patch(
            "runelite_planner.woodcutting.resolve_drop_cadence_tuning_payload",
            return_value=dict(drop_tuning_payload),
        ), patch(
            "runelite_planner.woodcutting.resolve_idle_cadence_tuning_payload",
            return_value=dict(idle_tuning_payload),
        ):
            strategy = WoodcuttingStrategy(
                cfg=WoodcuttingConfig(
                    auto_drop_when_full=True,
                    log_item_id=1519,
                )
            )
        snapshot = self._snapshot(
            tick=60,
            inventory_counts={1519: 12, 1521: 8},
            inventory_slots_used=28,
        )
        intents = list(strategy.intents(snapshot))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)
        self.assertEqual("DB_PARITY", intents[0].params.get("dropCadenceProfile"))
        expected_drop_tuning = apply_woodcutting_drop_speed_bias(drop_tuning_payload)
        self.assertEqual(expected_drop_tuning, intents[0].params.get("dropCadenceTuning"))
        self.assertEqual(idle_tuning_payload, intents[0].params.get("idleCadenceTuning"))

        core = RuntimeCore()
        dispatches = core.on_snapshot(snapshot, intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DROP_START_SESSION", dispatches[0].command.command_type)
        self.assertEqual("DB_PARITY", dispatches[0].command.payload.get("dropCadenceProfile"))
        self.assertEqual(expected_drop_tuning, dispatches[0].command.payload.get("dropCadenceTuning"))
        self.assertEqual(idle_tuning_payload, dispatches[0].command.payload.get("idleCadenceTuning"))

    def test_woodcutting_strict_drop_tuning_blocks_start_when_missing(self) -> None:
        strategy = WoodcuttingStrategy(cfg=WoodcuttingConfig(auto_drop_when_full=True, log_item_id=1519))
        strategy._drop_cadence_tuning_payload = None
        snapshot = self._snapshot(
            tick=63,
            inventory_counts={1519: 12, 1521: 8},
            inventory_slots_used=28,
        )
        intents = list(strategy.intents(snapshot))
        self.assertEqual([], intents)

    def test_woodcutting_strict_drop_tuning_warning_is_latched_until_condition_clears(self) -> None:
        strategy = WoodcuttingStrategy(cfg=WoodcuttingConfig(auto_drop_when_full=True, log_item_id=1519))
        strategy._drop_cadence_tuning_payload = None
        blocked = self._snapshot(
            tick=64,
            inventory_counts={1519: 12, 1521: 8},
            inventory_slots_used=28,
        )
        _ = list(strategy.intents(blocked))
        warnings = strategy.consume_runtime_warnings()
        self.assertEqual(1, len(warnings))
        self.assertIn("strict_drop_tuning", warnings[0])

        blocked_next = self._snapshot(
            tick=65,
            inventory_counts={1519: 12, 1521: 8},
            inventory_slots_used=28,
        )
        _ = list(strategy.intents(blocked_next))
        self.assertEqual((), strategy.consume_runtime_warnings())

        cleared = self._snapshot(
            tick=66,
            inventory_counts={1519: 4},
            inventory_slots_used=20,
        )
        _ = list(strategy.intents(cleared))
        self.assertEqual((), strategy.consume_runtime_warnings())

        blocked_again = self._snapshot(
            tick=67,
            inventory_counts={1519: 12, 1521: 8},
            inventory_slots_used=28,
        )
        _ = list(strategy.intents(blocked_again))
        warnings_again = strategy.consume_runtime_warnings()
        self.assertEqual(1, len(warnings_again))
        self.assertIn("strict_drop_tuning", warnings_again[0])

    def test_drop_start_session_deferred_retries_instead_of_completing(self) -> None:
        core = RuntimeCore()
        start_intent = Intent(
            intent_key="drop_probe:START_DROP_SESSION:1519",
            activity="drop_probe",
            kind=IntentKind.START_DROP_SESSION,
            target={"itemId": 1519},
            params={"itemId": 1519},
            policy_key="drop_probe_session",
            reason="drop_probe_start_drop_session",
        )
        with patch("runelite_planner.runtime_core.scheduler.random.randint", return_value=0):
            first = core.on_snapshot(
                self._snapshot(tick=50, inventory_counts={1519: 10}, inventory_slots_used=28),
                [start_intent],
            )
            self.assertEqual(1, len(first))
            ticket_id = first[0].ticket_id
            core.on_dispatch_enqueued(ticket_id=ticket_id, command_id="cmd-1", tick=50)
            core.on_executor_row(
                {
                    "commandId": "cmd-1",
                    "commandTick": 50,
                    "status": "executed",
                    "eventType": "DEFERRED",
                    "reason": "motor_lock_unavailable",
                    "source": "drop_probe",
                }
            )

            retry = core.on_snapshot(
                self._snapshot(tick=51, inventory_counts={1519: 10}, inventory_slots_used=28),
                [],
            )
            self.assertEqual(1, len(retry))
            self.assertEqual("DROP_START_SESSION", retry[0].command.command_type)

    def test_drop_start_session_fast_path_requires_executor_dispatched(self) -> None:
        core = RuntimeCore()
        start_intent = Intent(
            intent_key="drop_probe:START_DROP_SESSION:1519",
            activity="drop_probe",
            kind=IntentKind.START_DROP_SESSION,
            target={"itemId": 1519},
            params={"itemId": 1519},
            policy_key="drop_probe_session",
            reason="drop_probe_start_drop_session",
        )
        with patch("runelite_planner.runtime_core.scheduler.random.randint", return_value=0):
            first = core.on_snapshot(
                self._snapshot(tick=60, inventory_counts={1519: 10}, inventory_slots_used=28),
                [start_intent],
            )
            self.assertEqual(1, len(first))
            ticket_id = first[0].ticket_id
            core.on_dispatch_enqueued(ticket_id=ticket_id, command_id="cmd-2", tick=60)
            core.on_executor_row(
                {
                    "commandId": "cmd-2",
                    "commandTick": 60,
                    "status": "executed",
                    "eventType": "DISPATCHED",
                    "reason": "drop_session_start_dispatched",
                    "source": "drop_probe",
                }
            )

            follow_up = core.on_snapshot(
                self._snapshot(tick=61, inventory_counts={1519: 10}, inventory_slots_used=28),
                [],
            )
            self.assertEqual([], follow_up)


if __name__ == "__main__":
    unittest.main()

