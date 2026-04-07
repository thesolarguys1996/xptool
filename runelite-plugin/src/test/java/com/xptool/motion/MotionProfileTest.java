package com.xptool.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import org.junit.jupiter.api.Test;

class MotionProfileTest {
    @Test
    void resolveClickSettingsParsesFishingMoveEasingFields() {
        JsonObject payload = new JsonObject();
        payload.addProperty("moveAccelPercent", 44);
        payload.addProperty("moveDecelPercent", 70);
        payload.addProperty("terminalSlowdownRadiusPx", 88);

        ClickMotionSettings settings = MotionProfile.FISHING.resolveClickSettings(payload);

        assertEquals(44, settings.moveAccelPercent);
        assertEquals(70, settings.moveDecelPercent);
        assertEquals(88, settings.terminalSlowdownRadiusPx);
    }

    @Test
    void resolveClickSettingsClampsFishingMoveEasingFields() {
        JsonObject payload = new JsonObject();
        payload.addProperty("moveAccelPercent", 140);
        payload.addProperty("moveDecelPercent", -8);
        payload.addProperty("terminalSlowdownRadiusPx", 900);

        ClickMotionSettings settings = MotionProfile.FISHING.resolveClickSettings(payload);

        assertEquals(100, settings.moveAccelPercent);
        assertEquals(0, settings.moveDecelPercent);
        assertEquals(260, settings.terminalSlowdownRadiusPx);
    }
}

