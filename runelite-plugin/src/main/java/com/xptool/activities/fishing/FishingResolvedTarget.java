package com.xptool.activities.fishing;

import java.awt.Point;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

public final class FishingResolvedTarget {
    public final NPC npc;
    public final Point canvasPoint;
    public final int targetId;
    public final int targetIndex;
    public final WorldPoint targetWorldPoint;
    public final int localDistanceToTarget;
    public final boolean sameSpotReacquired;
    public final boolean sameDispatchTarget;
    public final String selectionMode;
    public final int selectionPoolSize;
    public final String selectionPolicy;

    public FishingResolvedTarget(
        NPC npc,
        Point canvasPoint,
        int targetId,
        int targetIndex,
        WorldPoint targetWorldPoint,
        int localDistanceToTarget,
        boolean sameSpotReacquired,
        boolean sameDispatchTarget
    ) {
        this(
            npc,
            canvasPoint,
            targetId,
            targetIndex,
            targetWorldPoint,
            localDistanceToTarget,
            sameSpotReacquired,
            sameDispatchTarget,
            "default",
            0,
            "default"
        );
    }

    public FishingResolvedTarget(
        NPC npc,
        Point canvasPoint,
        int targetId,
        int targetIndex,
        WorldPoint targetWorldPoint,
        int localDistanceToTarget,
        boolean sameSpotReacquired,
        boolean sameDispatchTarget,
        String selectionMode
    ) {
        this(
            npc,
            canvasPoint,
            targetId,
            targetIndex,
            targetWorldPoint,
            localDistanceToTarget,
            sameSpotReacquired,
            sameDispatchTarget,
            selectionMode,
            0,
            "default"
        );
    }

    public FishingResolvedTarget(
        NPC npc,
        Point canvasPoint,
        int targetId,
        int targetIndex,
        WorldPoint targetWorldPoint,
        int localDistanceToTarget,
        boolean sameSpotReacquired,
        boolean sameDispatchTarget,
        String selectionMode,
        int selectionPoolSize,
        String selectionPolicy
    ) {
        this.npc = npc;
        this.canvasPoint = canvasPoint;
        this.targetId = targetId;
        this.targetIndex = targetIndex;
        this.targetWorldPoint = targetWorldPoint;
        this.localDistanceToTarget = localDistanceToTarget;
        this.sameSpotReacquired = sameSpotReacquired;
        this.sameDispatchTarget = sameDispatchTarget;
        this.selectionMode = selectionMode == null ? "default" : selectionMode;
        this.selectionPoolSize = Math.max(0, selectionPoolSize);
        this.selectionPolicy = selectionPolicy == null ? "default" : selectionPolicy;
    }

    public FishingResolvedTarget withSelectionMode(String mode) {
        return new FishingResolvedTarget(
            npc,
            canvasPoint == null ? null : new Point(canvasPoint),
            targetId,
            targetIndex,
            targetWorldPoint,
            localDistanceToTarget,
            sameSpotReacquired,
            sameDispatchTarget,
            mode,
            selectionPoolSize,
            selectionPolicy
        );
    }

    public FishingResolvedTarget withSelectionMetadata(int poolSize, String policy) {
        return new FishingResolvedTarget(
            npc,
            canvasPoint == null ? null : new Point(canvasPoint),
            targetId,
            targetIndex,
            targetWorldPoint,
            localDistanceToTarget,
            sameSpotReacquired,
            sameDispatchTarget,
            selectionMode,
            poolSize,
            policy
        );
    }
}
