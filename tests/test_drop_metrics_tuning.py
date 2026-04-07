import unittest

from runelite_planner.drop_metrics_tuning import (
    DropCycleSample,
    derive_drop_cadence_tuning_from_samples,
    _looks_like_manual_drop_label,
)


class DropMetricsTuningTests(unittest.TestCase):
    def test_derive_returns_none_when_not_enough_manual_samples(self) -> None:
        samples = [
            DropCycleSample(avg_clicks_per_cycle=27.5, avg_cycle_duration_ms=12_200.0),
            DropCycleSample(avg_clicks_per_cycle=28.0, avg_cycle_duration_ms=11_900.0),
        ]
        self.assertIsNone(derive_drop_cadence_tuning_from_samples(samples))

    def test_derive_returns_expected_tuning_shape(self) -> None:
        samples = [
            DropCycleSample(avg_clicks_per_cycle=27.9, avg_cycle_duration_ms=12_450.0),
            DropCycleSample(avg_clicks_per_cycle=27.4, avg_cycle_duration_ms=12_210.0),
            DropCycleSample(avg_clicks_per_cycle=28.3, avg_cycle_duration_ms=12_180.0),
            DropCycleSample(avg_clicks_per_cycle=26.8, avg_cycle_duration_ms=10_760.0),
            DropCycleSample(avg_clicks_per_cycle=27.7, avg_cycle_duration_ms=12_200.0),
        ]
        tuning = derive_drop_cadence_tuning_from_samples(samples)
        self.assertIsNotNone(tuning)
        assert tuning is not None
        for key in (
            "localCooldownMinMs",
            "localCooldownMaxMs",
            "secondDispatchChancePercent",
            "rhythmPauseChanceMinPercent",
            "rhythmPauseChanceMaxPercent",
            "rhythmPauseRampStartDispatches",
            "sessionTickSkipChancePercent",
            "sessionBurstPauseThreshold",
            "sessionBurstPauseChancePercent",
            "sessionCooldownBiasMs",
            "targetCycleClicksMedian",
            "targetCycleDurationMsMedian",
        ):
            self.assertIn(key, tuning)
        self.assertGreaterEqual(int(tuning["localCooldownMaxMs"]), int(tuning["localCooldownMinMs"]))
        self.assertGreaterEqual(int(tuning["targetCycleClicksMedian"]), 20)
        self.assertLessEqual(int(tuning["targetCycleClicksMedian"]), 34)

    def test_label_matching_accepts_activity_loop_tests(self) -> None:
        self.assertTrue(
            _looks_like_manual_drop_label("WoodcuttingLoopTest17", activity_key="woodcutting")
        )
        self.assertTrue(
            _looks_like_manual_drop_label("FishingLoopTest03", activity_key="fishing")
        )
        self.assertFalse(
            _looks_like_manual_drop_label("WoodcuttingLoopTest17", activity_key="fishing")
        )


if __name__ == "__main__":
    unittest.main()
