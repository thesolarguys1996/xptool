package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DropRuntimeTest {
    @Test
    void defersDropDispatchWhenFishingInventoryFullAfkWindowArmed() throws Exception {
        TestHost host = new TestHost();
        host.dropSweepSessionActive = false;
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        DropRuntime runtime = new DropRuntime(host);

        JsonObject payload = new JsonObject();
        payload.addProperty("itemId", 1003);
        runtime.executeDropItem(payload);

        assertEquals("fishing_inventory_full_afk_armed", host.debugReasons.get(0));
        assertTrue(readBooleanField(runtime, "fishingInventoryFullAfkArmedForSession"));
        assertTrue(readLongField(runtime, "fishingInventoryFullAfkUntilMs") > System.currentTimeMillis());
    }

    @Test
    void keepsCurrentSlotWhenDispatchRepeatGuardWouldHaveRerouted() throws Exception {
        TestHost host = new TestHost();
        int itemId = 1001;
        host.dropSweepNextSlot = 5;
        host.setItemSlots(itemId, 5, 7);
        DropRuntime runtime = new DropRuntime(host);

        Object slotSelection = invokeResolveDispatchSlot(runtime, itemId);

        assertTrue(readBooleanField(slotSelection, "available"));
        assertEquals(5, readIntField(slotSelection, "slot"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_reroute"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_deferred"));
    }

    @Test
    void keepsCurrentSlotWhenDispatchRepeatGuardWouldHaveDeferred() throws Exception {
        TestHost host = new TestHost();
        int itemId = 1002;
        host.dropSweepNextSlot = 5;
        host.setItemSlots(itemId, 5);
        DropRuntime runtime = new DropRuntime(host);

        Object slotSelection = invokeResolveDispatchSlot(runtime, itemId);

        assertTrue(readBooleanField(slotSelection, "available"));
        assertEquals(5, readIntField(slotSelection, "slot"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_reroute"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_deferred"));
    }

    @Test
    void repeatGuardNoLongerSwitchesToSameRowCandidate() throws Exception {
        TestHost host = new TestHost();
        int itemId = 1003;
        host.dropSweepNextSlot = 2;
        host.setItemSlots(itemId, 0, 2, 4);
        DropRuntime runtime = new DropRuntime(host);

        Object slotSelection = invokeResolveDispatchSlot(runtime, itemId);

        assertTrue(readBooleanField(slotSelection, "available"));
        assertEquals(2, readIntField(slotSelection, "slot"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_reroute"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_deferred"));
    }

    @Test
    void repeatGuardNoLongerDefersWhenOnlyGlobalAlternateExists() throws Exception {
        TestHost host = new TestHost();
        int itemId = 1004;
        host.dropSweepNextSlot = 1;
        host.setItemSlots(itemId, 1, 20);
        DropRuntime runtime = new DropRuntime(host);

        Object slotSelection = invokeResolveDispatchSlot(runtime, itemId);

        assertTrue(readBooleanField(slotSelection, "available"));
        assertEquals(1, readIntField(slotSelection, "slot"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_reroute"));
        assertFalse(host.debugReasons.contains("drop_repeat_slot_guard_deferred"));
    }

    @Test
    void dropSessionProfileUsesContiguousSerpentineTraversal() throws Exception {
        TestHost host = new TestHost();
        DropRuntime runtime = new DropRuntime(host);

        String profile = readStringField(runtime, "activeDropTraversalProfile");
        int startSlot = readIntField(runtime, "dropTraversalStartSlot");
        int hopChancePercent = readIntField(runtime, "dropTraversalHopChancePercent");
        int strideSlots = readIntField(runtime, "dropTraversalStrideSlots");

        assertEquals("SERPENTINE", profile);
        assertEquals(0, startSlot);
        assertEquals(1, strideSlots);
        assertEquals(0, hopChancePercent);
    }

    @Test
    void startDropSessionStartsAtFirstPresentSlotAcrossContexts() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.WOODCUTTING;
        int itemId = 2123;
        host.setItemSlots(itemId, 6, 7, 12);
        DropRuntime runtime = new DropRuntime(host);

        JsonObject payload = new JsonObject();
        payload.addProperty("itemId", itemId);
        runtime.executeStartDropSession(payload);

        assertEquals(6, host.dropSweepNextSlot);
    }

    @Test
    void fishingContextForcesSerpentineAndStartsAtFirstPresentSlot() throws Exception {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.FISHING;
        int itemId = 2011;
        host.setItemSlots(itemId, 5, 6, 9);
        DropRuntime runtime = new DropRuntime(host);

        JsonObject payload = new JsonObject();
        payload.addProperty("itemId", itemId);
        runtime.executeStartDropSession(payload);

        assertEquals("SERPENTINE", readStringField(runtime, "activeDropTraversalProfile"));
        assertEquals(1, readIntField(runtime, "dropTraversalStrideSlots"));
        assertEquals(0, readIntField(runtime, "dropTraversalHopChancePercent"));
        assertEquals(5, host.dropSweepNextSlot);
    }

    @Test
    void traversalPrefersSerpentineWhenSameRowItemsRemain() throws Exception {
        TestHost host = new TestHost();
        int itemId = 2001;
        host.setItemSlots(itemId, 0, 1, 2, 3, 4);
        DropRuntime runtime = new DropRuntime(host);

        setEnumField(runtime, "activeDropTraversalProfile", "COLUMN_BIASED");
        setIntField(runtime, "dropTraversalStrideSlots", 2);
        setIntField(runtime, "dropTraversalHopChancePercent", 0);

        int nextSlot = invokeNextDropSweepStartSlot(runtime, 0, itemId);

        assertEquals(1, nextSlot);
    }

    @Test
    void advanceTickDefersSessionStopWhenNoSlotUnavailableIsTransient() {
        TestHost host = new TestHost();
        int itemId = 1519;
        host.dropSweepSessionActive = true;
        host.dropSweepItemId = itemId;
        host.dropSweepNextSlot = 9;
        host.setItemSlots(itemId, 10, 11, 12);
        host.forceNextFindInventoryEmpty(2);
        DropRuntime runtime = new DropRuntime(host);

        runtime.advanceTick(200);

        assertTrue(host.dropSweepSessionActive);
        assertTrue(host.debugReasons.contains("drop_session_slot_unavailable_deferred"));
    }

    @Test
    void advanceTickStopsSessionAfterConfirmedNoSlotStreak() {
        TestHost host = new TestHost();
        int itemId = 1519;
        host.dropSweepSessionActive = true;
        host.dropSweepItemId = itemId;
        host.dropSweepNextSlot = 9;
        host.setItemSlots(itemId, 10, 11, 12);
        host.forceNextFindInventoryEmpty(4);
        DropRuntime runtime = new DropRuntime(host);

        runtime.advanceTick(200);
        assertTrue(host.dropSweepSessionActive);

        runtime.advanceTick(201);
        assertFalse(host.dropSweepSessionActive);
        assertTrue(host.debugReasons.contains("drop_session_slot_unavailable_confirmed_stop"));
    }

    @Test
    void traversalSkipTelemetryEmitsWhenPathSkipsPresentSlots() throws Exception {
        TestHost host = new TestHost();
        int itemId = 2501;
        host.setItemSlots(itemId, 1, 7);
        DropRuntime runtime = new DropRuntime(host);

        invokeEmitTraversalSkipTelemetry(runtime, itemId, 2, 1, "post_confirm_plan");

        assertTrue(host.debugReasons.contains("drop_traversal_skip_detected"));
    }

    @Test
    void traversalSkipTelemetryDoesNotEmitWhenNoPresentSlotsSkipped() throws Exception {
        TestHost host = new TestHost();
        int itemId = 2502;
        host.setItemSlots(itemId, 1);
        DropRuntime runtime = new DropRuntime(host);

        invokeEmitTraversalSkipTelemetry(runtime, itemId, 2, 1, "post_confirm_plan");

        assertFalse(host.debugReasons.contains("drop_traversal_skip_detected"));
    }

    @Test
    void dispatchSelectionCorrectsLinearForwardScanToSerpentineOddRowAnchor() throws Exception {
        TestHost host = new TestHost();
        int itemId = 2601;
        host.dropSweepNextSlot = 6;
        host.setItemSlots(itemId, 4, 5, 8);
        DropRuntime runtime = new DropRuntime(host);

        Object slotSelection = invokeResolveDispatchSlot(runtime, itemId);

        assertTrue(readBooleanField(slotSelection, "available"));
        assertEquals(5, readIntField(slotSelection, "slot"));
        assertTrue(host.debugReasons.contains("drop_dispatch_serpentine_corrected"));
        assertFalse(host.debugReasons.contains("drop_traversal_skip_detected"));
    }

    @Test
    void dispatchSelectionCorrectsSameRowRightJumpOnOddRow() throws Exception {
        TestHost host = new TestHost();
        int itemId = 2602;
        host.dropSweepNextSlot = 15;
        host.setItemSlots(itemId, 12, 13, 14, 16);
        DropRuntime runtime = new DropRuntime(host);

        Object slotSelection = invokeResolveDispatchSlot(runtime, itemId);

        assertTrue(readBooleanField(slotSelection, "available"));
        assertEquals(14, readIntField(slotSelection, "slot"));
        assertTrue(host.debugReasons.contains("drop_dispatch_serpentine_corrected"));
        assertFalse(host.debugReasons.contains("drop_traversal_skip_detected"));
    }

    private static Object invokeResolveDispatchSlot(DropRuntime runtime, int itemId) throws Exception {
        Method method = DropRuntime.class.getDeclaredMethod("resolveDispatchSlot", int.class);
        method.setAccessible(true);
        return method.invoke(runtime, itemId);
    }

    private static int invokeNextDropSweepStartSlot(DropRuntime runtime, int droppedSlot, int itemId) throws Exception {
        Method method = DropRuntime.class.getDeclaredMethod("nextDropSweepStartSlot", int.class, int.class);
        method.setAccessible(true);
        return (int) method.invoke(runtime, droppedSlot, itemId);
    }

    private static void invokeEmitTraversalSkipTelemetry(
        DropRuntime runtime,
        int itemId,
        int fromSlot,
        int toSlot,
        String source
    ) throws Exception {
        Method method = DropRuntime.class.getDeclaredMethod(
            "emitTraversalSkipTelemetryIfNeeded",
            int.class,
            int.class,
            int.class,
            String.class
        );
        method.setAccessible(true);
        method.invoke(runtime, itemId, fromSlot, toSlot, source);
    }

    private static boolean readBooleanField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static int readIntField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static long readLongField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getLong(target);
    }

    private static String readStringField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(target);
        return value == null ? "" : String.valueOf(value);
    }

    private static void setIntField(Object target, String fieldName, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setEnumField(Object target, String fieldName, String enumName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Class enumType = field.getType().asSubclass(Enum.class);
        Object enumValue = Enum.valueOf(enumType, enumName);
        field.set(target, enumValue);
    }

    private static final class TestHost implements DropRuntime.Host {
        private boolean dropSweepSessionActive = true;
        private int dropSweepItemId = -1;
        private int dropSweepNextSlot = 0;
        private int dropSweepLastDispatchTick = Integer.MIN_VALUE;
        private int dropSweepDispatchFailStreak = 0;
        private boolean dropSweepAwaitingFirstCursorSync = false;
        private final Map<Integer, Set<Integer>> itemSlots = new HashMap<>();
        private final List<String> debugReasons = new ArrayList<>();
        private IdleSkillContext idleSkillContext = IdleSkillContext.GLOBAL;
        private FishingIdleMode fishingIdleMode = FishingIdleMode.STANDARD;
        private int forcedEmptyFindInventoryCallsRemaining = 0;

        private void setItemSlots(int itemId, int... slots) {
            Set<Integer> normalized = new HashSet<>();
            if (slots != null) {
                for (int slot : slots) {
                    if (slot >= 0 && slot < 28) {
                        normalized.add(slot);
                    }
                }
            }
            itemSlots.put(itemId, normalized);
            dropSweepItemId = itemId;
        }

        private void forceNextFindInventoryEmpty(int calls) {
            forcedEmptyFindInventoryCallsRemaining = Math.max(0, calls);
        }

        @Override
        public int currentExecutorTick() {
            return 100;
        }

        @Override
        public int currentPlayerAnimation() {
            return -1;
        }

        @Override
        public boolean isAnimationActive(int animation) {
            return false;
        }

        @Override
        public boolean isDropSweepSessionActive() {
            return dropSweepSessionActive;
        }

        @Override
        public int dropSweepItemId() {
            return dropSweepItemId;
        }

        @Override
        public int dropSweepNextSlot() {
            return dropSweepNextSlot;
        }

        @Override
        public int dropSweepLastDispatchTick() {
            return dropSweepLastDispatchTick;
        }

        @Override
        public int dropSweepDispatchFailStreak() {
            return dropSweepDispatchFailStreak;
        }

        @Override
        public boolean dropSweepAwaitingFirstCursorSync() {
            return dropSweepAwaitingFirstCursorSync;
        }

        @Override
        public void setDropSweepNextSlot(int slot) {
            this.dropSweepNextSlot = slot;
        }

        @Override
        public void setDropSweepLastDispatchTick(int tick) {
            this.dropSweepLastDispatchTick = tick;
        }

        @Override
        public void setDropSweepAwaitingFirstCursorSync(boolean awaiting) {
            this.dropSweepAwaitingFirstCursorSync = awaiting;
        }

        @Override
        public void setDropSweepProgressCheckPending(boolean pending) {
            // No-op.
        }

        @Override
        public void beginDropSweepSession(int itemId, Set<Integer> itemIds) {
            this.dropSweepSessionActive = true;
            this.dropSweepItemId = itemId;
        }

        @Override
        public void endDropSweepSession() {
            this.dropSweepSessionActive = false;
        }

        @Override
        public boolean updateDropSweepProgressState(int itemId) {
            return true;
        }

        @Override
        public boolean noteDropSweepDispatchFailure() {
            dropSweepDispatchFailStreak++;
            return true;
        }

        @Override
        public void noteDropSweepDispatchSuccess() {
            dropSweepDispatchFailStreak = 0;
        }

        @Override
        public Optional<Integer> findInventorySlotFrom(int itemId, int startSlot) {
            if (forcedEmptyFindInventoryCallsRemaining > 0) {
                forcedEmptyFindInventoryCallsRemaining--;
                return Optional.empty();
            }
            Set<Integer> slots = itemSlots.get(itemId);
            if (slots == null || slots.isEmpty()) {
                return Optional.empty();
            }
            int start = Math.floorMod(startSlot, 28);
            for (int offset = 0; offset < 28; offset++) {
                int candidate = Math.floorMod(start + offset, 28);
                if (slots.contains(candidate)) {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<Point> resolveInventorySlotPoint(int slot) {
            return Optional.of(new Point(100 + slot, 200));
        }

        @Override
        public Optional<Point> resolveInventorySlotBasePoint(int slot) {
            return Optional.of(new Point(100 + slot, 200));
        }

        @Override
        public Optional<Point> centerOfDropSweepRegionCanvas() {
            return Optional.empty();
        }

        @Override
        public boolean isCursorNearDropTarget(Point canvasPoint) {
            return false;
        }

        @Override
        public MotorHandle scheduleDropMoveGesture(Point canvasPoint) {
            return null;
        }

        @Override
        public boolean acquireOrRenewDropMotorOwner() {
            return true;
        }

        @Override
        public boolean isLoggedInAndBankClosed() {
            return true;
        }

        @Override
        public boolean dispatchInventoryDropAction(int slot, int expectedItemId, Point preparedCanvasPoint) {
            return false;
        }

        @Override
        public void applyDropPerceptionDelay() {
            // No-op.
        }

        @Override
        public void incrementClicksDispatched() {
            // No-op.
        }

        @Override
        public FatigueSnapshot fatigueSnapshot() {
            return FatigueSnapshot.neutral();
        }

        @Override
        public void onDropCadenceProfileSelected(String profileKey) {
            // No-op.
        }

        @Override
        public void onIdleCadenceTuningSelected(IdleCadenceTuning tuning) {
            // No-op.
        }

        @Override
        public IdleSkillContext resolveIdleSkillContext() {
            return idleSkillContext;
        }

        @Override
        public FishingIdleMode resolveFishingIdleMode(IdleSkillContext context) {
            return fishingIdleMode;
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
        public void emitDropDebug(String reason, JsonObject details) {
            debugReasons.add(reason == null ? "" : reason);
        }

        @Override
        public CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject details) {
            return null;
        }

        @Override
        public CommandExecutor.CommandDecision rejectDecision(String reason) {
            return null;
        }
    }
}
