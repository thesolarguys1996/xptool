package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Point;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

class InteractionAnchorResolverServiceTest {
    @Test
    void rememberInteractionAnchorForClickboxUsesClickboxCenterWhenAvailable() {
        TestHost host = new TestHost();
        InteractionAnchorResolverService service = new InteractionAnchorResolverService(host);

        service.rememberInteractionAnchorForClickbox(new Rectangle(10, 20, 8, 6), new Point(1, 1));

        assertNotNull(host.lastAnchorCenter);
        assertEquals(new Point(14, 23), host.lastAnchorCenter);
        assertNotNull(host.lastAnchorBounds);
        assertEquals(new Rectangle(10, 20, 8, 6), host.lastAnchorBounds);
    }

    @Test
    void rememberInteractionAnchorForClickboxFallsBackToProvidedPointWhenNoClickbox() {
        TestHost host = new TestHost();
        InteractionAnchorResolverService service = new InteractionAnchorResolverService(host);
        Point fallback = new Point(5, 7);

        service.rememberInteractionAnchorForClickbox(null, fallback);

        assertEquals(fallback, host.lastAnchorCenter);
        assertNull(host.lastAnchorBounds);
    }

    @Test
    void rememberInteractionAnchorForTileObjectWithNullTargetFallsBack() {
        TestHost host = new TestHost();
        InteractionAnchorResolverService service = new InteractionAnchorResolverService(host);
        Point fallback = new Point(9, 11);

        service.rememberInteractionAnchorForTileObject(null, fallback);

        assertEquals(fallback, host.lastAnchorCenter);
        assertNull(host.lastAnchorBounds);
    }

    private static final class TestHost implements InteractionAnchorResolverService.Host {
        Point lastAnchorCenter;
        Rectangle lastAnchorBounds;

        @Override
        public void rememberInteractionAnchor(Point anchorCenterCanvasPoint, Rectangle anchorBoundsCanvas) {
            lastAnchorCenter = anchorCenterCanvasPoint == null ? null : new Point(anchorCenterCanvasPoint);
            lastAnchorBounds = anchorBoundsCanvas == null ? null : new Rectangle(anchorBoundsCanvas);
        }
    }
}
