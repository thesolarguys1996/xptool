package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.activities.mining.MiningCommandService;
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

class MiningCommandServiceTest {
    @Test
    void reroutesToAlternateRockWhenSameTargetCooldownActive() throws Exception {
        TestHost host = new TestHost();
        MiningCommandService service = new MiningCommandService(host);
        long now = System.currentTimeMillis();

        TileObject primary = tileObjectProxy(2001, new WorldPoint(3301, 3301, 0));
        TileObject alternate = tileObjectProxy(2002, new WorldPoint(3302, 3301, 0));
        host.lockedRockTarget = Optional.of(primary);
        host.nearestRockTargetExcluding = Optional.of(alternate);
        host.setHoverPoint(2001, new Point(500, 300));
        host.setHoverPoint(2002, new Point(520, 300));

        setField(service, "lastDispatchWorldPoint", primary.getWorldLocation());
        setField(service, "lastDispatchAtMs", now - 300L);

        RuntimeDecision decision = service.executeMineNearestRock(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("mining_left_click_dispatched", decision.getReason());
        assertEquals(2002, host.lockedTargetId);
        assertEquals(3302, host.suppressedWorldPoint.getX());
        assertEquals(3301, host.suppressedWorldPoint.getY());
        assertEquals(0, host.suppressedWorldPoint.getPlane());
        assertTrue(host.lastAcceptedDetails.get("reroutedFromSameTargetCooldown").getAsBoolean());
    }

    @Test
    void defersWhenSameTargetCooldownActiveAndNoAlternateRockExists() throws Exception {
        TestHost host = new TestHost();
        MiningCommandService service = new MiningCommandService(host);
        long now = System.currentTimeMillis();

        TileObject primary = tileObjectProxy(2001, new WorldPoint(3301, 3301, 0));
        host.lockedRockTarget = Optional.of(primary);
        host.nearestRockTargetExcluding = Optional.empty();
        host.setHoverPoint(2001, new Point(500, 300));

        setField(service, "lastDispatchWorldPoint", primary.getWorldLocation());
        setField(service, "lastDispatchAtMs", now - 300L);

        RuntimeDecision decision = service.executeMineNearestRock(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("mining_same_target_reclick_cooldown", decision.getReason());
        assertEquals(0, host.scheduleCalls);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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

    private static final class TestHost implements MiningCommandService.Host {
        private final Map<Integer, Point> hoverPointsById = new HashMap<>();
        private Optional<TileObject> lockedRockTarget = Optional.empty();
        private Optional<TileObject> nearestRockTargetExcluding = Optional.empty();
        private int lockedTargetId = -1;
        private int scheduleCalls = 0;
        private WorldPoint suppressedWorldPoint = null;
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
        public void pruneMiningRockSuppression() {
            // No-op.
        }

        @Override
        public void extendMiningRetryWindow() {
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
        public void clearMiningOutcomeWaitWindow() {
            // No-op.
        }

        @Override
        public long miningOutcomeWaitUntilMs() {
            return 0L;
        }

        @Override
        public Optional<TileObject> resolveNearestSelectedRockTargetExcludingLocked() {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveNearestSelectedRockTarget() {
            return Optional.empty();
        }

        @Override
        public boolean hasLockedMiningTarget() {
            return lockedRockTarget.isPresent();
        }

        @Override
        public Optional<TileObject> resolveLockedRockTarget() {
            return lockedRockTarget;
        }

        @Override
        public Optional<TileObject> resolveNearestRockTarget() {
            return Optional.empty();
        }

        @Override
        public Optional<TileObject> resolveNearestRockTargetExcluding(WorldPoint excludedWorldPoint) {
            return nearestRockTargetExcluding;
        }

        @Override
        public void lockMiningTarget(TileObject targetObject) {
            lockedTargetId = targetObject == null ? -1 : targetObject.getId();
            lockedRockTarget = Optional.ofNullable(targetObject);
        }

        @Override
        public void clearMiningInteractionWindows() {
            // No-op.
        }

        @Override
        public int selectedMiningTargetCount() {
            return 0;
        }

        @Override
        public Point resolveMiningHoverPoint(TileObject targetObject) {
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
        public void clearMiningTargetLock() {
            lockedRockTarget = Optional.empty();
        }

        @Override
        public void clearMiningHoverPoint() {
            // No-op.
        }

        @Override
        public void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point) {
            // No-op.
        }

        @Override
        public MotorDispatchResult dispatchMiningMoveAndClick(
            Point canvasPoint,
            ClickMotionSettings motion,
            TileObject targetObject
        ) {
            scheduleCalls++;
            return new MotorDispatchResult(1L, MotorDispatchStatus.COMPLETE, "complete");
        }

        @Override
        public void noteInteractionActivityNow() {
            // No-op.
        }

        @Override
        public void suppressMiningRockTarget(WorldPoint worldPoint, long durationMs) {
            suppressedWorldPoint = worldPoint;
        }

        @Override
        public long miningTargetReclickCooldownMs() {
            return 5_000L;
        }

        @Override
        public void beginMiningOutcomeWaitWindow() {
            // No-op.
        }

        @Override
        public void incrementClicksDispatched() {
            // No-op.
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
