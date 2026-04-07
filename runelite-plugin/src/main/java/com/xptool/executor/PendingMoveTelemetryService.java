package com.xptool.executor;

import com.google.gson.JsonObject;
import java.awt.Point;
import java.util.function.Function;

final class PendingMoveTelemetryService {
    @FunctionalInterface
    interface DetailsBuilder {
        JsonObject build(Object... kvPairs);
    }

    @FunctionalInterface
    interface EventEmitter {
        void emit(String reason, JsonObject details);
    }

    private final DetailsBuilder detailsBuilder;
    private final EventEmitter eventEmitter;
    private final Function<Point, Point> currentPointerLocationOr;

    private double remainingDistanceSumPx = 0.0;
    private long remainingDistanceSamples = 0L;
    private double remainingDistanceMinPx = Double.POSITIVE_INFINITY;
    private double remainingDistanceMaxPx = 0.0;
    private int pendingMoveTicksAliveMax = 0;
    private int blockedLastTick = Integer.MIN_VALUE;
    private String blockedLastReason = "";
    private Point blockedLastTarget = null;

    PendingMoveTelemetryService(
        DetailsBuilder detailsBuilder,
        EventEmitter eventEmitter,
        Function<Point, Point> currentPointerLocationOr
    ) {
        this.detailsBuilder = detailsBuilder;
        this.eventEmitter = eventEmitter;
        this.currentPointerLocationOr = currentPointerLocationOr;
    }

    void notePendingMoveAge(PendingMouseMove pendingMove, int currentTick) {
        if (pendingMove == null) {
            return;
        }
        pendingMove.observeTick(currentTick);
        pendingMoveTicksAliveMax = Math.max(pendingMoveTicksAliveMax, pendingMove.ticksAlive);
    }

    void notePendingMoveRemainingDistance(PendingMouseMove pendingMove) {
        if (pendingMove == null || pendingMove.to == null) {
            return;
        }
        Point pointer = currentPointerLocationOr.apply(pendingMove.to);
        if (pointer == null) {
            return;
        }
        double remainingDistPx = ExecutorCursorMotion.pixelDistance(
            pointer,
            pendingMove.to
        );
        remainingDistanceSumPx += remainingDistPx;
        remainingDistanceSamples++;
        remainingDistanceMinPx = Math.min(remainingDistanceMinPx, remainingDistPx);
        remainingDistanceMaxPx = Math.max(remainingDistanceMaxPx, remainingDistPx);
    }

    void notePendingMoveBlocked(PendingMouseMove pendingMove, String reason, int tick) {
        Point target = pendingMove == null ? null : pendingMove.to;
        String normalizedReason = ExecutorValueParsers.safeString(reason);
        if (tick == blockedLastTick
            && normalizedReason.equals(blockedLastReason)
            && pointsEqual(target, blockedLastTarget)) {
            return;
        }
        blockedLastTick = tick;
        blockedLastReason = normalizedReason;
        blockedLastTarget = target == null ? null : new Point(target);
        emitOffscreenPendingMoveEvent("offscreen_pending_move_blocked", pendingMove, normalizedReason, tick, null);
    }

    void notePendingMoveAdvanced(PendingMouseMove pendingMove, int tick, Point after) {
        emitOffscreenPendingMoveEvent("offscreen_pending_move_advanced", pendingMove, "advanced", tick, after);
    }

    void notePendingMoveCleared(PendingMouseMove pendingMove, String reason, int tick) {
        emitOffscreenPendingMoveEvent(
            "offscreen_pending_move_cleared",
            pendingMove,
            ExecutorValueParsers.safeString(reason),
            tick,
            null
        );
    }

    double averageRemainingDistancePx() {
        if (remainingDistanceSamples == 0L) {
            return 0.0;
        }
        return remainingDistanceSumPx / (double) remainingDistanceSamples;
    }

    double minRemainingDistancePx() {
        if (remainingDistanceSamples == 0L) {
            return 0.0;
        }
        return remainingDistanceMinPx;
    }

    double maxRemainingDistancePx() {
        if (remainingDistanceSamples == 0L) {
            return 0.0;
        }
        return remainingDistanceMaxPx;
    }

    int pendingMoveTicksAliveMax() {
        return pendingMoveTicksAliveMax;
    }

    void resetDebugCounters() {
        remainingDistanceSumPx = 0.0;
        remainingDistanceSamples = 0L;
        remainingDistanceMinPx = Double.POSITIVE_INFINITY;
        remainingDistanceMaxPx = 0.0;
        pendingMoveTicksAliveMax = 0;
        blockedLastTick = Integer.MIN_VALUE;
        blockedLastReason = "";
        blockedLastTarget = null;
    }

    private void emitOffscreenPendingMoveEvent(
        String stageReason,
        PendingMouseMove pendingMove,
        String reason,
        int tick,
        Point pointerAfter
    ) {
        Point target = pendingMove == null ? null : pendingMove.to;
        Point safeFrom = pendingMove == null ? null : pendingMove.from;
        eventEmitter.emit(
            reason,
            detailsBuilder.build(
                "stageReason", ExecutorValueParsers.safeString(stageReason),
                "tick", tick,
                "reasonCode", ExecutorValueParsers.safeString(reason),
                "owner", pendingMove == null ? "" : ExecutorValueParsers.safeString(pendingMove.owner),
                "stepIndex", pendingMove == null ? -1 : pendingMove.stepIndex,
                "totalSteps", pendingMove == null ? -1 : pendingMove.totalSteps,
                "nextAllowedTick", pendingMove == null ? -1 : pendingMove.nextAllowedTick,
                "ticksAlive", pendingMove == null ? -1 : pendingMove.ticksAlive,
                "fromX", safeFrom == null ? -1 : safeFrom.x,
                "fromY", safeFrom == null ? -1 : safeFrom.y,
                "targetX", target == null ? -1 : target.x,
                "targetY", target == null ? -1 : target.y,
                "pointerAfterX", pointerAfter == null ? -1 : pointerAfter.x,
                "pointerAfterY", pointerAfter == null ? -1 : pointerAfter.y
            )
        );
    }

    private static boolean pointsEqual(Point left, Point right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.x == right.x && left.y == right.y;
    }
}
