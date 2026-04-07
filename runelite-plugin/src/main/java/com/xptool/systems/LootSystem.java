package com.xptool.systems;

import com.xptool.models.Snapshot;
import com.xptool.models.Snapshot.NearbyNpc;
import com.xptool.motor.BaseMotorEngine;

public final class LootSystem implements RuntimeSystem {
    private final BaseMotorEngine motorEngine;

    public LootSystem(BaseMotorEngine motorEngine) {
        this.motorEngine = motorEngine;
    }

    @Override
    public String name() {
        return "LootSystem";
    }

    @Override
    public boolean shouldRun(Snapshot snapshot) {
        if (snapshot == null || !snapshot.loggedIn) {
            return false;
        }
        for (NearbyNpc npc : snapshot.npcs) {
            if (npc == null) {
                continue;
            }
            if (npc.distance >= 0 && npc.distance <= 4) {
                return true;
            }
        }
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
        int x = Math.max(8, (int) Math.round(width * 0.50));
        int y = Math.max(8, (int) Math.round(height * 0.62));
        return motorEngine.moveMouse(x, y);
    }
}

