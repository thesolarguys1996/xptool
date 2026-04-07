package com.xptool.executor;

import java.awt.Point;
import java.util.function.Function;
import net.runelite.api.TileObject;

final class SceneInteractionPointService {
    @FunctionalInterface
    interface TileObjectPointVariator {
        Point vary(TileObject tileObject, Point point, String profileKey, int minJitterPx, int maxJitterPx);
    }

    private final Function<TileObject, Point> resolveTileObjectClickPoint;
    private final Function<net.runelite.api.Point, Point> toAwtPoint;
    private final TileObjectPointVariator varyForTileObject;
    private final int sceneObjectJitterMinPx;
    private final int sceneObjectJitterMaxPx;
    private final int groundItemJitterMinPx;
    private final int groundItemJitterMaxPx;

    SceneInteractionPointService(
        Function<TileObject, Point> resolveTileObjectClickPoint,
        Function<net.runelite.api.Point, Point> toAwtPoint,
        TileObjectPointVariator varyForTileObject,
        int sceneObjectJitterMinPx,
        int sceneObjectJitterMaxPx,
        int groundItemJitterMinPx,
        int groundItemJitterMaxPx
    ) {
        this.resolveTileObjectClickPoint = resolveTileObjectClickPoint;
        this.toAwtPoint = toAwtPoint;
        this.varyForTileObject = varyForTileObject;
        this.sceneObjectJitterMinPx = Math.max(0, sceneObjectJitterMinPx);
        this.sceneObjectJitterMaxPx = Math.max(this.sceneObjectJitterMinPx, sceneObjectJitterMaxPx);
        this.groundItemJitterMinPx = Math.max(0, groundItemJitterMinPx);
        this.groundItemJitterMaxPx = Math.max(this.groundItemJitterMinPx, groundItemJitterMaxPx);
    }

    Point resolveSceneObjectClickPoint(TileObject targetObject) {
        if (targetObject == null) {
            return null;
        }
        Point point = resolveTileObjectClickPoint.apply(targetObject);
        if (point == null) {
            point = toAwtPoint.apply(targetObject.getCanvasLocation());
        }
        return varyForTileObject.vary(
            targetObject,
            point,
            "scene_object",
            sceneObjectJitterMinPx,
            sceneObjectJitterMaxPx
        );
    }

    Point resolveGroundItemClickPoint(GroundItemRef groundItem) {
        if (groundItem == null) {
            return null;
        }
        Point point = null;
        TileObject clickTarget = groundItem.itemLayer;
        if (clickTarget != null) {
            point = resolveTileObjectClickPoint.apply(clickTarget);
            if (point == null) {
                point = toAwtPoint.apply(clickTarget.getCanvasLocation());
            }
        }
        if (clickTarget != null) {
            return varyForTileObject.vary(
                clickTarget,
                point,
                "ground_item",
                groundItemJitterMinPx,
                groundItemJitterMaxPx
            );
        }
        return point;
    }
}
