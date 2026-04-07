import unittest
from unittest.mock import patch

from runelite_planner.runtime_core.core import RuntimeCore
from runelite_planner.runtime_core.models import IntentKind
from runelite_planner.activity_profiles import FISHING_PROFILE_DB_PARITY
from runelite_planner.fishing import (
    FishingConfig,
    FishingStrategy,
    apply_fishing_drop_speed_bias,
    parse_drop_cadence_tuning_overrides,
    parse_fishing_motor_tuning_overrides,
)
from runelite_planner.models import Snapshot


class FishingActivityTests(unittest.TestCase):
    @staticmethod
    def _enable_drop_tuning(strategy: FishingStrategy) -> None:
        strategy._drop_cadence_tuning_payload = {
            "localCooldownMinMs": 18,
            "localCooldownMaxMs": 52,
            "secondDispatchChancePercent": 34,
            "rhythmPauseChanceMinPercent": 0,
            "rhythmPauseChanceMaxPercent": 4,
            "rhythmPauseRampStartDispatches": 9,
            "sessionCooldownBiasMs": 4,
            "targetCycleClicksMedian": 24,
            "targetCycleDurationMsMedian": 11800,
        }

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

    def test_fishing_strategy_emits_fish_intent_when_idle(self) -> None:
        strategy = FishingStrategy(cfg=FishingConfig(stop_when_inventory_full=True))
        intents = list(strategy.intents(self._snapshot(tick=1, inventory_slots_used=20, player_animation=0)))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.FISH_SPOT, intents[0].kind)
        self.assertEqual("NEAREST_FISHING_SPOT", intents[0].params.get("targetCategory"))

    def test_fishing_strategy_holds_when_full(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=False,
                stop_when_inventory_full=True,
            )
        )
        _ = list(strategy.intents(self._snapshot(tick=2, inventory_slots_used=28, player_animation=0)))
        intents = list(strategy.intents(self._snapshot(tick=3, inventory_slots_used=28, player_animation=0)))
        self.assertEqual([], intents)

    def test_core_maps_fishing_intent_to_fishing_command(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=False,
                stop_when_inventory_full=False,
            )
        )
        intents = list(strategy.intents(self._snapshot(tick=3, inventory_slots_used=18, player_animation=0)))
        self.assertEqual(1, len(intents))

        core = RuntimeCore()
        dispatches = core.on_snapshot(self._snapshot(tick=3, inventory_slots_used=18, player_animation=0), intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual("FISH_NEAREST_SPOT_SAFE", dispatches[0].command.command_type)

    def test_fishing_start_drop_session_includes_all_configured_item_ids(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=True,
                fish_item_ids=(317, 321),
                stop_when_inventory_full=False,
            )
        )
        self._enable_drop_tuning(strategy)
        snapshot = Snapshot(
            tick=5,
            logged_in=True,
            bank_open=False,
            inventory_counts={317: 8, 321: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(snapshot))
        snapshot_confirmed = Snapshot(
            tick=6,
            logged_in=True,
            bank_open=False,
            inventory_counts={317: 8, 321: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        intents = list(strategy.intents(snapshot_confirmed))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)
        core = RuntimeCore()
        dispatches = core.on_snapshot(snapshot_confirmed, intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DROP_START_SESSION", dispatches[0].command.command_type)
        self.assertEqual([317, 321], dispatches[0].command.payload.get("itemIds"))
        self.assertEqual("DB_PARITY", dispatches[0].command.payload.get("dropCadenceProfile"))

    def test_fishing_strategy_drops_multiple_configured_ids_when_started(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=True,
                fish_item_ids=(317, 321),
                stop_when_inventory_full=False,
            )
        )
        self._enable_drop_tuning(strategy)
        s1 = Snapshot(
            tick=10,
            logged_in=True,
            bank_open=False,
            inventory_counts={317: 10, 321: 6},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(s1))
        s1_confirmed = Snapshot(
            tick=11,
            logged_in=True,
            bank_open=False,
            inventory_counts={317: 10, 321: 6},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        intents = list(strategy.intents(s1_confirmed))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)
        self.assertEqual(317, intents[0].params.get("itemId"))

        # Shrimp drained, anchovies remain: strategy should keep session active with no restart.
        s2 = Snapshot(
            tick=12,
            logged_in=True,
            bank_open=False,
            inventory_counts={321: 6},
            bank_counts={},
            inventory_slots_used=18,
            player_animation=0,
            raw={},
        )
        self.assertEqual([], list(strategy.intents(s2)))

        s3 = Snapshot(
            tick=13,
            logged_in=True,
            bank_open=False,
            inventory_counts={321: 6},
            bank_counts={},
            inventory_slots_used=18,
            player_animation=0,
            raw={},
        )
        self.assertEqual([], list(strategy.intents(s3)))

    def test_fishing_db_parity_profile_sets_drop_cadence_payload(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=True,
                fish_item_ids=(335, 331),
                stop_when_inventory_full=False,
                tuning_profile=FISHING_PROFILE_DB_PARITY,
            )
        )
        self._enable_drop_tuning(strategy)
        s1 = Snapshot(
            tick=30,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(s1))
        s2 = Snapshot(
            tick=31,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        intents = list(strategy.intents(s2))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)
        core = RuntimeCore()
        dispatches = core.on_snapshot(s2, intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DB_PARITY", dispatches[0].command.payload.get("dropCadenceProfile"))

    def test_fishing_start_drop_session_includes_detected_secondary_fish_ids(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=True,
                fish_item_ids=(335,),
                stop_when_inventory_full=False,
            )
        )
        self._enable_drop_tuning(strategy)
        s1 = Snapshot(
            tick=35,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(s1))
        s2 = Snapshot(
            tick=36,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        intents = list(strategy.intents(s2))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)
        core = RuntimeCore()
        dispatches = core.on_snapshot(s2, intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual("DROP_START_SESSION", dispatches[0].command.command_type)
        self.assertEqual([335, 331], dispatches[0].command.payload.get("itemIds"))

    def test_fishing_db_parity_profile_includes_drop_cadence_tuning_payload(self) -> None:
        tuning_payload = {
            "localCooldownMinMs": 18,
            "localCooldownMaxMs": 64,
            "secondDispatchChancePercent": 37,
            "rhythmPauseChanceMinPercent": 0,
            "rhythmPauseChanceMaxPercent": 4,
            "rhythmPauseRampStartDispatches": 10,
            "sessionTickSkipChancePercent": 0,
            "sessionBurstPauseThreshold": 9,
            "sessionBurstPauseChancePercent": 5,
            "sessionCooldownBiasMs": 6,
            "targetCycleClicksMedian": 28,
            "targetCycleDurationMsMedian": 12199,
        }
        idle_tuning_payload = {
            "fishingIdleMinIntervalTicks": 4,
            "fishingIdleMaxIntervalTicks": 10,
            "fishingIdleRetryMinIntervalTicks": 2,
            "fishingIdleRetryMaxIntervalTicks": 7,
            "fishingDbParityIdleMinIntervalTicks": 3,
            "fishingDbParityIdleMaxIntervalTicks": 9,
            "fishingDbParityIdleRetryMinIntervalTicks": 1,
            "fishingDbParityIdleRetryMaxIntervalTicks": 6,
            "postDropIdleCooldownMinTicks": 4,
            "postDropIdleCooldownMaxTicks": 9,
            "postDropIdleDbParityCooldownMinTicks": 2,
            "postDropIdleDbParityCooldownMaxTicks": 7,
            "profileHoverChancePercent": 7,
            "profileDriftChancePercent": 44,
            "profileCameraChancePercent": 10,
            "profileNoopChancePercent": 39,
            "profileParkAfterBurstMinActions": 3,
            "profileParkAfterBurstChancePercent": 30,
        }
        with patch(
            "runelite_planner.fishing.resolve_drop_cadence_tuning_payload",
            return_value=dict(tuning_payload),
        ), patch(
            "runelite_planner.fishing.resolve_idle_cadence_tuning_payload",
            return_value=dict(idle_tuning_payload),
        ):
            strategy = FishingStrategy(
                cfg=FishingConfig(
                    auto_drop_when_full=True,
                    fish_item_ids=(335, 331),
                    stop_when_inventory_full=False,
                    tuning_profile=FISHING_PROFILE_DB_PARITY,
                )
            )
        s1 = Snapshot(
            tick=40,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(s1))
        s2 = Snapshot(
            tick=41,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        intents = list(strategy.intents(s2))
        self.assertEqual(1, len(intents))
        self.assertEqual(IntentKind.START_DROP_SESSION, intents[0].kind)
        expected_drop_tuning = apply_fishing_drop_speed_bias(tuning_payload)
        self.assertEqual(expected_drop_tuning, intents[0].params.get("dropCadenceTuning"))
        self.assertEqual(idle_tuning_payload, intents[0].params.get("idleCadenceTuning"))
        core = RuntimeCore()
        dispatches = core.on_snapshot(s2, intents)
        self.assertEqual(1, len(dispatches))
        self.assertEqual(expected_drop_tuning, dispatches[0].command.payload.get("dropCadenceTuning"))
        self.assertEqual(idle_tuning_payload, dispatches[0].command.payload.get("idleCadenceTuning"))

    def test_parse_fishing_drop_tuning_overrides_clamps_and_normalizes_ranges(self) -> None:
        parsed = parse_drop_cadence_tuning_overrides(
            "localCooldownMinMs=900,localCooldownMaxMs=10,rhythmPauseChanceMinPercent=90,rhythmPauseChanceMaxPercent=10"
        )
        self.assertIsNotNone(parsed)
        self.assertEqual(500, parsed.get("localCooldownMinMs"))
        self.assertEqual(500, parsed.get("localCooldownMaxMs"))
        self.assertEqual(90, parsed.get("rhythmPauseChanceMinPercent"))
        self.assertEqual(90, parsed.get("rhythmPauseChanceMaxPercent"))

    def test_parse_fishing_drop_tuning_overrides_rejects_unknown_keys(self) -> None:
        with self.assertRaises(ValueError):
            parse_drop_cadence_tuning_overrides("notARealKey=12")

    def test_parse_fishing_motor_tuning_overrides_clamps(self) -> None:
        parsed = parse_fishing_motor_tuning_overrides(
            "moveAccelPercent=122,moveDecelPercent=-5,terminalSlowdownRadiusPx=999"
        )
        self.assertIsNotNone(parsed)
        self.assertEqual(100, parsed.get("moveAccelPercent"))
        self.assertEqual(0, parsed.get("moveDecelPercent"))
        self.assertEqual(260, parsed.get("terminalSlowdownRadiusPx"))

    def test_parse_fishing_motor_tuning_overrides_rejects_unknown_keys(self) -> None:
        with self.assertRaises(ValueError):
            parse_fishing_motor_tuning_overrides("notARealMotorKey=12")

    def test_fishing_drop_tuning_overrides_merge_with_db_payload(self) -> None:
        base_tuning_payload = {
            "localCooldownMinMs": 18,
            "localCooldownMaxMs": 64,
            "secondDispatchChancePercent": 37,
            "rhythmPauseChanceMinPercent": 0,
            "rhythmPauseChanceMaxPercent": 4,
            "rhythmPauseRampStartDispatches": 10,
            "sessionCooldownBiasMs": 6,
            "targetCycleClicksMedian": 28,
            "targetCycleDurationMsMedian": 12199,
            "sessionTickSkipChancePercent": 0,
            "sessionBurstPauseThreshold": 9,
            "sessionBurstPauseChancePercent": 5,
        }
        idle_tuning_payload = {
            "fishingIdleMinIntervalTicks": 4,
            "fishingIdleMaxIntervalTicks": 10,
            "fishingIdleRetryMinIntervalTicks": 2,
            "fishingIdleRetryMaxIntervalTicks": 7,
            "fishingDbParityIdleMinIntervalTicks": 3,
            "fishingDbParityIdleMaxIntervalTicks": 9,
            "fishingDbParityIdleRetryMinIntervalTicks": 1,
            "fishingDbParityIdleRetryMaxIntervalTicks": 6,
        }
        with patch(
            "runelite_planner.fishing.resolve_drop_cadence_tuning_payload",
            return_value=dict(base_tuning_payload),
        ), patch(
            "runelite_planner.fishing.resolve_idle_cadence_tuning_payload",
            return_value=dict(idle_tuning_payload),
        ):
            strategy = FishingStrategy(
                cfg=FishingConfig(
                    auto_drop_when_full=True,
                    fish_item_ids=(335, 331),
                    stop_when_inventory_full=False,
                    tuning_profile=FISHING_PROFILE_DB_PARITY,
                    fishing_drop_tuning={
                        "localCooldownMinMs": 40,
                        "localCooldownMaxMs": 80,
                        "secondDispatchChancePercent": 22,
                    },
                )
            )
        s1 = Snapshot(
            tick=50,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(s1))
        s2 = Snapshot(
            tick=51,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 9, 331: 9},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        intents = list(strategy.intents(s2))
        self.assertEqual(1, len(intents))
        merged_payload = intents[0].params.get("dropCadenceTuning")
        self.assertIsNotNone(merged_payload)
        self.assertEqual(40, merged_payload.get("localCooldownMinMs"))
        self.assertEqual(80, merged_payload.get("localCooldownMaxMs"))
        self.assertEqual(22, merged_payload.get("secondDispatchChancePercent"))
        self.assertEqual(9, merged_payload.get("sessionBurstPauseThreshold"))

    def test_fishing_motor_tuning_overrides_pass_to_runtime_payload(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=False,
                stop_when_inventory_full=False,
                fishing_motor_tuning={
                    "moveAccelPercent": 42,
                    "moveDecelPercent": 68,
                    "terminalSlowdownRadiusPx": 96,
                },
            )
        )
        snapshot = self._snapshot(tick=60, inventory_slots_used=14, player_animation=0)
        intents = list(strategy.intents(snapshot))
        self.assertEqual(1, len(intents))
        self.assertEqual(42, intents[0].params.get("moveAccelPercent"))
        self.assertEqual(68, intents[0].params.get("moveDecelPercent"))
        self.assertEqual(96, intents[0].params.get("terminalSlowdownRadiusPx"))
        core = RuntimeCore()
        dispatches = core.on_snapshot(snapshot, intents)
        self.assertEqual(1, len(dispatches))
        payload = dispatches[0].command.payload
        self.assertEqual(42, payload.get("moveAccelPercent"))
        self.assertEqual(68, payload.get("moveDecelPercent"))
        self.assertEqual(96, payload.get("terminalSlowdownRadiusPx"))

    def test_fishing_strict_drop_tuning_blocks_start_when_missing(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=True,
                fish_item_ids=(335, 331),
                stop_when_inventory_full=False,
            )
        )
        # Strict mode: do not permit drop-session start without cadence tuning.
        strategy._drop_cadence_tuning_payload = None
        s1 = Snapshot(
            tick=70,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 10, 331: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(s1))
        s2 = Snapshot(
            tick=71,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 10, 331: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        intents = list(strategy.intents(s2))
        self.assertEqual([], intents)

    def test_fishing_strict_drop_tuning_warning_is_latched_until_condition_clears(self) -> None:
        strategy = FishingStrategy(
            cfg=FishingConfig(
                auto_drop_when_full=True,
                fish_item_ids=(335, 331),
                stop_when_inventory_full=False,
            )
        )
        strategy._drop_cadence_tuning_payload = None
        blocked = Snapshot(
            tick=80,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 10, 331: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(blocked))
        blocked_confirmed = Snapshot(
            tick=81,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 10, 331: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(blocked_confirmed))
        warnings = strategy.consume_runtime_warnings()
        self.assertEqual(1, len(warnings))
        self.assertIn("strict_drop_tuning", warnings[0])

        # Still blocked on next tick: warning should not spam.
        blocked_next = Snapshot(
            tick=82,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 10, 331: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(blocked_next))
        self.assertEqual((), strategy.consume_runtime_warnings())

        # Clear block condition (inventory not full), then block again => warning re-arms once.
        clear_state = Snapshot(
            tick=83,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 4, 331: 3},
            bank_counts={},
            inventory_slots_used=20,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(clear_state))
        self.assertEqual((), strategy.consume_runtime_warnings())

        blocked_again = Snapshot(
            tick=84,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 10, 331: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(blocked_again))
        blocked_again_confirmed = Snapshot(
            tick=85,
            logged_in=True,
            bank_open=False,
            inventory_counts={335: 10, 331: 8},
            bank_counts={},
            inventory_slots_used=28,
            player_animation=0,
            raw={},
        )
        _ = list(strategy.intents(blocked_again_confirmed))
        warnings_again = strategy.consume_runtime_warnings()
        self.assertEqual(1, len(warnings_again))
        self.assertIn("strict_drop_tuning", warnings_again[0])


if __name__ == "__main__":
    unittest.main()

