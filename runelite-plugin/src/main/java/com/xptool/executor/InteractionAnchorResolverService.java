package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import net.runelite.api.TileObject;

final class InteractionAnchorResolverService {
    interface Host {
        void rememberInteractionAnchor(Point anchorCenterCanvasPoint, Rectangle anchorBoundsCanvas);
    }

    private final Host host;

    InteractionAnchorResolverService(Host host) {
        this.host = host;
    }

    void rememberInteractionAnchorForTileObject(TileObject targetObject, Point fallbackCanvasPoint) {
        Shape clickbox = null;
        if (targetObject != null) {
            try {
                clickbox = targetObject.getClickbox();
            } catch (Exception ignored) {
                clickbox = null;
            }
        }
        rememberInteractionAnchorForClickbox(clickbox, fallbackCanvasPoint);
    }

    void rememberInteractionAnchorForClickbox(Shape clickbox, Point fallbackCanvasPoint) {
        Rectangle clickboxBounds = clickbox == null ? null : clickbox.getBounds();
        Point center = fallbackCanvasPoint == null ? null : new Point(fallbackCanvasPoint);
        if (clickboxBounds != null && clickboxBounds.width > 0 && clickboxBounds.height > 0) {
            center = new Point(
                (int) Math.round(clickboxBounds.getCenterX()),
                (int) Math.round(clickboxBounds.getCenterY())
            );
        }
        host.rememberInteractionAnchor(center, clickboxBounds);
    }
}
