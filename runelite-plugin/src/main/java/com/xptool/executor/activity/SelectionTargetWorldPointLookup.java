package com.xptool.executor.activity;

import java.util.Optional;
import net.runelite.api.coords.WorldPoint;

@FunctionalInterface
public interface SelectionTargetWorldPointLookup {
    Optional<WorldPoint> resolve(int sceneX, int sceneY, int worldViewId);
}
