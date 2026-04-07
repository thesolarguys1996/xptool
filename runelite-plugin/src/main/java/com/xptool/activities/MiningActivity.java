package com.xptool.activities;

import com.xptool.models.Snapshot;
import com.xptool.systems.DropSystem;
import com.xptool.systems.WalkSystem;

public final class MiningActivity implements Activity {
    private final DropSystem dropSystem;
    private final WalkSystem walkSystem;

    public MiningActivity(DropSystem dropSystem, WalkSystem walkSystem) {
        this.dropSystem = dropSystem;
        this.walkSystem = walkSystem;
    }

    @Override
    public String name() {
        return "mining";
    }

    @Override
    public boolean run(Snapshot snapshot) {
        if (dropSystem.shouldRun(snapshot) && dropSystem.run(snapshot)) {
            return true;
        }
        return walkSystem.shouldRun(snapshot) && walkSystem.run(snapshot);
    }
}
