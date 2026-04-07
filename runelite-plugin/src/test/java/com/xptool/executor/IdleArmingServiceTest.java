package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import org.junit.jupiter.api.Test;

class IdleArmingServiceTest {
    @Test
    void armAndDisarmAreTrackedPerActivity() {
        IdleArmingService service = new IdleArmingService();

        service.armActivity("fishing", FishingIdleMode.STANDARD, "command_queue");
        service.armActivity("mining", FishingIdleMode.STANDARD, "command_queue");

        assertTrue(service.isArmedForActivity("fishing"));
        assertTrue(service.isArmedForActivity("mining"));
        assertEquals(2, service.armedActivityCount());
        assertEquals("command_queue", service.armSourceForActivity("fishing"));
        assertFalse(service.isOffscreenArmedForActivity("fishing"));

        service.disarmActivity("mining");
        assertFalse(service.isArmedForActivity("mining"));
        assertEquals(IdleArmingService.SOURCE_NONE, service.armSourceForActivity("mining"));
        assertEquals(1, service.armedActivityCount());
    }

    @Test
    void offscreenModeRequiresOffscreenArmState() {
        IdleArmingService service = new IdleArmingService();

        service.armActivity("fishing", FishingIdleMode.OFFSCREEN_BIASED, "fishing_mode_override");
        assertTrue(service.isArmedForActivity("fishing"));
        assertTrue(service.isOffscreenArmedForActivity("fishing"));
        assertEquals("fishing_mode_override", service.offscreenArmSourceForActivity("fishing"));
        assertTrue(service.isArmedForContext(IdleSkillContext.FISHING, FishingIdleMode.OFFSCREEN_BIASED));

        service.armActivity("fishing", FishingIdleMode.STANDARD, "command_queue");
        assertTrue(service.isArmedForActivity("fishing"));
        assertFalse(service.isOffscreenArmedForActivity("fishing"));
        assertEquals(IdleArmingService.SOURCE_NONE, service.offscreenArmSourceForActivity("fishing"));
        assertFalse(service.isArmedForContext(IdleSkillContext.FISHING, FishingIdleMode.OFFSCREEN_BIASED));
    }

    @Test
    void plannerTagMappingFallsBackToGlobal() {
        assertEquals(
            ActivityIdlePolicyRegistry.ACTIVITY_FISHING,
            IdleArmingService.activityKeyFromPlannerTag("fishing")
        );
        assertEquals(
            ActivityIdlePolicyRegistry.ACTIVITY_STORE_BANK,
            IdleArmingService.activityKeyFromPlannerTag("store-bank")
        );
        assertEquals(
            ActivityIdlePolicyRegistry.ACTIVITY_GLOBAL,
            IdleArmingService.activityKeyFromPlannerTag("unknown")
        );
        assertEquals(
            IdleSkillContext.COMBAT,
            IdleArmingService.idleContextFromActivityKey("combat")
        );
        assertEquals(
            IdleSkillContext.GLOBAL,
            IdleArmingService.idleContextFromActivityKey("unknown")
        );
        assertEquals(
            IdleSkillContext.WOODCUTTING,
            IdleArmingService.idleContextFromPlannerTag("woodcutting")
        );
    }
}
