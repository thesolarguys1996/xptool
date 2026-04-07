package com.xptool.executor;

import com.google.gson.JsonObject;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class IdleActionPatternDetector {
    static final String REASON_REPEAT_AVOIDED = "idle_repeat_avoided";
    static final String REASON_PATTERN_DETECTED = "idle_pattern_repetition_detected";

    private static final int MAX_RECENT_ACTIONS = 8;
    private static final int MIN_REPEAT_AVOIDED_EMIT_INTERVAL_TICKS = 8;
    private static final int MIN_PATTERN_EMIT_INTERVAL_TICKS = 22;

    private final ArrayDeque<String> recentActions = new ArrayDeque<>();
    private final Map<String, Integer> lastEmitTickByKey = new HashMap<>();

    List<DetectionEvent> onMoveExecuted(
        int tick,
        String actionReason,
        Point target,
        int blockedRepeatCount,
        boolean usedBlockedFallback
    ) {
        List<DetectionEvent> events = new ArrayList<>(2);
        String reason = normalizeActionReason(actionReason);
        if (reason.isEmpty()) {
            return events;
        }

        if (blockedRepeatCount > 0
            && shouldEmit(
                tick,
                "repeat_avoided:" + reason,
                MIN_REPEAT_AVOIDED_EMIT_INTERVAL_TICKS
            )) {
            JsonObject details = targetDetails(target);
            details.addProperty("tick", tick);
            details.addProperty("actionReason", reason);
            details.addProperty("blockedRepeatCount", blockedRepeatCount);
            details.addProperty("usedBlockedFallback", usedBlockedFallback);
            events.add(new DetectionEvent(REASON_REPEAT_AVOIDED, details));
        }

        remember(reason);

        int consecutive = trailingConsecutiveCount(reason);
        if (consecutive >= 3
            && shouldEmit(
                tick,
                "pattern_consecutive:" + reason,
                MIN_PATTERN_EMIT_INTERVAL_TICKS
            )) {
            JsonObject details = targetDetails(target);
            details.addProperty("tick", tick);
            details.addProperty("patternType", "consecutive_reason");
            details.addProperty("actionReason", reason);
            details.addProperty("repetitionCount", consecutive);
            events.add(new DetectionEvent(REASON_PATTERN_DETECTED, details));
        } else {
            List<String> alternating = trailingAlternatingPair();
            if (!alternating.isEmpty()) {
                String first = alternating.get(0);
                String second = alternating.get(1);
                String key = "pattern_abab:" + first + ":" + second;
                if (shouldEmit(tick, key, MIN_PATTERN_EMIT_INTERVAL_TICKS)) {
                    JsonObject details = targetDetails(target);
                    details.addProperty("tick", tick);
                    details.addProperty("patternType", "alternating_pair");
                    details.addProperty("firstReason", first);
                    details.addProperty("secondReason", second);
                    details.addProperty("sequence", first + "," + second + "," + first + "," + second);
                    events.add(new DetectionEvent(REASON_PATTERN_DETECTED, details));
                }
            }
        }

        return events;
    }

    void reset() {
        recentActions.clear();
        lastEmitTickByKey.clear();
    }

    private void remember(String reason) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        recentActions.addLast(reason);
        while (recentActions.size() > MAX_RECENT_ACTIONS) {
            recentActions.removeFirst();
        }
    }

    private int trailingConsecutiveCount(String reason) {
        if (reason == null || reason.isBlank() || recentActions.isEmpty()) {
            return 0;
        }
        int streak = 0;
        Object[] items = recentActions.toArray();
        for (int i = items.length - 1; i >= 0; i--) {
            String item = String.valueOf(items[i]);
            if (reason.equals(item)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private List<String> trailingAlternatingPair() {
        if (recentActions.size() < 4) {
            return List.of();
        }
        Object[] items = recentActions.toArray();
        int n = items.length;
        String r1 = String.valueOf(items[n - 4]);
        String r2 = String.valueOf(items[n - 3]);
        String r3 = String.valueOf(items[n - 2]);
        String r4 = String.valueOf(items[n - 1]);
        if (r1.isBlank() || r2.isBlank()) {
            return List.of();
        }
        if (r1.equals(r3) && r2.equals(r4) && !r1.equals(r2)) {
            return List.of(r1, r2);
        }
        return List.of();
    }

    private boolean shouldEmit(int tick, String key, int minIntervalTicks) {
        if (key == null || key.isBlank()) {
            return false;
        }
        int interval = Math.max(1, minIntervalTicks);
        Integer lastTick = lastEmitTickByKey.get(key);
        if (lastTick != null && elapsedTicksSince(tick, lastTick) < interval) {
            return false;
        }
        lastEmitTickByKey.put(key, tick);
        return true;
    }

    private static int elapsedTicksSince(int nowTick, int thenTick) {
        long elapsed = (long) nowTick - (long) thenTick;
        if (elapsed <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (elapsed >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) elapsed;
    }

    private static String normalizeActionReason(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static JsonObject targetDetails(Point target) {
        JsonObject out = new JsonObject();
        if (target != null) {
            out.addProperty("targetX", target.x);
            out.addProperty("targetY", target.y);
        }
        return out;
    }

    static final class DetectionEvent {
        final String reason;
        final JsonObject details;

        DetectionEvent(String reason, JsonObject details) {
            this.reason = reason == null ? "" : reason;
            this.details = details == null ? new JsonObject() : details;
        }
    }
}
