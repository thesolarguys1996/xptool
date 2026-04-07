package com.xptool.executor.activity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import net.runelite.api.coords.WorldPoint;

public final class WoodcuttingSelectionService {
    private final BiPredicate<WorldPoint, WorldPoint> worldPointMatcher;
    private final Set<WorldPoint> selectedTargets = new LinkedHashSet<>();

    public WoodcuttingSelectionService(BiPredicate<WorldPoint, WorldPoint> worldPointMatcher) {
        this.worldPointMatcher = worldPointMatcher;
    }

    public int size() {
        return selectedTargets.size();
    }

    public boolean isEmpty() {
        return selectedTargets.isEmpty();
    }

    public boolean hasTargetNear(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return false;
        }
        for (WorldPoint candidate : selectedTargets) {
            if (candidate != null && worldPointMatcher.test(candidate, worldPoint)) {
                return true;
            }
        }
        return false;
    }

    public void add(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return;
        }
        selectedTargets.add(worldPoint);
    }

    public void removeTargetsNear(WorldPoint worldPoint) {
        if (worldPoint == null || selectedTargets.isEmpty()) {
            return;
        }
        selectedTargets.removeIf(candidate -> candidate != null && worldPointMatcher.test(candidate, worldPoint));
    }

    public WorldPoint latestSelectedWorldPoint() {
        WorldPoint latest = null;
        for (WorldPoint candidate : selectedTargets) {
            if (candidate != null) {
                latest = candidate;
            }
        }
        return latest;
    }
}
