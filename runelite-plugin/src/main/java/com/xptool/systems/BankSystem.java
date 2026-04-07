package com.xptool.systems;

import com.xptool.models.Snapshot;
import com.xptool.models.Snapshot.NearbyObject;
import com.xptool.motor.BaseMotorEngine;

public final class BankSystem implements RuntimeSystem {
    private final BaseMotorEngine motorEngine;
    private final DropSystem dropSystem;

    public BankSystem(BaseMotorEngine motorEngine, DropSystem dropSystem) {
        this.motorEngine = motorEngine;
        this.dropSystem = dropSystem;
    }

    @Override
    public String name() {
        return "BankSystem";
    }

    @Override
    public boolean shouldRun(Snapshot snapshot) {
        return snapshot != null
            && snapshot.loggedIn
            && snapshot.isInventoryFull()
            && !dropSystem.hasDropCandidate(snapshot)
            && hasNearbyBankObject(snapshot);
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
        int x = Math.max(8, (int) Math.round(width * 0.56));
        int y = Math.max(8, (int) Math.round(height * 0.48));
        return motorEngine.moveMouse(x, y);
    }

    private static boolean hasNearbyBankObject(Snapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        for (NearbyObject object : snapshot.nearbyObjects) {
            if (object == null) {
                continue;
            }
            String name = object.name == null ? "" : object.name.trim().toLowerCase();
            if (name.contains("bank") || name.contains("booth") || name.contains("chest")) {
                return true;
            }
        }
        return false;
    }
}

