package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

class RandomEventDismissRuntimeTest {
    @Test
    void emitsDismissDeferredConflictWhenSessionConflictActive() {
        TestHost host = new TestHost();
        host.activeSessionOtherThanInteraction = true;
        RandomEventDismissRuntime runtime = new RandomEventDismissRuntime(host);

        runtime.onGameTick(20);

        assertTrue(host.reasons.contains("random_event_dismiss_deferred_conflict"));
        JsonObject details = host.firstDetailsForReason("random_event_dismiss_deferred_conflict");
        assertTrue(details != null);
        assertEquals("dismiss_deferred_conflict", details.get("deferReason").getAsString());
    }

    @Test
    void doesNotReleaseInteractionOwnershipWhenSessionGateBlocksBeforeAcquire() {
        TestHost host = new TestHost();
        host.activeSessionOtherThanInteraction = true;
        RandomEventDismissRuntime runtime = new RandomEventDismissRuntime(host);

        runtime.onGameTick(25);

        assertEquals(0, host.acquireInteractionMotorOwnershipCalls);
        assertEquals(0, host.releaseInteractionMotorOwnershipCalls);
    }

    @Test
    void emitsSameTargetRetryGuardWhenGuardWindowActive() throws Exception {
        TestHost host = new TestHost();
        host.withSingleCandidateNpc(321, 9001, new WorldPoint(3202, 3202, 0), new Point(400, 300));
        RandomEventDismissRuntime runtime = new RandomEventDismissRuntime(host);

        setIntField(runtime, "lastDispatchNpcIndex", 321);
        setLongField(runtime, "sameTargetRetryGuardUntilMs", System.currentTimeMillis() + 10_000L);

        runtime.onGameTick(50);

        assertTrue(host.reasons.contains("random_event_same_target_retry_guard"));
        assertEquals(1, host.acquireInteractionMotorOwnershipCalls);
        assertEquals(1, host.releaseInteractionMotorOwnershipCalls);
    }

    @Test
    void releasesInteractionOwnershipAfterAcquireWhenDeferred() {
        TestHost host = new TestHost();
        host.withSingleCandidateNpc(420, 9004, new WorldPoint(3202, 3202, 0), new Point(430, 330));
        RandomEventDismissRuntime runtime = new RandomEventDismissRuntime(host);

        runtime.onGameTick(75);

        assertEquals(1, host.acquireInteractionMotorOwnershipCalls);
        assertEquals(1, host.releaseInteractionMotorOwnershipCalls);
        assertTrue(host.reasons.contains("random_event_dismiss_deferred"));
    }

    @Test
    void defersOnCursorReadyHoldThenDispatchesOnNextTick() throws Exception {
        TestHost host = new TestHost();
        host.withSingleCandidateNpc(777, 9002, new WorldPoint(3202, 3202, 0), new Point(420, 320));
        host.cursorNearTarget = true;
        RandomEventDismissRuntime runtime = new RandomEventDismissRuntime(host);

        setIntField(runtime, "observedCandidateNpcIndex", 777);
        setIntField(runtime, "candidateStableTicks", 1);
        setIntField(runtime, "localStationaryTicks", 1);
        setObjectField(runtime, "lastLocalWorldPoint", host.localPoint);

        runtime.onGameTick(100);

        assertTrue(host.reasons.contains("random_event_dismiss_deferred"));
        assertTrue(host.hasDeferredReason("cursor_ready_hold"));

        host.clearEvents();
        setLongField(runtime, "cooldownUntilMs", 0L);
        setIntField(runtime, "retryAtTick", Integer.MIN_VALUE);
        setIntField(runtime, "cursorReadyNpcIndex", 777);
        setLongField(runtime, "cursorReadySinceMs", System.currentTimeMillis() - 200L);
        setLongField(runtime, "cursorReadyHoldTargetMs", 40L);

        runtime.onGameTick(101);

        assertTrue(host.reasons.contains("random_event_dismiss_attempt"));
        assertTrue(host.reasons.contains("random_event_dismiss_dispatched"));
        assertEquals(1, host.selectDismissMenuCalls);
        assertEquals(2, host.acquireInteractionMotorOwnershipCalls);
        assertEquals(2, host.releaseInteractionMotorOwnershipCalls);
    }

    @Test
    void defersWhenTargetRepeatBlockGuardIsActive() throws Exception {
        TestHost host = new TestHost();
        host.withSingleCandidateNpc(888, 9055, new WorldPoint(3202, 3202, 0), new Point(420, 322));
        RandomEventDismissRuntime runtime = new RandomEventDismissRuntime(host);

        setIntField(runtime, "observedCandidateNpcIndex", 888);
        setIntField(runtime, "candidateStableTicks", 1);
        setIntField(runtime, "localStationaryTicks", 1);
        setObjectField(runtime, "lastLocalWorldPoint", host.localPoint);

        long key = dismissedTargetZoneKey(9055, 888, new Point(420, 322));
        long[] history = (long[]) getField(runtime, "dismissedTargetZoneHistory");
        long[] until = (long[]) getField(runtime, "dismissedTargetZoneUntilMs");
        history[0] = key;
        until[0] = System.currentTimeMillis() + 5_000L;

        runtime.onGameTick(130);

        assertTrue(host.reasons.contains("random_event_dismiss_deferred"));
        assertTrue(host.hasDeferredReason("target_repeat_blocked"));
    }

    @Test
    void emitsVerifyFailedRetryWhenMenuSelectFailsAfterReady() throws Exception {
        TestHost host = new TestHost();
        host.withSingleCandidateNpc(990, 9066, new WorldPoint(3202, 3202, 0), new Point(420, 322));
        host.cursorNearTarget = true;
        host.selectDismissResult = false;
        RandomEventDismissRuntime runtime = new RandomEventDismissRuntime(host);

        setIntField(runtime, "observedCandidateNpcIndex", 990);
        setIntField(runtime, "candidateStableTicks", 1);
        setIntField(runtime, "localStationaryTicks", 1);
        setObjectField(runtime, "lastLocalWorldPoint", host.localPoint);
        setIntField(runtime, "cursorReadyNpcIndex", 990);
        setLongField(runtime, "cursorReadySinceMs", System.currentTimeMillis() - 250L);
        setLongField(runtime, "cursorReadyHoldTargetMs", 40L);

        runtime.onGameTick(180);

        assertTrue(host.reasons.contains("random_event_verify_failed_retry_scheduled"));
        assertTrue(host.reasons.contains("random_event_dismiss_retry_scheduled"));
    }

    private static final class TestHost implements RandomEventDismissRuntime.Host {
        private boolean runtimeEnabled = true;
        private boolean runtimeArmed = true;
        private boolean loggedIn = true;
        private boolean bankOpen = false;
        private boolean activeSessionOtherThanInteraction = false;
        private boolean activeInteractionMotorProgram = false;
        private boolean canAcquireInteractionMotorOwnership = true;
        private Player localPlayer = playerProxy(new WorldPoint(3200, 3200, 0), null);
        private WorldPoint localPoint = new WorldPoint(3200, 3200, 0);
        private final List<NPC> npcList = new ArrayList<>();
        private Point clickPoint = new Point(400, 300);
        private boolean cursorNearTarget = false;
        private boolean moveCursorResult = true;
        private boolean selectDismissResult = true;
        private int selectDismissMenuCalls = 0;
        private int acquireInteractionMotorOwnershipCalls = 0;
        private int releaseInteractionMotorOwnershipCalls = 0;
        private final List<String> reasons = new ArrayList<>();
        private final List<JsonObject> detailsByEvent = new ArrayList<>();

        private long preAttemptCooldownMinMs = 1L;
        private long preAttemptCooldownMaxMs = 1L;
        private long successCooldownMinMs = 1L;
        private long successCooldownMaxMs = 1L;
        private long failureCooldownMinMs = 1L;
        private long failureCooldownMaxMs = 1L;
        private long cursorReadyHoldMs = 80L;

        private void withSingleCandidateNpc(int npcIndex, int npcId, WorldPoint worldPoint, Point targetPoint) {
            NPC npc = npcProxy(npcIndex, npcId, worldPoint, localPlayer, "Genie");
            this.npcList.clear();
            this.npcList.add(npc);
            this.clickPoint = targetPoint == null ? null : new Point(targetPoint);
            this.localPoint = new WorldPoint(3200, 3200, worldPoint == null ? 0 : worldPoint.getPlane());
            this.localPlayer = playerProxy(this.localPoint, null);
            NPC updatedNpc = npcProxy(npcIndex, npcId, worldPoint, localPlayer, "Genie");
            this.npcList.clear();
            this.npcList.add(updatedNpc);
        }

        private void clearEvents() {
            reasons.clear();
            detailsByEvent.clear();
        }

        private boolean hasDeferredReason(String deferReason) {
            for (int i = 0; i < reasons.size(); i++) {
                if (!"random_event_dismiss_deferred".equals(reasons.get(i))) {
                    continue;
                }
                JsonObject details = detailsByEvent.get(i);
                if (details == null) {
                    continue;
                }
                JsonElement token = details.get("deferReason");
                if (token == null || token.isJsonNull()) {
                    continue;
                }
                if (deferReason.equals(token.getAsString())) {
                    return true;
                }
            }
            return false;
        }

        private JsonObject firstDetailsForReason(String reason) {
            for (int i = 0; i < reasons.size(); i++) {
                if (reason.equals(reasons.get(i))) {
                    return detailsByEvent.get(i);
                }
            }
            return null;
        }

        @Override
        public boolean isRuntimeEnabled() {
            return runtimeEnabled;
        }

        @Override
        public boolean isRuntimeArmed() {
            return runtimeArmed;
        }

        @Override
        public boolean isLoggedIn() {
            return loggedIn;
        }

        @Override
        public boolean isBankOpen() {
            return bankOpen;
        }

        @Override
        public boolean hasActiveSessionOtherThan(String sessionName) {
            return activeSessionOtherThanInteraction;
        }

        @Override
        public boolean hasActiveInteractionMotorProgram() {
            return activeInteractionMotorProgram;
        }

        @Override
        public boolean acquireOrRenewInteractionMotorOwnership() {
            acquireInteractionMotorOwnershipCalls++;
            return canAcquireInteractionMotorOwnership;
        }

        @Override
        public void releaseInteractionMotorOwnership() {
            releaseInteractionMotorOwnershipCalls++;
        }

        @Override
        public Player localPlayer() {
            return localPlayer;
        }

        @Override
        public Iterable<NPC> npcs() {
            return npcList;
        }

        @Override
        public Point resolveVariedNpcClickPoint(NPC npc) {
            return clickPoint == null ? null : new Point(clickPoint);
        }

        @Override
        public boolean isUsableCanvasPoint(Point point) {
            return point != null && point.x >= 0 && point.y >= 0;
        }

        @Override
        public boolean moveInteractionCursorToCanvasPoint(Point canvasPoint) {
            if (moveCursorResult) {
                cursorNearTarget = true;
            }
            return moveCursorResult;
        }

        @Override
        public boolean isCursorNearTarget(Point canvasPoint) {
            return cursorNearTarget;
        }

        @Override
        public boolean selectDismissMenuOptionAt(Point canvasPoint) {
            selectDismissMenuCalls++;
            return selectDismissResult;
        }

        @Override
        public long randomBetween(long minInclusive, long maxInclusive) {
            return Math.min(minInclusive, maxInclusive);
        }

        @Override
        public long randomEventPreAttemptCooldownMinMs() {
            return preAttemptCooldownMinMs;
        }

        @Override
        public long randomEventPreAttemptCooldownMaxMs() {
            return preAttemptCooldownMaxMs;
        }

        @Override
        public long randomEventSuccessCooldownMinMs() {
            return successCooldownMinMs;
        }

        @Override
        public long randomEventSuccessCooldownMaxMs() {
            return successCooldownMaxMs;
        }

        @Override
        public long randomEventFailureRetryCooldownMinMs() {
            return failureCooldownMinMs;
        }

        @Override
        public long randomEventFailureRetryCooldownMaxMs() {
            return failureCooldownMaxMs;
        }

        @Override
        public long randomEventCursorReadyHoldMs() {
            return cursorReadyHoldMs;
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
        public void emitRandomEventEvent(String reason, JsonObject details) {
            reasons.add(reason == null ? "" : reason);
            detailsByEvent.add(details == null ? new JsonObject() : details.deepCopy());
        }
    }

    private static Player playerProxy(WorldPoint worldPoint, Actor interacting) {
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("getWorldLocation".equals(name)) {
                    return worldPoint;
                }
                if ("getInteracting".equals(name)) {
                    return interacting;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static NPC npcProxy(int index, int id, WorldPoint worldPoint, Actor interacting, String name) {
        NPCComposition composition = npcCompositionProxy(name);
        return (NPC) Proxy.newProxyInstance(
            NPC.class.getClassLoader(),
            new Class<?>[] {NPC.class},
            (proxy, method, args) -> {
                String methodName = method.getName();
                if ("getIndex".equals(methodName)) {
                    return index;
                }
                if ("getId".equals(methodName)) {
                    return id;
                }
                if ("getWorldLocation".equals(methodName)) {
                    return worldPoint;
                }
                if ("getInteracting".equals(methodName)) {
                    return interacting;
                }
                if ("getTransformedComposition".equals(methodName)) {
                    return null;
                }
                if ("getComposition".equals(methodName)) {
                    return composition;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static NPCComposition npcCompositionProxy(String npcName) {
        return (NPCComposition) Proxy.newProxyInstance(
            NPCComposition.class.getClassLoader(),
            new Class<?>[] {NPCComposition.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("isInteractible".equals(name)) {
                    return true;
                }
                if ("getActions".equals(name)) {
                    return new String[] {"Dismiss"};
                }
                if ("getName".equals(name)) {
                    return npcName;
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

    private static void setIntField(Object target, String fieldName, int value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    private static void setLongField(Object target, String fieldName, long value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setLong(target, value);
    }

    private static void setObjectField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    private static long dismissedTargetZoneKey(int npcId, int npcIndex, Point point) {
        int grid = 28;
        int zoneX = Math.max(0, point.x / grid);
        int zoneY = Math.max(0, point.y / grid);
        long idBits = Math.max(0, npcId) & 0xFFFFFL;
        long indexBits = Math.max(0, npcIndex) & 0xFFFFFL;
        long zoneXBits = zoneX & 0x1FFFL;
        long zoneYBits = zoneY & 0x1FFFL;
        return (idBits << 46) | (indexBits << 26) | (zoneXBits << 13) | zoneYBits;
    }
}
