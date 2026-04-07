package com.xptool.executor.activity;

import java.util.Optional;
import java.util.function.IntFunction;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

public final class SelectionWorldPointResolver implements SelectionTargetWorldPointLookup {
    private final IntFunction<WorldView> worldViewResolver;

    public SelectionWorldPointResolver(IntFunction<WorldView> worldViewResolver) {
        this.worldViewResolver = worldViewResolver;
    }

    @Override
    public Optional<WorldPoint> resolve(int sceneX, int sceneY, int worldViewId) {
        if (sceneX < 0 || sceneY < 0) {
            return Optional.empty();
        }
        WorldView view = worldViewResolver.apply(worldViewId);
        if (view == null) {
            return Optional.empty();
        }
        WorldPoint wp = WorldPoint.fromScene(view, sceneX, sceneY, view.getPlane());
        if (wp == null) {
            return Optional.empty();
        }
        return Optional.of(wp);
    }
}
