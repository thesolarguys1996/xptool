import unittest

from runelite_planner.idle_metrics_tuning import (
    IdleSessionSample,
    _looks_like_manual_drop_label,
    derive_idle_cadence_tuning_from_samples,
)


class IdleMetricsTuningTests(unittest.TestCase):
    def test_derive_returns_none_when_not_enough_manual_samples(self) -> None:
        samples = [
            IdleSessionSample(
                avg_clicks_per_cycle=27.5,
                avg_cycle_duration_ms=12_200.0,
                movement_count=200.0,
                hover_count=8.0,
                pause_before_burst_mean_ms=6_000.0,
                pause_after_burst_mean_ms=900.0,
                burst_count_per_min=1.8,
                quality_sample_density_events_per_s=24.0,
            ),
            IdleSessionSample(
                avg_clicks_per_cycle=28.0,
                avg_cycle_duration_ms=12_100.0,
                movement_count=210.0,
                hover_count=6.0,
                pause_before_burst_mean_ms=5_500.0,
                pause_after_burst_mean_ms=870.0,
                burst_count_per_min=1.7,
                quality_sample_density_events_per_s=23.0,
            ),
        ]
        self.assertIsNone(derive_idle_cadence_tuning_from_samples(samples))

    def test_derive_returns_expected_tuning_shape(self) -> None:
        samples = [
            IdleSessionSample(27.9, 12_450.0, 349.0, 6.0, 7_795.0, 5_062.0, 1.82, 23.0),
            IdleSessionSample(27.4, 12_224.0, 152.0, 4.0, 832.0, 7_816.0, 2.62, 24.2),
            IdleSessionSample(27.3, 12_013.0, 306.0, 4.0, 7_027.0, 1_224.0, 1.95, 28.8),
            IdleSessionSample(26.8, 10_759.0, 107.0, 0.0, 614.0, 802.0, 1.64, 19.8),
            IdleSessionSample(27.8, 12_199.0, 221.0, 15.0, 3_260.0, 877.0, 1.79, 26.5),
        ]
        tuning = derive_idle_cadence_tuning_from_samples(samples)
        self.assertIsNotNone(tuning)
        assert tuning is not None
        for key in (
            "fishingIdleMinIntervalTicks",
            "fishingIdleMaxIntervalTicks",
            "fishingIdleRetryMinIntervalTicks",
            "fishingIdleRetryMaxIntervalTicks",
            "fishingDbParityIdleMinIntervalTicks",
            "fishingDbParityIdleMaxIntervalTicks",
            "fishingDbParityIdleRetryMinIntervalTicks",
            "fishingDbParityIdleRetryMaxIntervalTicks",
            "postDropIdleCooldownMinTicks",
            "postDropIdleCooldownMaxTicks",
            "postDropIdleDbParityCooldownMinTicks",
            "postDropIdleDbParityCooldownMaxTicks",
            "profileHoverChancePercent",
            "profileDriftChancePercent",
            "profileCameraChancePercent",
            "profileNoopChancePercent",
            "profileParkAfterBurstMinActions",
            "profileParkAfterBurstChancePercent",
        ):
            self.assertIn(key, tuning)
        self.assertGreaterEqual(
            int(tuning["fishingIdleMaxIntervalTicks"]),
            int(tuning["fishingIdleMinIntervalTicks"]),
        )
        self.assertGreaterEqual(
            int(tuning["fishingIdleRetryMaxIntervalTicks"]),
            int(tuning["fishingIdleRetryMinIntervalTicks"]),
        )

    def test_derive_woodcutting_accepts_lower_density_floor(self) -> None:
        samples = [
            IdleSessionSample(26.0, 14_300.0, 210.0, 8.0, 4_200.0, 1_250.0, 1.6, 5.2),
            IdleSessionSample(24.0, 13_500.0, 180.0, 5.0, 4_800.0, 1_420.0, 1.5, 5.0),
            IdleSessionSample(25.0, 15_100.0, 240.0, 6.0, 5_100.0, 1_520.0, 1.7, 4.8),
        ]
        fishing_tuning = derive_idle_cadence_tuning_from_samples(samples, activity_key="fishing")
        woodcutting_tuning = derive_idle_cadence_tuning_from_samples(samples, activity_key="woodcutting")
        self.assertIsNone(fishing_tuning)
        self.assertIsNotNone(woodcutting_tuning)

    def test_label_matching_accepts_activity_loop_tests(self) -> None:
        self.assertTrue(
            _looks_like_manual_drop_label("WoodcuttingLoopTest17", activity_key="woodcutting")
        )
        self.assertFalse(
            _looks_like_manual_drop_label("WoodcuttingLoopTest17", activity_key="fishing")
        )


if __name__ == "__main__":
    unittest.main()
