package com.xptool.systems;

import com.xptool.models.Snapshot;
import com.xptool.motor.BaseMotorEngine;

public final class RandomEventSystem implements RuntimeSystem {
    private final BaseMotorEngine motorEngine;

    public RandomEventSystem(BaseMotorEngine motorEngine) {
        this.motorEngine = motorEngine;
    }

    @Override
    public String name() {
        return "RandomEventSystem";
    }

    @Override
    public boolean shouldRun(Snapshot snapshot) {
        if (snapshot == null || !snapshot.loggedIn) {
            return false;
        }
        // Reserved for explicit random-event predicates.
        return false;
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
        int x = Math.max(8, (int) Math.round(width * 0.64));
        int y = Math.max(8, (int) Math.round(height * 0.46));
        return motorEngine.moveMouse(x, y);
    }
}

