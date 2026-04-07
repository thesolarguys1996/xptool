package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActivityIdlePolicyRegistryTest {
    @Test
    void defaultsResolveKnownActivitiesToExpectedDbParityModes() {
        ActivityIdlePolicyRegistry registry = ActivityIdlePolicyRegistry.defaults();
        ActivityIdlePolicy fishing = registry.resolveForActivity(ActivityIdlePolicyRegistry.ACTIVITY_FISHING);
        ActivityIdlePolicy woodcutting = registry.resolveForActivity(ActivityIdlePolicyRegistry.ACTIVITY_WOODCUTTING);

        assertEquals(
            "DB_PARITY",
            fishing.profileKey()
        );
        assertEquals(
            FishingIdleMode.STANDARD,
            fishing.fishingIdleMode()
        );
        assertEquals(
            5,
            fishing.cadenceWindow().actedMinIntervalTicks()
        );
        assertEquals(
            14,
            fishing.cadenceWindow().actedMaxIntervalTicks()
        );
        assertEquals(
            84,
            fishing.resolveBehaviorProfile(FishingIdleMode.OFFSCREEN_BIASED).offscreenParkChancePercent()
        );
        assertEquals(
            "DB_PARITY",
            woodcutting.profileKey()
        );
        assertEquals(
            FishingIdleMode.OFFSCREEN_BIASED,
            woodcutting.fishingIdleMode()
        );
        assertEquals(
            "DB_PARITY",
            registry.resolveForActivity(ActivityIdlePolicyRegistry.ACTIVITY_MINING).profileKey()
        );
        assertEquals(
            "DB_PARITY",
            registry.resolveForActivity(ActivityIdlePolicyRegistry.ACTIVITY_COMBAT).profileKey()
        );
    }

    @Test
    void resolveForContextUsesContextMapping() {
        ActivityIdlePolicyRegistry registry = ActivityIdlePolicyRegistry.defaults();

        assertEquals(
            ActivityIdlePolicyRegistry.ACTIVITY_FISHING,
            ActivityIdlePolicyRegistry.activityKeyForContext(IdleSkillContext.FISHING)
        );
        assertEquals(
            ActivityIdlePolicyRegistry.ACTIVITY_GLOBAL,
            ActivityIdlePolicyRegistry.activityKeyForContext(IdleSkillContext.GLOBAL)
        );
        assertEquals(
            "DB_PARITY",
            registry.resolveForContext(IdleSkillContext.FISHING).profileKey()
        );
    }

    @Test
    void unknownActivityFallsBackToFallbackPolicy() {
        ActivityIdlePolicy fallback = ActivityIdlePolicy.of("DB_PARITY", FishingIdleMode.STANDARD);
        ActivityIdlePolicy fishing = ActivityIdlePolicy.of("DB_PARITY", FishingIdleMode.OFFSCREEN_BIASED);
        ActivityIdlePolicyRegistry registry = ActivityIdlePolicyRegistry.of(
            fallback,
            Map.of(ActivityIdlePolicyRegistry.ACTIVITY_FISHING, fishing)
        );

        assertSame(fishing, registry.resolveForActivity("fishing"));
        assertSame(fallback, registry.resolveForActivity("unknown_activity"));
    }

    @Test
    void normalizeActivityKeyHandlesWhitespaceAndSeparators() {
        assertEquals("store_bank", ActivityIdlePolicyRegistry.normalizeActivityKey(" Store-Bank "));
        assertEquals("woodcutting", ActivityIdlePolicyRegistry.normalizeActivityKey("WOODCUTTING"));
    }
}
