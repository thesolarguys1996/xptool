package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.core.motor.MotorDispatchResult;
import com.xptool.core.motor.MotorDispatchStatus;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

class WoodcuttingCommandServiceTest {
    @Test
    void reusesInitialPlayerAreaAnchorWhenCoordinatesAreOmitted() {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);

        TileObject primary = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        host.lockedNormalTreeTarget = Optional.of(primary);
        host.nearestNormalTreeTarget = Optional.of(primary);
        host.setHoverPoint(1001, new Point(420, 320));

        JsonObject payload = new JsonObject();
        payload.addProperty("targetCategory", "NORMAL");
        payload.addProperty("targetMaxDistance", 6);
        host.lastDropSweepSessionEndedAtMs = System.currentTimeMillis() - 100L;

        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        RuntimeDecision first = service.executeChopNearestTree(payload, MotionProfile.GENERIC_INTERACT);
        assertTrue(first.isAccepted());
        assertEquals(3200, host.lastBoundaryX);
        assertEquals(3200, host.lastBoundaryY);
        assertEquals(6, host.lastBoundaryRadius);

        host.localWorldPoint = new WorldPoint(3210, 3210, 0);
        RuntimeDecision second = service.executeChopNearestTree(payload, MotionProfile.GENERIC_INTERACT);
        assertTrue(second.isAccepted());
        assertEquals(3200, host.lastBoundaryX);
        assertEquals(3200, host.lastBoundaryY);
        assertEquals(6, host.lastBoundaryRadius);
    }

    @Test
    void prefersAlternateTreeBeforeLockedWhenSameTargetCooldownActive() throws Exception {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);
        long now = System.currentTimeMillis();

        TileObject primary = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        TileObject alternate = tileObjectProxy(1002, new WorldPoint(3203, 3201, 0));
        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        host.lockedNormalTreeTarget = Optional.of(primary);
        host.nearestNormalTreeTarget = Optional.of(alternate);
        host.lastDispatchWorldPoint = primary.getWorldLocation();
        host.lastDispatchAtMs = now - 300L;
        host.setHoverPoint(1001, new Point(420, 320));
        host.setHoverPoint(1002, new Point(440, 320));
        primeNoticeDelayController(service, host.lastDispatchAtMs);

        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("woodcut_left_click_dispatched", decision.getReason());
        assertEquals(1002, host.lastDispatchTargetId);
        assertFalse(host.lastAcceptedDetails.get("reroutedFromSameTargetCooldown").getAsBoolean());
    }

    @Test
    void defersWhenSameTargetCooldownActiveAndNoAlternateTreeExists() throws Exception {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);
        long now = System.currentTimeMillis();

        TileObject primary = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        host.lockedNormalTreeTarget = Optional.of(primary);
        host.nearestNormalTreeTarget = Optional.of(primary);
        host.lastDispatchWorldPoint = primary.getWorldLocation();
        host.lastDispatchAtMs = now - 300L;
        host.setHoverPoint(1001, new Point(420, 320));
        primeNoticeDelayController(service, host.lastDispatchAtMs);

        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("woodcut_same_target_reclick_cooldown", decision.getReason());
        assertEquals(-1, host.lastDispatchTargetId);
        assertFalse(host.lastAcceptedDetails.get("reroutedFromSameTargetCooldown").getAsBoolean());
    }

    @Test
    void defersBrieflyAfterDropSweepToAvoidRoboticImmediateRetarget() {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);

        TileObject primary = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        host.lockedNormalTreeTarget = Optional.of(primary);
        host.nearestNormalTreeTarget = Optional.of(primary);
        host.setHoverPoint(1001, new Point(420, 320));
        host.lastDropSweepSessionEndedAtMs = System.currentTimeMillis() - 75L;

        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("woodcut_post_drop_reacquire_delay", decision.getReason());
        assertTrue(host.lastAcceptedDetails.get("waitMsRemaining").getAsLong() > 0L);
    }

    @Test
    void recentDropCompletionBypassesArmedPostChopNoticeDelay() throws Exception {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);
        long now = System.currentTimeMillis();

        TileObject primary = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        host.lockedNormalTreeTarget = Optional.of(primary);
        host.nearestNormalTreeTarget = Optional.of(primary);
        host.setHoverPoint(1001, new Point(420, 320));
        host.lastDispatchAtMs = now - 450L;
        host.lastDropSweepSessionEndedAtMs = now - 90L;
        primeNoticeDelayController(service, host.lastDispatchAtMs);
        armNoticeDelayWindow(service, now + 4_000L);

        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("woodcut_post_drop_reacquire_delay", decision.getReason());
        assertTrue(host.lastAcceptedDetails.get("waitMsRemaining").getAsLong() > 0L);
    }

    @Test
    void holdDecisionArmsOffscreenIdleSuppressionWindow() {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);
        host.woodcutOutcomeWaitUntilMs = System.currentTimeMillis() + 3_000L;

        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("woodcut_waiting_outcome_window", decision.getReason());
        assertTrue(service.isOffscreenIdleSuppressedNow());
        long suppressionRemainingMs = service.idleOffscreenSuppressionRemainingMs();
        assertTrue(suppressionRemainingMs > 0L);
        assertTrue(suppressionRemainingMs <= 600L);
        assertEquals(600L, host.lastAcceptedDetails.get("idleOffscreenSuppressionCapMs").getAsLong());
        assertTrue(host.lastAcceptedDetails.get("idleOffscreenSuppressionWaitMs").getAsLong() <= 600L);
    }

    @Test
    void defersOnColdStartBeforeFirstDispatch() {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);
        host.lastDispatchAtMs = 0L;
        host.lastDropSweepSessionEndedAtMs = 0L;

        TileObject primary = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        host.lockedNormalTreeTarget = Optional.of(primary);
        host.nearestNormalTreeTarget = Optional.of(primary);
        host.setHoverPoint(1001, new Point(420, 320));

        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("woodcut_cold_start_hold_window", decision.getReason());
        assertTrue(host.lastAcceptedDetails.get("waitMsRemaining").getAsLong() > 0L);
    }

    @Test
    void dispatchDecisionIncludesMovementCauseTelemetry() throws Exception {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);
        long now = System.currentTimeMillis();

        TileObject primary = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        host.lockedNormalTreeTarget = Optional.of(primary);
        host.nearestNormalTreeTarget = Optional.of(primary);
        host.lastDispatchAtMs = now - 6_000L;
        host.setHoverPoint(1001, new Point(420, 320));
        primeNoticeDelayController(service, host.lastDispatchAtMs);

        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("woodcut_left_click_dispatched", decision.getReason());
        assertTrue(host.lastAcceptedDetails.has("movementCause"));
        assertNotNull(host.lastAcceptedDetails.get("movementCause").getAsString());
        assertFalse(host.lastAcceptedDetails.get("movementCause").getAsString().isBlank());
    }

    @Test
    void defersRapidTargetPingPongToReduceReacquireThrash() throws Exception {
        TestHost host = new TestHost();
        WoodcuttingCommandService service = new WoodcuttingCommandService(host);
        long now = System.currentTimeMillis();

        TileObject first = tileObjectProxy(1001, new WorldPoint(3201, 3201, 0));
        TileObject second = tileObjectProxy(1002, new WorldPoint(3202, 3201, 0));
        host.localWorldPoint = new WorldPoint(3200, 3200, 0);
        host.setHoverPoint(1001, new Point(420, 320));
        host.setHoverPoint(1002, new Point(440, 320));
        host.lastDispatchAtMs = now - 300L;
        host.lastDispatchWorldPoint = second.getWorldLocation();
        primeNoticeDelayController(service, host.lastDispatchAtMs);

        Field recentField = WoodcuttingCommandService.class.getDeclaredField("localRecentDispatchWorldPoint");
        recentField.setAccessible(true);
        recentField.set(service, second.getWorldLocation());
        Field priorField = WoodcuttingCommandService.class.getDeclaredField("localPriorDispatchWorldPoint");
        priorField.setAccessible(true);
        priorField.set(service, first.getWorldLocation());
        Field recentAtField = WoodcuttingCommandService.class.getDeclaredField("localRecentDispatchAtMs");
        recentAtField.setAccessible(true);
        recentAtField.setLong(service, now - 300L);

        host.lockedNormalTreeTarget = Optional.of(first);
        host.nearestNormalTreeTarget = Optional.of(first);
        RuntimeDecision decision = service.executeChopNearestTree(new JsonObject(), MotionProfile.GENERIC_INTERACT);
        assertTrue(decision.isAccepted());
        assertEquals("woodcut_target_ping_pong_cooldown", decision.getReason());
    }

    private static void primeNoticeDelayController(WoodcuttingCommandService service, long observedDispatchAtMs) throws Exception {
        Field controllerField = WoodcuttingCommandService.class.getDeclaredField("woodcutNoticeDelayController");
        controllerField.setAccessible(true);
        Object controller = controllerField.get(service);
        Field lastObservedDispatchField = controller.getClass().getDeclaredField("lastObservedDispatchAtMs");
        lastObservedDispatchField.setAccessible(true);
        lastObservedDispatchField.setLong(controller, observedDispatchAtMs);
    }

    private static void armNoticeDelayWindow(WoodcuttingCommandService service, long noticeDelayUntilMs) throws Exception {
        Field controllerField = WoodcuttingCommandService.class.getDeclaredField("woodcutNoticeDelayController");
        controllerField.setAccessible(true);
        Object controller = controllerField.get(service);
        Field noticeDelayUntilField = controller.getClass().getDeclaredField("noticeDelayUntilMs");
        noticeDelayUntilField.setAccessible(true);
        noticeDelayUntilField.setLong(controller, noticeDelayUntilMs);
    }

    private static TileObject tileObjectProxy(int id, WorldPoint worldPoint) {
        return (TileObject) Proxy.newProxyInstance(
            TileObject.class.getClassLoader(),
            new Class<?>[] {TileObject.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("getId".equals(name)) {
                    return id;
                }
                if ("getWorldLocation".equals(name)) {
                    return worldPoint;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == null || !returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0f;
        }
        if (double.class.equals(returnType)) {
            return 0d;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return null;
    }

    private static final class TestHost implements WoodcuttingCommandService.Host {
        private final Map<Integer, Point> hoverPointsById = new HashMap<>();
        private WorldPoint localWorldPoint = null;
        private WorldPoint lastDispatchWorldPoint = null;
        private long lastDispatchAtMs = 0L;
        private long woodcutOutcomeWaitUntilMs = 0L;
        private long lastDropSweepSessionEndedAtMs = 0L;
        private Optional<TileObject> lockedNormalTreeTarget = Optional.empty();
        private Optional<TileObject> nearestNormalTreeTarget = Optional.empty();
        private int lastDispatchTargetId = -1;
        private int lastBoundaryX = -1;
        private int lastBoundaryY = -1;
        private int lastBoundaryRadius = -1;
        private JsonObject lastAcceptedDetails = new JsonObject();

        private void setHoverPoint(int objectId, Point point) {
            hoverPointsById.put(objectId, point == null ? null : new Point(point));
        }

        @Override
        public boolean isDropSweepSessionActive() {
            return false;
        }

        @Override
        public void endDropSweepSession() {
            // No-op.
        }

        @Override
        public long lastDropSweepSessionEndedAtMs() {
            return lastDropSweepSessionEndedAtMs;
        }

        @Override
        public void extendWoodcutRetryWindow() {
            // No-op.
        }

        @Override
        public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
            return new ClickMotionSettings(0.0, 4L, 6L);
        }

        @Override
        public int currentPlayerAnimation() {
            return -1;
        }

        @Override
        public WorldPoint localPlayerWorldPoint() {
            return localWorldPoint;
        }

        @Override
        public void clearWoodcutOutcomeWaitWindow() {
            // No-op.
        }

        @Override
        public void clearWoodcutTargetAttempt() {
            // No-op.
        }

        @Override
        public void clearWoodcutDispatchAttempt() {
            // No-op.
        }

        @Override
        public long woodcutOutcomeWaitUntilMs() {
            return woodcutOutcomeWaitUntilMs;
        }

        @Override
        public WorldPoint woodcutLastAttemptWorldPoint() {
            return null;
        }

        @Override
        public long woodcutApproachWaitUntilMs() {
            return 0L;
        }

        @Override
        public WorldPoint woodcutLastDispatchWorldPoint() {
            return lastDispatchWorldPoint;
        }

        @Override
        public long woodcutLastDispatchAtMs() {
            return lastDispatchAtMs;
        }

        @Override
        public long woodcutSameTargetReclickCooldownMs() {
            return 5_000L;
        }

        @Override
        public Optional<TileObject> resolveLockedOakTreeTarget() {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveNearestOakTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance) {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveLockedWillowTreeTarget() {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveNearestWillowTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance) {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveLockedSelectedTreeTarget() {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolvePreferredSelectedTreeTarget() {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveNearestSelectedTreeTarget() {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveNearestTreeTargetInArea(int targetWorldX, int targetWorldY, int targetMaxDistance) {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveLockedNormalTreeTarget() {
            return lockedNormalTreeTarget;
        }

        @Override
        public Optional<TileObject> resolveNearestNormalTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance) {
            return nearestNormalTreeTarget;
        }

        @Override
        public void lockWoodcutTarget(TileObject targetObject) {
            // No-op.
        }

        @Override
        public void clearWoodcutInteractionWindows() {
            // No-op.
        }

        @Override
        public int selectedWoodcutTargetCount() {
            return 0;
        }

        @Override
        public Point resolveWoodcutHoverPoint(TileObject targetObject) {
            if (targetObject == null) {
                return null;
            }
            Point point = hoverPointsById.get(targetObject.getId());
            return point == null ? null : new Point(point);
        }

        @Override
        public boolean isUsableCanvasPoint(Point point) {
            return point != null && point.x >= 0 && point.y >= 0;
        }

        @Override
        public void clearWoodcutTargetLock() {
            // No-op.
        }

        @Override
        public void clearWoodcutHoverPoint() {
            // No-op.
        }

        @Override
        public void updateWoodcutBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance) {
            lastBoundaryX = targetWorldX;
            lastBoundaryY = targetWorldY;
            lastBoundaryRadius = targetMaxDistance;
        }

        @Override
        public void clearWoodcutBoundary() {
            lastBoundaryX = -1;
            lastBoundaryY = -1;
            lastBoundaryRadius = -1;
        }

        @Override
        public void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point) {
            // No-op.
        }

        @Override
        public MotorDispatchResult dispatchWoodcutMoveAndClick(
            Point canvasPoint,
            ClickMotionSettings motion,
            TileObject targetObject
        ) {
            return new MotorDispatchResult(1L, MotorDispatchStatus.COMPLETE, "complete");
        }

        @Override
        public void noteInteractionActivityNow() {
            // No-op.
        }

        @Override
        public void noteWoodcutTargetAttempt(TileObject targetObject) {
            // No-op.
        }

        @Override
        public void noteWoodcutDispatchAttempt(TileObject targetObject, long now) {
            lastDispatchTargetId = targetObject == null ? -1 : targetObject.getId();
            lastDispatchWorldPoint = targetObject == null ? null : targetObject.getWorldLocation();
            lastDispatchAtMs = now;
        }

        @Override
        public void beginWoodcutOutcomeWaitWindow() {
            // No-op.
        }

        @Override
        public void incrementClicksDispatched() {
            // No-op.
        }

        @Override
        public com.xptool.core.runtime.FatigueSnapshot fatigueSnapshot() {
            return com.xptool.core.runtime.FatigueSnapshot.neutral();
        }

        @Override
        public JsonObject details(Object... kvPairs) {
            JsonObject out = new JsonObject();
            if (kvPairs == null) {
                return out;
            }
            int count = kvPairs.length - (kvPairs.length % 2);
            for (int i = 0; i < count; i += 2) {
                String key = kvPairs[i] == null ? "" : String.valueOf(kvPairs[i]);
                Object value = kvPairs[i + 1];
                if (key.isBlank()) {
                    continue;
                }
                if (value == null) {
                    out.addProperty(key, "");
                } else if (value instanceof Number) {
                    out.addProperty(key, (Number) value);
                } else if (value instanceof Boolean) {
                    out.addProperty(key, (Boolean) value);
                } else {
                    out.addProperty(key, String.valueOf(value));
                }
            }
            return out;
        }

        @Override
        public String safeString(String value) {
            return value == null ? "" : value;
        }

        @Override
        public RuntimeDecision accept(String reason, JsonObject details) {
            lastAcceptedDetails = details == null ? new JsonObject() : details.deepCopy();
            return RuntimeDecision.accept(reason, details);
        }

        @Override
        public RuntimeDecision reject(String reason) {
            return RuntimeDecision.reject(reason);
        }
    }
}
