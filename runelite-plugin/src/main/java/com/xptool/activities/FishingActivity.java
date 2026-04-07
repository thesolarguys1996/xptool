package com.xptool.activities;

import com.xptool.models.Snapshot;
import com.xptool.systems.DropSystem;
import com.xptool.systems.WalkSystem;

public final class FishingActivity implements Activity {
    private final DropSystem dropSystem;
    private final WalkSystem walkSystem;

    public FishingActivity(DropSystem dropSystem, WalkSystem walkSystem) {
        this.dropSystem = dropSystem;
        this.walkSystem = walkSystem;
    }

    @Override
    public String name() {
        return "fishing";
    }

    @Override
    public boolean run(Snapshot snapshot) {
        if (dropSystem.shouldRun(snapshot) && dropSystem.run(snapshot)) {
            return true;
        }
        return walkSystem.shouldRun(snapshot) && walkSystem.run(snapshot);
    }
}
