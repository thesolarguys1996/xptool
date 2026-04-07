package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.fishing.FishingExecutionContext;
import com.xptool.activities.fishing.FishingResolvedTarget;
import com.xptool.core.motor.MotorDispatchResult;
import com.xptool.core.motor.MotorDispatchStatus;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

class FishingCommandServiceTest {
    @Test
    void sameTargetCooldownHoldsWithoutReroute() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC primary = npcProxy(10, 9001, new WorldPoint(3201, 3201, 0));
        host.setNpcClickPoint(10, new Point(410, 310));
        host.nearestTarget = Optional.of(primary);
        host.lastDispatchWorldPoint = new WorldPoint(3201, 3201, 0);

        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 500L,
            500L,
            10,
            0,
            false
        );
        FishingResolvedTarget sameTarget = new FishingResolvedTarget(
            primary,
            new Point(410, 310),
            primary.getId(),
            primary.getIndex(),
            primary.getWorldLocation(),
            1,
            true,
            true
        );

        RuntimeDecision decision = invokeMaybeAcceptPreDispatchDelay(service, context, sameTarget);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertEquals("fishing_post_dispatch_hold_window", decision.getReason());
        assertEquals(-1, host.lockedTargetIndex);
    }

    @Test
    void defersWhenSameTargetCooldownActiveAndNoRerouteWasApplied() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        host.lastDropSweepSessionEndedAtMs = now - 100L;
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC primary = npcProxy(10, 9001, new WorldPoint(3201, 3201, 0));

        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 500L,
            500L,
            10,
            0,
            false
        );
        FishingResolvedTarget sameTarget = new FishingResolvedTarget(
            primary,
            new Point(410, 310),
            primary.getId(),
            primary.getIndex(),
            primary.getWorldLocation(),
            1,
            true,
            true
        );

        RuntimeDecision decision = invokeMaybeAcceptPreDispatchDelay(service, context, sameTarget);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertEquals("fishing_post_dispatch_hold_window", decision.getReason());
    }

    @Test
    void sameTargetCooldownDoesNotAnnotateRerouteAttempt() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        host.lastDropSweepSessionEndedAtMs = now - 100L;
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC primary = npcProxy(10, 9001, new WorldPoint(3201, 3201, 0));

        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 500L,
            500L,
            10,
            0,
            false
        );
        FishingResolvedTarget sameTarget = new FishingResolvedTarget(
            primary,
            new Point(410, 310),
            primary.getId(),
            primary.getIndex(),
            primary.getWorldLocation(),
            1,
            true,
            true
        );

        RuntimeDecision decision = invokeMaybeAcceptPreDispatchDelay(service, context, sameTarget);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertEquals("fishing_post_dispatch_hold_window", decision.getReason());
    }

    @Test
    void depletedSpotReacquireDelayAddsNonDeterministicHoldBeforeSwitchingTarget() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        host.fishingOutcomeWaitUntilMs = now - 10L;
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC newTarget = npcProxy(52, 9052, new WorldPoint(3205, 3204, 0));

        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 26000L,
            26000L,
            51,
            0,
            false
        );
        FishingResolvedTarget switchedTarget = new FishingResolvedTarget(
            newTarget,
            new Point(430, 332),
            newTarget.getId(),
            newTarget.getIndex(),
            newTarget.getWorldLocation(),
            6,
            false,
            false
        );

        RuntimeDecision decision = invokeMaybeAcceptPreDispatchDelay(service, context, switchedTarget);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertEquals("fishing_depleted_spot_reacquire_delay", decision.getReason());
    }

    @Test
    void dispatchSelectionUsesNearestCandidateFromPool() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC recent = npcProxy(15, 9005, new WorldPoint(3201, 3201, 0));
        NPC fresh = npcProxy(16, 9006, new WorldPoint(3205, 3205, 0));
        host.nearestTargets = new ArrayList<>();
        host.nearestTargets.add(recent);
        host.nearestTargets.add(fresh);
        host.setNpcClickPoint(15, new Point(405, 305));
        host.setNpcClickPoint(16, new Point(435, 335));
        seedRecentTargetHistory(service, recent.getIndex(), recent.getWorldLocation());

        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            0L,
            0L,
            -1,
            0,
            false
        );

        FishingResolvedTarget selected = invokeResolveTargetForDispatch(service, context);

        assertNotNull(selected);
        assertTrue(selected.targetIndex == 15 || selected.targetIndex == 16);
        assertEquals(2, selected.selectionPoolSize);
        assertTrue(
            "nearest_top_n_nearest_priority".equals(selected.selectionPolicy)
                || "nearest_top_n_weighted".equals(selected.selectionPolicy)
                || "nearest_top_n_weighted_recent".equals(selected.selectionPolicy)
        );
    }

    @Test
    void dispatchSelectionReroutesWhenSameTargetCooldownStillActive() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC same = npcProxy(40, 9040, new WorldPoint(3201, 3201, 0));
        NPC alternate = npcProxy(41, 9041, new WorldPoint(3203, 3202, 0));
        host.nearestTargets = new ArrayList<>();
        host.nearestTargets.add(same);
        host.nearestTargets.add(alternate);
        host.setNpcClickPoint(40, new Point(410, 310));
        host.setNpcClickPoint(41, new Point(430, 330));
        host.lastDispatchWorldPoint = same.getWorldLocation();

        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 300L,
            300L,
            40,
            0,
            false
        );

        FishingResolvedTarget selected = invokeResolveTargetForDispatch(service, context);

        assertNotNull(selected);
        assertEquals(41, selected.targetIndex);
        assertEquals("nearest_top_n_weighted_cooldown_reroute", selected.selectionPolicy);
    }

    @Test
    void noProgressWindowUsesSameTargetCooldownPathWithoutReroute() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC primary = npcProxy(10, 9001, new WorldPoint(3201, 3201, 0));
        host.lastDropSweepSessionEndedAtMs = now - 100L;
        host.fishingOutcomeWaitUntilMs = now - 1L;
        host.nearestTargets = new ArrayList<>();

        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 2000L,
            2000L,
            10,
            0,
            false
        );
        FishingResolvedTarget sameTarget = new FishingResolvedTarget(
            primary,
            new Point(410, 310),
            primary.getId(),
            primary.getIndex(),
            primary.getWorldLocation(),
            1,
            true,
            true
        );

        RuntimeDecision decision = invokeMaybeAcceptPreDispatchDelay(service, context, sameTarget);

        assertNull(decision);
    }

    @Test
    void clickPointRegionMemoryAvoidsRepeatedHullZone() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        NPC target = npcProxy(21, 9010, new WorldPoint(3210, 3210, 0));
        host.setNpcClickPointSequence(
            21,
            List.of(
                new Point(102, 99),
                new Point(141, 139)
            )
        );
        seedRecentClickRegionHistory(service, new Point(100, 100));

        Point point = invokeResolveRetrySafeNpcClickPoint(service, target);

        assertNotNull(point);
        assertEquals(141, point.x);
        assertEquals(139, point.y);
    }

    @Test
    void dispatchTelemetryIncludesSelectionCodes() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC targetNpc = npcProxy(30, 9020, new WorldPoint(3203, 3202, 0));
        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 500L,
            500L,
            30,
            0,
            false
        );
        FishingResolvedTarget target = new FishingResolvedTarget(
            targetNpc,
            new Point(420, 320),
            targetNpc.getId(),
            targetNpc.getIndex(),
            targetNpc.getWorldLocation(),
            2,
            false,
            true,
            "nearest",
            4,
            "nearest_top_n"
        );

        RuntimeDecision decision = invokeDispatchToTarget(service, context, target);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertEquals("fishing_left_click_dispatched", decision.getReason());
        JsonObject details = decision.getDetails();
        assertNotNull(details);
        assertTrue(details.has("selection_pool_size"));
        assertTrue(details.has("selection_policy"));
        assertEquals(4, details.get("selection_pool_size").getAsInt());
        assertEquals("nearest_top_n", details.get("selection_policy").getAsString());
    }

    @Test
    void dispatchTelemetryFlagsLevelUpFastRefishHoldBypass() throws Exception {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        Player local = playerProxy(new WorldPoint(3200, 3200, 0));
        NPC targetNpc = npcProxy(31, 9021, new WorldPoint(3202, 3201, 0));
        FishingExecutionContext context = new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            local,
            Collections.emptySet(),
            now - 300L,
            300L,
            -1,
            0,
            true
        );
        FishingResolvedTarget target = new FishingResolvedTarget(
            targetNpc,
            new Point(421, 321),
            targetNpc.getId(),
            targetNpc.getIndex(),
            targetNpc.getWorldLocation(),
            2,
            false,
            false
        );

        RuntimeDecision gateDecision = invokeMaybeAcceptPreDispatchDelay(service, context, target);
        assertNull(gateDecision);
        RuntimeDecision dispatchDecision = invokeDispatchToTarget(service, context, target);

        assertNotNull(dispatchDecision);
        assertTrue(dispatchDecision.isAccepted());
        assertEquals("fishing_left_click_dispatched", dispatchDecision.getReason());
        JsonObject details = dispatchDecision.getDetails();
        assertNotNull(details);
        assertTrue(details.has("levelUpFastRefishHoldBypassed"));
        assertTrue(details.get("levelUpFastRefishHoldBypassed").getAsBoolean());
        assertTrue(details.has("levelUpFastRefishHoldBypassRemainingMs"));
        assertTrue(details.get("levelUpFastRefishHoldBypassRemainingMs").getAsLong() > 0L);
    }

    @Test
    void busyAnimationWithoutInteractionOrRecentDispatchDoesNotDefer() {
        TestHost host = new TestHost();
        FishingCommandService service = new FishingCommandService(host);
        NPC targetNpc = npcProxy(70, 9070, new WorldPoint(3201, 3201, 0));
        host.currentPlayer = playerProxy(new WorldPoint(3200, 3200, 0), 622, null);
        host.nearestTarget = Optional.of(targetNpc);
        host.lastDispatchAtMs = 0L;

        RuntimeDecision decision = service.executeFishNearestSpot(new JsonObject(), MotionProfile.GENERIC_INTERACT);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertTrue(!"fishing_busy_animation_active".equals(decision.getReason()));
    }

    @Test
    void levelUpPromptDismissModeDispatchesContinueDismiss() throws Exception {
        TestHost host = new TestHost();
        host.levelUpPromptVisible = true;
        host.dismissLevelUpPromptResult = true;
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        forceLevelUpDismissMode(service, now - 1L);
        FishingExecutionContext context = fishingContextAt(now);

        RuntimeDecision decision = invokeMaybeHandleLevelUpPrompt(service, context);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertEquals("fishing_level_up_continue_dismissed", decision.getReason());
        assertTrue(host.dismissLevelUpPromptAttempts > 0);
    }

    @Test
    void levelUpPromptDismissModePendingWaitsWhenDismissFails() throws Exception {
        TestHost host = new TestHost();
        host.levelUpPromptVisible = true;
        host.dismissLevelUpPromptResult = false;
        FishingCommandService service = new FishingCommandService(host);
        long now = System.currentTimeMillis();
        forceLevelUpDismissMode(service, now - 1L);
        FishingExecutionContext context = fishingContextAt(now);

        RuntimeDecision decision = invokeMaybeHandleLevelUpPrompt(service, context);

        assertNotNull(decision);
        assertTrue(decision.isAccepted());
        assertEquals("fishing_level_up_continue_pending", decision.getReason());
        assertTrue(host.dismissLevelUpPromptAttempts > 0);
        JsonObject details = decision.getDetails();
        assertNotNull(details);
        assertTrue(details.has("waitMsRemaining"));
        assertTrue(details.get("waitMsRemaining").getAsLong() > 0L);
    }

    private static RuntimeDecision invokeMaybeHandleLevelUpPrompt(
        FishingCommandService service,
        FishingExecutionContext context
    ) throws Exception {
        Method method = FishingCommandService.class.getDeclaredMethod(
            "maybeHandleLevelUpPrompt",
            FishingExecutionContext.class
        );
        method.setAccessible(true);
        return (RuntimeDecision) method.invoke(service, context);
    }

    private static FishingExecutionContext fishingContextAt(long now) {
        return new FishingExecutionContext(
            new JsonObject(),
            new ClickMotionSettings(0.0, 4L, 6L),
            now,
            coreNeutralFatigue(),
            null,
            Collections.emptySet(),
            0L,
            0L,
            -1,
            0,
            false
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void forceLevelUpDismissMode(FishingCommandService service, long retryAtMs) throws Exception {
        writeField(service, "levelUpPromptEpisodeActive", true);
        writeField(service, "levelUpPromptLastSeenAtMs", System.currentTimeMillis());
        writeField(service, "levelUpDismissRetryAtMs", retryAtMs);
        Field modeField = service.getClass().getDeclaredField("levelUpRecoveryMode");
        modeField.setAccessible(true);
        Class<? extends Enum> modeType = (Class<? extends Enum>) modeField.getType().asSubclass(Enum.class);
        modeField.set(service, Enum.valueOf(modeType, "DISMISS_CONTINUE"));
    }

    private static RuntimeDecision invokeMaybeAcceptPreDispatchDelay(
        FishingCommandService service,
        FishingExecutionContext context,
        FishingResolvedTarget target
    ) throws Exception {
        Method method = FishingCommandService.class.getDeclaredMethod(
            "maybeAcceptPreDispatchDelay",
            FishingExecutionContext.class,
            FishingResolvedTarget.class
        );
        method.setAccessible(true);
        return (RuntimeDecision) method.invoke(service, context, target);
    }

    private static FishingResolvedTarget invokeResolveTargetForDispatch(
        FishingCommandService service,
        FishingExecutionContext context
    ) throws Exception {
        Method method = FishingCommandService.class.getDeclaredMethod(
            "resolveTargetForDispatch",
            FishingExecutionContext.class
        );
        method.setAccessible(true);
        return (FishingResolvedTarget) method.invoke(service, context);
    }

    private static Point invokeResolveRetrySafeNpcClickPoint(FishingCommandService service, NPC npc) throws Exception {
        Method method = FishingCommandService.class.getDeclaredMethod("resolveRetrySafeNpcClickPoint", NPC.class);
        method.setAccessible(true);
        return (Point) method.invoke(service, npc);
    }

    private static RuntimeDecision invokeDispatchToTarget(
        FishingCommandService service,
        FishingExecutionContext context,
        FishingResolvedTarget target
    ) throws Exception {
        Method method = FishingCommandService.class.getDeclaredMethod(
            "dispatchToTarget",
            FishingExecutionContext.class,
            FishingResolvedTarget.class
        );
        method.setAccessible(true);
        return (RuntimeDecision) method.invoke(service, context, target);
    }

    private static void seedRecentTargetHistory(
        FishingCommandService service,
        int targetIndex,
        WorldPoint worldPoint
    ) throws Exception {
        long[] history = (long[]) readField(service, "recentTargetHistory");
        long key = targetHistoryKey(targetIndex, worldPoint);
        history[0] = key;
    }

    private static com.xptool.core.runtime.FatigueSnapshot coreNeutralFatigue() {
        return com.xptool.core.runtime.FatigueSnapshot.neutral();
    }

    private static void seedRecentClickRegionHistory(FishingCommandService service, Point point) throws Exception {
        long[] history = (long[]) readField(service, "recentClickRegionHistory");
        history[0] = clickRegionKey(point);
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void writeField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static long targetHistoryKey(int targetIndex, WorldPoint worldPoint) {
        long indexBits = targetIndex < 0 ? 0xFFFFFL : (targetIndex & 0xFFFFFL);
        long worldBits = worldPointKey(worldPoint);
        if (worldBits == Long.MIN_VALUE) {
            return (indexBits << 43) | 0x7FFFFFFFFFFL;
        }
        return (indexBits << 43) | (worldBits & 0x7FFFFFFFFFFL);
    }

    private static long worldPointKey(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return Long.MIN_VALUE;
        }
        long x = worldPoint.getX() & 0x1FFFFFL;
        long y = worldPoint.getY() & 0x1FFFFFL;
        long plane = worldPoint.getPlane() & 0x3L;
        return (plane << 42) | (x << 21) | y;
    }

    private static long clickRegionKey(Point point) {
        if (point == null) {
            return Long.MIN_VALUE;
        }
        int regionSize = resolveFishingClickRegionSizePx();
        int regionX = Math.floorDiv(point.x, regionSize);
        int regionY = Math.floorDiv(point.y, regionSize);
        long x = ((long) regionX) & 0xFFFFFFFFL;
        long y = ((long) regionY) & 0xFFFFFFFFL;
        return (x << 32) | y;
    }

    private static int resolveFishingClickRegionSizePx() {
        try {
            Field field = FishingCommandService.class.getDeclaredField("RECENT_CLICK_REGION_CELL_SIZE_PX");
            field.setAccessible(true);
            return Math.max(1, field.getInt(null));
        } catch (Exception ignored) {
            return 18;
        }
    }

    private static NPC npcProxy(int index, int id, WorldPoint worldPoint) {
        return (NPC) Proxy.newProxyInstance(
            NPC.class.getClassLoader(),
            new Class<?>[] {NPC.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("getIndex".equals(name)) {
                    return index;
                }
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

    private static Player playerProxy(WorldPoint worldPoint) {
        return playerProxy(worldPoint, -1, null);
    }

    private static Player playerProxy(WorldPoint worldPoint, int animation, Object interacting) {
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class, Actor.class},
            (proxy, method, args) -> {
                if ("getWorldLocation".equals(method.getName())) {
                    return worldPoint;
                }
                if ("getAnimation".equals(method.getName())) {
                    return animation;
                }
                if ("getInteracting".equals(method.getName())) {
                    return interacting;
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

    private static final class TestHost implements FishingCommandService.Host {
        private Optional<NPC> nearestTarget = Optional.empty();
        private List<NPC> nearestTargets = new ArrayList<>();
        private final Map<Integer, Point> npcClickPoints = new HashMap<>();
        private final Map<Integer, List<Point>> npcClickPointSequences = new HashMap<>();
        private final Map<Integer, Integer> npcClickPointSequenceIndexes = new HashMap<>();
        private long lastDropSweepSessionEndedAtMs = 0L;
        private long fishingOutcomeWaitUntilMs = 0L;
        private long lastDispatchAtMs = 0L;
        private WorldPoint lastDispatchWorldPoint = null;
        private int lastDispatchNpcIndex = -1;
        private int lockedTargetIndex = -1;
        private boolean levelUpPromptVisible = false;
        private boolean dismissLevelUpPromptResult = false;
        private int dismissLevelUpPromptAttempts = 0;
        private Player currentPlayer = null;

        private void setNpcClickPoint(int npcIndex, Point point) {
            if (point == null) {
                npcClickPoints.remove(npcIndex);
                return;
            }
            npcClickPoints.put(npcIndex, new Point(point));
        }

        private void setNpcClickPointSequence(int npcIndex, List<Point> points) {
            if (points == null || points.isEmpty()) {
                npcClickPointSequences.remove(npcIndex);
                npcClickPointSequenceIndexes.remove(npcIndex);
                return;
            }
            List<Point> copy = new ArrayList<>();
            for (Point point : points) {
                if (point != null) {
                    copy.add(new Point(point));
                }
            }
            npcClickPointSequences.put(npcIndex, copy);
            npcClickPointSequenceIndexes.put(npcIndex, 0);
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
        public void extendFishingRetryWindow() {
            // No-op.
        }

        @Override
        public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
            return new ClickMotionSettings(0.0, 4L, 6L);
        }

        @Override
        public Player currentPlayer() {
            return currentPlayer;
        }

        @Override
        public boolean isFishingLevelUpPromptVisible() {
            return levelUpPromptVisible;
        }

        @Override
        public boolean dismissFishingLevelUpPrompt() {
            dismissLevelUpPromptAttempts++;
            return dismissLevelUpPromptResult;
        }

        @Override
        public void clearFishingOutcomeWaitWindow() {
            // No-op.
        }

        @Override
        public void clearFishingTargetAttempt() {
            // No-op.
        }

        @Override
        public Set<Integer> parsePreferredNpcIds(JsonElement targetNpcIdElement, JsonElement targetNpcIdsElement) {
            return Collections.emptySet();
        }

        @Override
        public long fishingOutcomeWaitUntilMs() {
            return fishingOutcomeWaitUntilMs;
        }

        @Override
        public int fishingLastAttemptNpcIndex() {
            return -1;
        }

        @Override
        public WorldPoint fishingLastAttemptWorldPoint() {
            return null;
        }

        @Override
        public long fishingApproachWaitUntilMs() {
            return 0L;
        }

        @Override
        public Optional<NPC> resolveLockedFishingTarget(Set<Integer> preferredNpcIds) {
            return Optional.empty();
        }

        @Override
        public Optional<NPC> resolveNearestFishingTarget(Player local, Set<Integer> preferredNpcIds) {
            if (nearestTarget.isPresent()) {
                return nearestTarget;
            }
            if (nearestTargets == null || nearestTargets.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(nearestTargets.get(0));
        }

        @Override
        public List<NPC> resolveNearestFishingTargets(Player local, Set<Integer> preferredNpcIds, int maxTargets) {
            if (maxTargets <= 0) {
                return Collections.emptyList();
            }
            if (nearestTargets != null && !nearestTargets.isEmpty()) {
                int limit = Math.min(maxTargets, nearestTargets.size());
                return new ArrayList<>(nearestTargets.subList(0, limit));
            }
            if (nearestTarget.isEmpty()) {
                return Collections.emptyList();
            }
            return List.of(nearestTarget.get());
        }

        @Override
        public void lockFishingTarget(NPC npc) {
            lockedTargetIndex = npc == null ? -1 : npc.getIndex();
        }

        @Override
        public void clearFishingInteractionWindows() {
            // No-op.
        }

        @Override
        public void clearFishingInteractionWindowsPreserveDispatchSignal() {
            // No-op.
        }

        @Override
        public Point resolveNpcClickPoint(NPC npc) {
            if (npc == null) {
                return null;
            }
            List<Point> sequence = npcClickPointSequences.get(npc.getIndex());
            if (sequence != null && !sequence.isEmpty()) {
                int index = npcClickPointSequenceIndexes.getOrDefault(npc.getIndex(), 0);
                int bounded = Math.min(index, sequence.size() - 1);
                npcClickPointSequenceIndexes.put(npc.getIndex(), index + 1);
                Point sequencePoint = sequence.get(bounded);
                return sequencePoint == null ? null : new Point(sequencePoint);
            }
            Point point = npcClickPoints.get(npc.getIndex());
            return point == null ? null : new Point(point);
        }

        @Override
        public boolean isUsableCanvasPoint(Point point) {
            return point != null && point.x >= 0 && point.y >= 0;
        }

        @Override
        public void clearFishingTargetLock() {
            // No-op.
        }

        @Override
        public void rememberInteractionAnchorForNpc(NPC npc, Point point) {
            // No-op.
        }

        @Override
        public long fishingLastDispatchAtMs() {
            return lastDispatchAtMs;
        }

        @Override
        public WorldPoint fishingLastDispatchWorldPoint() {
            return lastDispatchWorldPoint;
        }

        @Override
        public int fishingLastDispatchNpcIndex() {
            return lastDispatchNpcIndex;
        }

        @Override
        public long fishingSameTargetReclickCooldownMs() {
            return 1200L;
        }

        @Override
        public MotorDispatchResult dispatchFishingMoveAndClick(Point canvasPoint, ClickMotionSettings motion) {
            return new MotorDispatchResult(1L, MotorDispatchStatus.COMPLETE, "complete");
        }

        @Override
        public void noteInteractionActivityNow() {
            // No-op.
        }

        @Override
        public void noteFishingTargetAttempt(Player local, NPC targetNpc) {
            // No-op.
        }

        @Override
        public void noteFishingDispatchAttempt(NPC targetNpc, long now) {
            // No-op.
        }

        @Override
        public void beginFishingOutcomeWaitWindow() {
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
            return RuntimeDecision.accept(reason, details);
        }

        @Override
        public RuntimeDecision reject(String reason) {
            return RuntimeDecision.reject(reason);
        }
    }
}
