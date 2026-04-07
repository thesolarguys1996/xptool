package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

class InteractionClickEventTest {
    @Test
    void constructorDefensivelyCopiesPointAndBoundsFields() {
        Point clickPoint = new Point(10, 20);
        Point anchorPoint = new Point(11, 21);
        Rectangle anchorBounds = new Rectangle(5, 6, 20, 30);
        InteractionClickEvent event = new InteractionClickEvent(
            7L,
            44,
            1234L,
            " interaction ",
            ExecutorMotorProfileCatalog.CLICK_TYPE_FISHING_WORLD,
            clickPoint,
            anchorPoint,
            anchorBounds,
            99L
        );

        clickPoint.x = 500;
        anchorPoint.y = 700;
        anchorBounds.width = 1;

        assertEquals(new Point(10, 20), event.getClickCanvasPoint());
        assertEquals(new Point(11, 21), event.getAnchorCanvasPoint());
        assertEquals(new Rectangle(5, 6, 20, 30), event.getAnchorBoundsCanvas());
    }

    @Test
    void settleEligibilityAndNullStringSafetyMatchCurrentContract() {
        InteractionClickEvent settleEligible = new InteractionClickEvent(
            1L,
            2,
            3L,
            null,
            ExecutorMotorProfileCatalog.CLICK_TYPE_FISHING_WORLD,
            null,
            null,
            null,
            4L
        );
        assertTrue(settleEligible.isSettleEligible());
        assertEquals("", settleEligible.getOwner());
        assertEquals(ExecutorMotorProfileCatalog.CLICK_TYPE_FISHING_WORLD, settleEligible.getClickType());
        assertNull(settleEligible.getClickCanvasPoint());
        assertNull(settleEligible.getAnchorCanvasPoint());
        assertNull(settleEligible.getAnchorBoundsCanvas());
        assertNotNull(settleEligible);
    }
}
