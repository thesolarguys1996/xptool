package com.xptool.executor.activity;

import java.util.Optional;
import java.util.function.BiPredicate;
import net.runelite.api.coords.WorldPoint;

public final class WoodcuttingSelectionController {
    private final SelectionTargetWorldPointLookup worldPointResolver;
    private final WoodcuttingSelectionService selectionService;
    private final WoodcuttingTargetStateService targetStateService;
    private final BiPredicate<WorldPoint, WorldPoint> worldPointMatcher;
    private final Runnable clearTargetLock;

    public WoodcuttingSelectionController(
        SelectionTargetWorldPointLookup worldPointResolver,
        WoodcuttingSelectionService selectionService,
        WoodcuttingTargetStateService targetStateService,
        BiPredicate<WorldPoint, WorldPoint> worldPointMatcher,
        Runnable clearTargetLock
    ) {
        this.worldPointResolver = worldPointResolver;
        this.selectionService = selectionService;
        this.targetStateService = targetStateService;
        this.worldPointMatcher = worldPointMatcher;
        this.clearTargetLock = clearTargetLock;
    }

    public boolean isSelectedTarget(int sceneX, int sceneY, int worldViewId) {
        Optional<WorldPoint> worldPoint = worldPointResolver.resolve(sceneX, sceneY, worldViewId);
        return worldPoint.isPresent() && selectionService.hasTargetNear(worldPoint.get());
    }

    public boolean toggleSelectedTarget(int sceneX, int sceneY, int worldViewId) {
        Optional<WorldPoint> worldPoint = worldPointResolver.resolve(sceneX, sceneY, worldViewId);
        if (worldPoint.isEmpty()) {
            return false;
        }
        WorldPoint targetWorldPoint = worldPoint.get();
        if (selectionService.hasTargetNear(targetWorldPoint)) {
            selectionService.removeTargetsNear(targetWorldPoint);
            if (worldPointMatcher.test(targetStateService.lockedWorldPoint(), targetWorldPoint)) {
                clearTargetLock.run();
            }
            if (worldPointMatcher.test(targetStateService.preferredSelectedWorldPoint(), targetWorldPoint)) {
                targetStateService.setPreferredSelectedWorldPoint(selectionService.latestSelectedWorldPoint());
            }
            if (selectionService.isEmpty()) {
                targetStateService.clearPreferredSelectedWorldPoint();
            }
            return false;
        }
        selectionService.add(targetWorldPoint);
        targetStateService.lockSelectedTarget(targetWorldPoint);
        return true;
    }
}
