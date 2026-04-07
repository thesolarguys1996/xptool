package com.xptool.executor;

import com.google.gson.JsonObject;
import java.awt.Point;
import java.awt.Robot;
import java.util.concurrent.ThreadLocalRandom;

final class IdleOffscreenMoveEngine {
    interface Host {
        PendingMouseMove pendingMouseMove();

        void setPendingMouseMove(PendingMouseMove move);

        void clearPendingMouseMove();

        boolean isPendingMouseMoveOwnerValid(PendingMouseMove move);

        Point currentPointerLocationOr(Point fallback);

        Robot getOrCreateRobot();

        int currentExecutorTick();

        boolean isCursorNearScreenPoint(Point point, double tolerancePx);

        void emitIdleEvent(String reason, JsonObject details);
    }

    static final class Config {
        final String owner;
        final double targetTolerancePx;
        final int minSteps;
        final int maxSteps;
        final boolean humanizedTimingEnabled;

        Config(
            String owner,
            double targetTolerancePx,
            int minSteps,
            int maxSteps,
            boolean humanizedTimingEnabled
        ) {
            this.owner = owner == null ? "" : owner;
            this.targetTolerancePx = Math.max(0.0, targetTolerancePx);
            this.minSteps = Math.max(1, minSteps);
            this.maxSteps = Math.max(this.minSteps, maxSteps);
            this.humanizedTimingEnabled = humanizedTimingEnabled;
        }
    }

    private final Host host;
    private final Config config;

    IdleOffscreenMoveEngine(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    boolean scheduleIdleOffscreenMove(Point screenTarget) {
        JsonObject scheduleDetails = new JsonObject();
        scheduleDetails.addProperty("screenTargetNull", screenTarget == null);
        scheduleDetails.addProperty("ownerBlank", config.owner.isBlank());
        scheduleDetails.addProperty("targetTolerancePx", config.targetTolerancePx);
        scheduleDetails.addProperty("minSteps", config.minSteps);
        scheduleDetails.addProperty("maxSteps", config.maxSteps);
        if (screenTarget != null) {
            scheduleDetails.addProperty("targetX", screenTarget.x);
            scheduleDetails.addProperty("targetY", screenTarget.y);
        }
        if (screenTarget == null) {
            scheduleDetails.addProperty("failureReason", "screen_target_null");
            host.emitIdleEvent("offscreen_schedule_failed", scheduleDetails);
            return false;
        }
        if (config.owner.isBlank()) {
            scheduleDetails.addProperty("failureReason", "owner_blank");
            host.emitIdleEvent("offscreen_schedule_failed", scheduleDetails);
            return false;
        }
        PendingMouseMove existing = host.pendingMouseMove();
        if (existing != null) {
            scheduleDetails.addProperty("existingPendingMovePresent", true);
            scheduleDetails.addProperty("existingPendingMoveOwner", existing.owner);
            if (existing.matchesTarget(screenTarget)) {
                scheduleDetails.addProperty("existingPendingMoveMatchesTarget", true);
                scheduleDetails.addProperty("pendingMoveCreated", false);
                scheduleDetails.addProperty("result", "existing_move_matches_target");
                host.emitIdleEvent("offscreen_schedule_success", scheduleDetails);
                return true;
            }
            boolean sameOwner = config.owner.equals(existing.owner);
            scheduleDetails.addProperty("existingPendingMoveMatchesTarget", false);
            scheduleDetails.addProperty("existingPendingMoveSameOwner", sameOwner);
            boolean existingOwnerValid = host.isPendingMouseMoveOwnerValid(existing);
            if (sameOwner || !existingOwnerValid) {
                scheduleDetails.addProperty("existingPendingMoveOwnerValid", existingOwnerValid);
                host.clearPendingMouseMove();
            } else {
                scheduleDetails.addProperty("existingPendingMoveOwnerValid", true);
                scheduleDetails.addProperty("existingPendingMoveBlockedScheduling", true);
                scheduleDetails.addProperty("failureReason", "existing_pending_move_blocked");
                host.emitIdleEvent("offscreen_schedule_failed", scheduleDetails);
                return false;
            }
        } else {
            scheduleDetails.addProperty("existingPendingMovePresent", false);
        }
        if (host.isCursorNearScreenPoint(screenTarget, config.targetTolerancePx)) {
            scheduleDetails.addProperty("cursorAlreadyNearTarget", true);
            scheduleDetails.addProperty("pendingMoveCreated", false);
            scheduleDetails.addProperty("result", "cursor_already_near_target");
            host.emitIdleEvent("offscreen_schedule_success", scheduleDetails);
            return true;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            scheduleDetails.addProperty("robotNull", true);
            scheduleDetails.addProperty("failureReason", "robot_unavailable");
            host.emitIdleEvent("offscreen_schedule_failed", scheduleDetails);
            return false;
        }
        scheduleDetails.addProperty("robotNull", false);
        Point from = host.currentPointerLocationOr(screenTarget);
        if (from != null) {
            scheduleDetails.addProperty("fromX", from.x);
            scheduleDetails.addProperty("fromY", from.y);
        }
        int steps = resolveSteps(from, screenTarget);
        host.setPendingMouseMove(
            new PendingMouseMove(
                robot,
                from,
                screenTarget,
                null,
                config.owner,
                steps,
                host.currentExecutorTick()
            )
        );
        scheduleDetails.addProperty("pendingMoveCreated", true);
        scheduleDetails.addProperty("scheduledSteps", steps);
        scheduleDetails.addProperty("result", "pending_move_created");
        host.emitIdleEvent("offscreen_schedule_success", scheduleDetails);
        return true;
    }

    private int resolveSteps(Point from, Point to) {
        int computed = (int) Math.round(pixelDistance(from, to) / 13.0) + 9;
        if (config.humanizedTimingEnabled) {
            double scale = sampleHumanizedStepScale();
            int additiveJitter = ThreadLocalRandom.current().nextInt(-3, 7);
            computed = (int) Math.round((double) computed * scale) + additiveJitter;
        }
        return Math.max(config.minSteps, Math.min(config.maxSteps, computed));
    }

    private static double sampleHumanizedStepScale() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 24) {
            return ThreadLocalRandom.current().nextDouble(0.74, 0.94);
        }
        if (roll < 78) {
            return ThreadLocalRandom.current().nextDouble(0.90, 1.18);
        }
        return ThreadLocalRandom.current().nextDouble(1.12, 1.42);
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return 0.0;
        }
        double dx = (double) b.x - a.x;
        double dy = (double) b.y - a.y;
        return Math.hypot(dx, dy);
    }
}
