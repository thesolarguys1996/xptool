package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InteractionClickTelemetryServiceTest {
    @Test
    void noteInteractionClickSuccessTracksStateAndEmitsTelemetryAndSettleEvent() {
        TestHost host = new TestHost();
        host.activeMotorOwnerContext = ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION;
        host.settleEligible = true;
        host.mouseCanvasPoint = new Point(22, 34);

        InteractionClickTelemetryService service = new InteractionClickTelemetryService(host, true);
        service.noteInteractionClickSuccess(ExecutorMotorProfileCatalog.CLICK_TYPE_FISHING_WORLD);

        assertEquals(1, host.noteInteractionActivityNowCalls);
        assertEquals(1, host.noteMotorActionCalls);
        assertEquals(1L, service.interactionClickSerial());
        assertTrue(service.isInteractionClickFresh(5_000L));
        assertEquals(new Point(22, 34), service.lastInteractionClickCanvasPoint().orElseThrow());

        assertEquals(1, host.emittedTelemetry.size());
        JsonObject telemetry = host.emittedTelemetry.get(0);
        assertEquals(1L, telemetry.get("clickSerial").getAsLong());
        assertEquals(22, telemetry.get("canvasX").getAsInt());
        assertEquals(34, telemetry.get("canvasY").getAsInt());
        assertEquals(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION, telemetry.get("motorOwner").getAsString());

        assertEquals(1, host.settleClickEvents.size());
        InteractionClickEvent settle = host.settleClickEvents.get(0);
        assertEquals(1L, settle.getClickSerial());
        assertEquals(host.executorTick, settle.getTick());
        assertTrue(settle.getClickedAtMs() > 0L);
        assertEquals(ExecutorMotorProfileCatalog.CLICK_TYPE_FISHING_WORLD, settle.getClickType());
        assertEquals(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION, settle.getOwner());
        assertNotNull(settle.getClickCanvasPoint());
        assertEquals(new Point(22, 34), settle.getClickCanvasPoint());
        assertNotNull(settle.getAnchorCanvasPoint());
        assertEquals(new Point(22, 34), settle.getAnchorCanvasPoint());
        assertNotNull(settle.getAnchorBoundsCanvas());
        assertTrue(settle.getAnchorBoundsCanvas().contains(settle.getClickCanvasPoint()));
        assertEquals(host.motorActionSerial, settle.getMotorActionSerialAtClick());
    }

    @Test
    void dropSweepTelemetryUsesThrottleUnlessPixelRepeats() {
        TestHost host = new TestHost();
        host.activeMotorOwnerContext = ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP;
        host.settleEligible = false;
        InteractionClickTelemetryService service = new InteractionClickTelemetryService(host, true);

        host.mouseCanvasPoint = new Point(10, 10);
        service.noteInteractionClickSuccess(ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD);
        host.mouseCanvasPoint = new Point(11, 10);
        service.noteInteractionClickSuccess(ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD);
        host.mouseCanvasPoint = new Point(12, 10);
        service.noteInteractionClickSuccess(ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD);

        assertEquals(1, host.emittedTelemetry.size());
        assertEquals(3L, host.emittedTelemetry.get(0).get("clickSerial").getAsLong());

        host.mouseCanvasPoint = new Point(12, 10);
        service.noteInteractionClickSuccess(ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD);
        assertEquals(2, host.emittedTelemetry.size());
        assertEquals(4L, host.emittedTelemetry.get(1).get("clickSerial").getAsLong());
    }

    @Test
    void rememberInteractionAnchorMaintainsUsableAnchorState() {
        TestHost host = new TestHost();
        InteractionClickTelemetryService service = new InteractionClickTelemetryService(host, false);

        service.rememberInteractionAnchor(new Point(0, 0), new Rectangle(0, 0, 12, 12));
        Rectangle bounds = service.lastInteractionAnchorBoundsCanvasOrNull();
        assertNotNull(bounds);
        assertTrue(bounds.contains(new Point(1, 1)));

        service.rememberInteractionAnchor(new Point(4, 5), null);
        Point center = service.lastInteractionAnchorCenterCanvasPointOrNull();
        assertEquals(new Point(4, 5), center);

        assertFalse(service.lastInteractionClickCanvasPoint().isPresent());
    }

    private static final class TestHost implements InteractionClickTelemetryService.Host {
        Point mouseCanvasPoint = new Point(10, 10);
        int canvasWidth = 400;
        int canvasHeight = 300;
        String activeMotorOwnerContext = "";
        boolean settleEligible = false;
        boolean targetVariationEnabled = true;
        int executorTick = 42;
        long motorActionSerial = 9L;
        int noteInteractionActivityNowCalls = 0;
        int noteMotorActionCalls = 0;
        List<JsonObject> emittedTelemetry = new ArrayList<>();
        List<InteractionClickEvent> settleClickEvents = new ArrayList<>();

        @Override
        public boolean isUsableCanvasPoint(Point point) {
            if (point == null) {
                return false;
            }
            return point.x >= 1 && point.x < (canvasWidth - 1) && point.y >= 1 && point.y < (canvasHeight - 1);
        }

        @Override
        public Point currentMouseCanvasPoint() {
            return mouseCanvasPoint == null ? null : new Point(mouseCanvasPoint);
        }

        @Override
        public int canvasWidth() {
            return canvasWidth;
        }

        @Override
        public int canvasHeight() {
            return canvasHeight;
        }

        @Override
        public String normalizedMotorOwnerName(String owner) {
            return ExecutorValueParsers.safeString(owner).trim().toLowerCase();
        }

        @Override
        public String activeMotorOwnerContext() {
            return activeMotorOwnerContext;
        }

        @Override
        public boolean isSettleEligibleClickType(String clickType) {
            return settleEligible;
        }

        @Override
        public boolean targetVariationEnabled() {
            return targetVariationEnabled;
        }

        @Override
        public int currentExecutorTick() {
            return executorTick;
        }

        @Override
        public long motorActionSerial() {
            return motorActionSerial;
        }

        @Override
        public void noteInteractionActivityNow() {
            noteInteractionActivityNowCalls++;
        }

        @Override
        public void noteMotorAction() {
            noteMotorActionCalls++;
        }

        @Override
        public void emitInteractionClickTelemetry(JsonObject telemetry) {
            emittedTelemetry.add(telemetry.deepCopy());
        }

        @Override
        public void onSettleEligibleInteractionClick(InteractionClickEvent clickEvent) {
            settleClickEvents.add(clickEvent);
        }
    }
}
