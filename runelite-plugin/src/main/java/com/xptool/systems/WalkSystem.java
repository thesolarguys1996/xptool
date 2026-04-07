package com.xptool.systems;

import com.xptool.models.Snapshot;
import com.xptool.motor.BaseMotorEngine;

public final class WalkSystem implements RuntimeSystem {
    private final BaseMotorEngine motorEngine;

    public WalkSystem(BaseMotorEngine motorEngine) {
        this.motorEngine = motorEngine;
    }

    @Override
    public String name() {
        return "WalkSystem";
    }

    @Override
    public boolean shouldRun(Snapshot snapshot) {
        return snapshot != null && snapshot.loggedIn && snapshot.playerPosition != null;
    }

    @Override
    public boolean run(Snapshot snapshot) {
        int width = motorEngine.canvasWidth();
        int height = motorEngine.canvasHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        int x = Math.max(8, (int) Math.round(width * 0.52));
        int y = Math.max(8, (int) Math.round(height * 0.48));
        return motorEngine.moveMouse(x, y);
    }
}

