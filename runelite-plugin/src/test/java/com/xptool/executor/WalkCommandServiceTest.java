package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

class WalkCommandServiceTest {
    @Test
    void reroutesToAlternateWalkTargetDuringSameTargetCooldown() throws Exception {
        TestHost host = new TestHost();
        WalkCommandService service = new WalkCommandService(host);
        long now = System.currentTimeMillis();

        host.localPlayerWorldPoint = new WorldPoint(3200, 3200, 0);
        host.requestedWorldPoint = new WorldPoint(3201, 3201, 0);
        host.initialResolvedWorldPoint = new WorldPoint(3201, 3201, 0);
        host.alternateResolvedWorldPoint = new WorldPoint(3203, 3201, 0);
        host.enableAlternateReroute = true;
        host.setClickPoint(3201, 3201, 0, new Point(410, 310));
        host.setClickPoint(3203, 3201, 0, new Point(430, 315));

        setField(service, "lastDispatchWorldPoint", host.initialResolvedWorldPoint);
        setField(service, "lastDispatchAtMs", now - 180L);

        JsonObject payload = new JsonObject();
        payload.addProperty("targetWorldX", 3201);
        payload.addProperty("targetWorldY", 3201);

        CommandExecutor.CommandDecision decision = service.executeWalkToWorldPoint(payload, MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("walk_left_click_dispatched", decision.getReason());
        assertTrue(host.lastAcceptedDetails.get("reroutedFromSameTargetCooldown").getAsBoolean());
        assertEquals(3203, host.lastAcceptedDetails.get("targetWorldX").getAsInt());
        assertEquals(3201, host.lastAcceptedDetails.get("targetWorldY").getAsInt());
    }

    @Test
    void defersWhenSameTargetCooldownActiveAndNoAlternateWalkTargetExists() throws Exception {
        TestHost host = new TestHost();
        WalkCommandService service = new WalkCommandService(host);
        long now = System.currentTimeMillis();

        host.localPlayerWorldPoint = new WorldPoint(3200, 3200, 0);
        host.requestedWorldPoint = new WorldPoint(3201, 3201, 0);
        host.initialResolvedWorldPoint = new WorldPoint(3201, 3201, 0);
        host.enableAlternateReroute = false;
        host.setClickPoint(3201, 3201, 0, new Point(410, 310));

        setField(service, "lastDispatchWorldPoint", host.initialResolvedWorldPoint);
        setField(service, "lastDispatchAtMs", now - 180L);

        JsonObject payload = new JsonObject();
        payload.addProperty("targetWorldX", 3201);
        payload.addProperty("targetWorldY", 3201);

        CommandExecutor.CommandDecision decision = service.executeWalkToWorldPoint(payload, MotionProfile.GENERIC_INTERACT);

        assertTrue(decision.isAccepted());
        assertEquals("walk_same_target_reclick_cooldown", decision.getReason());
        assertFalse(host.lastAcceptedDetails.get("reroutedFromSameTargetCooldown").getAsBoolean());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Player playerProxy(WorldPoint worldPoint) {
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
                if ("getWorldLocation".equals(method.getName())) {
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

    private static String worldKey(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return "";
        }
        return worldPoint.getX() + ":" + worldPoint.getY() + ":" + worldPoint.getPlane();
    }

    private static String worldKey(int x, int y, int plane) {
        return x + ":" + y + ":" + plane;
    }

    private static final class TestHost implements WalkCommandService.Host {
        private final Map<String, Point> clickPointsByWorld = new HashMap<>();
        private WorldPoint localPlayerWorldPoint = null;
        private WorldPoint requestedWorldPoint = null;
        private WorldPoint initialResolvedWorldPoint = null;
        private WorldPoint alternateResolvedWorldPoint = null;
        private boolean enableAlternateReroute = false;
        private JsonObject lastAcceptedDetails = new JsonObject();

        private void setClickPoint(int x, int y, int plane, Point point) {
            clickPointsByWorld.put(worldKey(x, y, plane), point == null ? null : new Point(point));
        }

        @Override
        public CommandExecutor.CommandDecision accept(String reason, JsonObject details) {
            lastAcceptedDetails = details == null ? new JsonObject() : details.deepCopy();
            return newDecision(true, reason, details);
        }

        @Override
        public CommandExecutor.CommandDecision reject(String reason) {
            return newDecision(false, reason, null);
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
        public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
            return new ClickMotionSettings(0.0, 4L, 6L);
        }

        @Override
        public Player localPlayer() {
            return playerProxy(localPlayerWorldPoint);
        }

        @Override
        public Point resolveWorldTileClickPoint(WorldPoint worldPoint) {
            Point point = clickPointsByWorld.get(worldKey(worldPoint));
            return point == null ? null : new Point(point);
        }

        @Override
        public Point resolveWorldTileMinimapClickPoint(WorldPoint worldPoint) {
            Point point = clickPointsByWorld.get(worldKey(worldPoint));
            return point == null ? null : new Point(point);
        }

        @Override
        public WorldPoint resolveNearestWalkableWorldPoint(
            WorldPoint localWorldPoint,
            WorldPoint targetWorldPoint,
            int maxRadiusTiles
        ) {
            if (targetWorldPoint == null) {
                return null;
            }
            if (maxRadiusTiles >= 3 && requestedWorldPoint != null
                && targetWorldPoint.getX() == requestedWorldPoint.getX()
                && targetWorldPoint.getY() == requestedWorldPoint.getY()
                && targetWorldPoint.getPlane() == requestedWorldPoint.getPlane()) {
                return initialResolvedWorldPoint;
            }
            if (maxRadiusTiles == 0 && enableAlternateReroute && alternateResolvedWorldPoint != null
                && targetWorldPoint.getX() == alternateResolvedWorldPoint.getX()
                && targetWorldPoint.getY() == alternateResolvedWorldPoint.getY()
                && targetWorldPoint.getPlane() == alternateResolvedWorldPoint.getPlane()) {
                return alternateResolvedWorldPoint;
            }
            return null;
        }

        @Override
        public boolean isUsableCanvasPoint(Point point) {
            return point != null && point.x >= 0 && point.y >= 0;
        }

        @Override
        public MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile) {
            return new MotorHandle(1L, MotorGestureStatus.COMPLETE, "complete");
        }

        @Override
        public MotorProfile buildWalkMoveAndClickProfile(ClickMotionSettings motion) {
            return null;
        }

        @Override
        public void noteInteractionActivityNow() {
            // No-op.
        }

        @Override
        public void incrementClicksDispatched() {
            // No-op.
        }

        private CommandExecutor.CommandDecision newDecision(boolean accepted, String reason, JsonObject details) {
            try {
                Constructor<CommandExecutor.CommandDecision> ctor = CommandExecutor.CommandDecision.class.getDeclaredConstructor(
                    boolean.class,
                    String.class,
                    JsonObject.class
                );
                ctor.setAccessible(true);
                return ctor.newInstance(accepted, reason, details);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to construct CommandDecision", ex);
            }
        }
    }
}
