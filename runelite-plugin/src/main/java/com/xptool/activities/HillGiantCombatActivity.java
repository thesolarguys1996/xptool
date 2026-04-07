package com.xptool.activities;

import com.xptool.models.Snapshot;
import com.xptool.systems.LootSystem;
import com.xptool.systems.WalkSystem;

public final class HillGiantCombatActivity implements Activity {
    private final LootSystem lootSystem;
    private final WalkSystem walkSystem;

    public HillGiantCombatActivity(LootSystem lootSystem, WalkSystem walkSystem) {
        this.lootSystem = lootSystem;
        this.walkSystem = walkSystem;
    }

    @Override
    public String name() {
        return "hill_giant_combat";
    }

    @Override
    public boolean run(Snapshot snapshot) {
        if (lootSystem.shouldRun(snapshot) && lootSystem.run(snapshot)) {
            return true;
        }
        return walkSystem.shouldRun(snapshot) && walkSystem.run(snapshot);
    }
}
