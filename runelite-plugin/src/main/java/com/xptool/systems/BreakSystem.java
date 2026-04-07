package com.xptool.systems;

import com.xptool.models.Snapshot;
import com.xptool.motor.BaseMotorEngine;

public final class BreakSystem implements RuntimeSystem {
    private final BaseMotorEngine motorEngine;
    private final boolean enabled;
    private long breakUntilMs = 0L;

    public BreakSystem(BaseMotorEngine motorEngine) {
        this.motorEngine = motorEngine;
        this.enabled = Boolean.parseBoolean(System.getProperty("xptool.breakSystemEnabled", "false"));
    }

    @Override
    public String name() {
        return "BreakSystem";
    }

    @Override
    public boolean shouldRun(Snapshot snapshot) {
        if (!enabled || snapshot == null || !snapshot.loggedIn) {
            return false;
        }
        return System.currentTimeMillis() < breakUntilMs;
    }

    @Override
    public boolean run(Snapshot snapshot) {
        if (!shouldRun(snapshot)) {
            return false;
        }
        int width = motorEngine.canvasWidth();
        int height = motorEngine.canvasHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        int x = Math.max(8, (int) Math.round(width * 0.14));
        int y = Math.max(8, (int) Math.round(height * 0.12));
        return motorEngine.moveMouse(x, y);
    }

    public void requestBreak(long durationMs) {
        long now = System.currentTimeMillis();
        breakUntilMs = now + Math.max(0L, durationMs);
    }
}

