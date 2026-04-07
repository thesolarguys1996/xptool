package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Locale;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

final class WalkCommandService {
    private static final int WALK_TARGET_MATCH_RADIUS_TILES = 1;
    private static final int WALK_TARGET_WALKABLE_ADJUST_RADIUS_TILES = 3;
    private static final long WALK_APPROACH_BASE_WAIT_MS = 460L;
    private static final long WALK_APPROACH_WAIT_PER_TILE_MS = 160L;
    private static final long WALK_APPROACH_MAX_WAIT_MS = 4200L;
    private static final long WALK_SAME_TARGET_RECLICK_BASE_MS = 1300L;
    private static final long WALK_SAME_TARGET_RECLICK_PER_TILE_MS = 240L;
    private static final long WALK_SAME_TARGET_RECLICK_MAX_MS = 4200L;
    private static final double WALK_RETRY_POINT_REPEAT_EXCLUSION_PX = 2.8;
    private static final int WALK_RETRY_POINT_SAMPLE_RADIUS_PX = 6;
    private static final int WALK_RETRY_POINT_SAMPLE_ATTEMPTS = 20;
    private static final String WALK_CLICK_MODE_SCENE = "SCENE";
    private static final String WALK_CLICK_MODE_MINIMAP = "MINIMAP";
    private static final String WALK_CLICK_MODE_MIXED = "MIXED";
    private static final String WALK_CLICK_MODE_DEFAULT = WALK_CLICK_MODE_MIXED;
    private static final int WALK_MINIMAP_MIXED_BASE_CHANCE_PCT = 78;
    private static final int WALK_MINIMAP_MIXED_DISTANCE_PIVOT_TILES = 6;
    private static final int WALK_MINIMAP_MIXED_DISTANCE_STEP_PCT = 1;
    private static final int WALK_MINIMAP_MIXED_MAX_CHANCE_PCT = 92;
    private static final int WALK_MINIMAP_MIXED_MIN_CHANCE_PCT = 62;
    private static final int WALK_REROUTE_HISTORY_SIZE = 6;
    private static final int[][] WALK_SAME_TARGET_REROUTE_OFFSETS = {
        {1, 0}, {0, 1}, {-1, 0}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
        {2, 0}, {0, 2}, {-2, 0}, {0, -2},
        {2, 1}, {1, 2}, {-1, 2}, {-2, 1},
        {-2, -1}, {-1, -2}, {1, -2}, {2, -1},
        {2, 2}, {2, -2}, {-2, 2}, {-2, -2}
    };

    interface Host {
        CommandExecutor.CommandDecision accept(String reason, JsonObject details);

        CommandExecutor.CommandDecision reject(String reason);

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        Player localPlayer();

        Point resolveWorldTileClickPoint(WorldPoint worldPoint);

        Point resolveWorldTileMinimapClickPoint(WorldPoint worldPoint);

        WorldPoint resolveNearestWalkableWorldPoint(WorldPoint localWorldPoint, WorldPoint targetWorldPoint, int maxRadiusTiles);

        boolean isUsableCanvasPoint(Point point);

        MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile);

        MotorProfile buildWalkMoveAndClickProfile(ClickMotionSettings motion);

        void noteInteractionActivityNow();

        void incrementClicksDispatched();
    }

    private final Host host;
    private WorldPoint lastAttemptWorldPoint = null;
    private long approachWaitUntilMs = 0L;
    private WorldPoint lastDispatchWorldPoint = null;
    private long lastDispatchAtMs = 0L;
    private Point lastDispatchCanvasPoint = null;
    private final long[] recentRerouteTargetKeys = new long[WALK_REROUTE_HISTORY_SIZE];
    private int recentRerouteTargetWriteIndex = 0;

    WalkCommandService(Host host) {
        this.host = host;
        for (int i = 0; i < recentRerouteTargetKeys.length; i++) {
            recentRerouteTargetKeys[i] = Long.MIN_VALUE;
        }
    }

    CommandExecutor.CommandDecision executeWalkToWorldPoint(JsonObject payload, MotionProfile motionProfile) {
        JsonObject safePayload = payload == null ? new JsonObject() : payload;
        Player local = host.localPlayer();
        if (local == null || local.getWorldLocation() == null) {
            return host.reject("walk_player_unavailable");
        }

        int targetWorldX = readTargetAxis(safePayload, "targetWorldX", "worldX");
        int targetWorldY = readTargetAxis(safePayload, "targetWorldY", "worldY");
        if (targetWorldX <= 0 || targetWorldY <= 0) {
            return host.reject("walk_target_worldpoint_missing");
        }
        int targetPlane = asInt(safePayload.get("targetPlane"), local.getWorldLocation().getPlane());
        WorldPoint requestedWorldPoint = new WorldPoint(targetWorldX, targetWorldY, targetPlane);
        WorldPoint localWorldPoint = local.getWorldLocation();
        if (localWorldPoint.getPlane() != requestedWorldPoint.getPlane()) {
            return host.accept(
                "walk_target_plane_mismatch",
                host.details(
                    "localPlane", localWorldPoint.getPlane(),
                    "targetPlane", requestedWorldPoint.getPlane(),
                    "targetWorldX", targetWorldX,
                    "targetWorldY", targetWorldY
                )
            );
        }
        WorldPoint targetWorldPoint = host.resolveNearestWalkableWorldPoint(
            localWorldPoint,
            requestedWorldPoint,
            WALK_TARGET_WALKABLE_ADJUST_RADIUS_TILES
        );
        if (targetWorldPoint == null) {
            return host.accept(
                "walk_target_not_walkable",
                host.details(
                    "targetWorldX", targetWorldX,
                    "targetWorldY", targetWorldY,
                    "targetPlane", targetPlane,
                    "localWorldX", localWorldPoint.getX(),
                    "localWorldY", localWorldPoint.getY()
                )
            );
        }
        boolean targetAdjusted = !targetWorldPoint.equals(requestedWorldPoint);
        int resolvedTargetWorldX = targetWorldPoint.getX();
        int resolvedTargetWorldY = targetWorldPoint.getY();
        int resolvedTargetPlane = targetWorldPoint.getPlane();

        int localDistance = localWorldPoint.distanceTo(targetWorldPoint);
        if (localDistance < 0) {
            return host.accept(
                "walk_target_distance_unavailable",
                host.details("targetWorldX", resolvedTargetWorldX, "targetWorldY", resolvedTargetWorldY)
            );
        }
        int arriveDistanceTiles = clamp(asInt(safePayload.get("arriveDistanceTiles"), 0), 0, 12);
        if (localDistance <= arriveDistanceTiles) {
            clearTargetState(targetWorldPoint);
            return host.accept(
                "walk_target_reached",
                host.details(
                    "targetWorldX", resolvedTargetWorldX,
                    "targetWorldY", resolvedTargetWorldY,
                    "targetPlane", resolvedTargetPlane,
                    "arriveDistanceTiles", arriveDistanceTiles,
                    "localDistance", localDistance,
                    "targetAdjusted", targetAdjusted,
                    "requestedTargetWorldX", requestedWorldPoint.getX(),
                    "requestedTargetWorldY", requestedWorldPoint.getY()
                )
            );
        }

        long now = System.currentTimeMillis();
        boolean sameTargetAsAttempt = worldPointsNear(targetWorldPoint, lastAttemptWorldPoint, WALK_TARGET_MATCH_RADIUS_TILES);
        if (sameTargetAsAttempt && now <= approachWaitUntilMs) {
            return host.accept(
                "walk_approaching_target",
                host.details(
                    "targetWorldX", resolvedTargetWorldX,
                    "targetWorldY", resolvedTargetWorldY,
                    "targetPlane", resolvedTargetPlane,
                    "localDistance", localDistance,
                    "waitMsRemaining", Math.max(0L, approachWaitUntilMs - now),
                    "targetAdjusted", targetAdjusted,
                    "requestedTargetWorldX", requestedWorldPoint.getX(),
                    "requestedTargetWorldY", requestedWorldPoint.getY()
                )
            );
        }

        boolean sameTargetAsDispatch =
            worldPointsNear(targetWorldPoint, lastDispatchWorldPoint, WALK_TARGET_MATCH_RADIUS_TILES);
        long sameTargetCooldownMs = sameTargetReclickCooldownMs(localDistance, lastDispatchAtMs, targetWorldPoint);
        long sinceLastDispatchMs = now - lastDispatchAtMs;
        boolean reroutedFromSameTargetCooldown = false;
        if (sameTargetAsDispatch
            && lastDispatchAtMs > 0L
            && sinceLastDispatchMs >= 0L
            && sinceLastDispatchMs < sameTargetCooldownMs) {
            WorldPoint reroutedTargetWorldPoint = resolveAlternateTargetForSameTargetCooldown(
                localWorldPoint,
                targetWorldPoint,
                now
            );
            if (reroutedTargetWorldPoint != null) {
                targetWorldPoint = reroutedTargetWorldPoint;
                localDistance = localWorldPoint.distanceTo(targetWorldPoint);
                resolvedTargetWorldX = targetWorldPoint.getX();
                resolvedTargetWorldY = targetWorldPoint.getY();
                resolvedTargetPlane = targetWorldPoint.getPlane();
                targetAdjusted = !targetWorldPoint.equals(requestedWorldPoint);
                sameTargetAsDispatch =
                    worldPointsNear(targetWorldPoint, lastDispatchWorldPoint, WALK_TARGET_MATCH_RADIUS_TILES);
                sameTargetCooldownMs = sameTargetReclickCooldownMs(localDistance, lastDispatchAtMs, targetWorldPoint);
                sinceLastDispatchMs = now - lastDispatchAtMs;
                reroutedFromSameTargetCooldown = true;
            }
        }
        if (sameTargetAsDispatch
            && lastDispatchAtMs > 0L
            && sinceLastDispatchMs >= 0L
            && sinceLastDispatchMs < sameTargetCooldownMs) {
            return host.accept(
                "walk_same_target_reclick_cooldown",
                host.details(
                    "targetWorldX", resolvedTargetWorldX,
                    "targetWorldY", resolvedTargetWorldY,
                    "targetPlane", resolvedTargetPlane,
                    "localDistance", localDistance,
                    "waitMsRemaining", sameTargetCooldownMs - sinceLastDispatchMs,
                    "targetAdjusted", targetAdjusted,
                    "requestedTargetWorldX", requestedWorldPoint.getX(),
                    "requestedTargetWorldY", requestedWorldPoint.getY(),
                    "reroutedFromSameTargetCooldown", false
                )
            );
        }

        ClickMotionSettings motion = host.resolveClickMotion(safePayload, motionProfile);
        WalkClickSelection clickSelection = selectWalkClickSelection(
            safePayload,
            targetWorldPoint,
            localDistance,
            now
        );
        Point targetCanvasPoint = clickSelection.canvasPoint;
        targetCanvasPoint = resolveRetrySafeCanvasPoint(targetWorldPoint, targetCanvasPoint);
        if (targetCanvasPoint == null || !host.isUsableCanvasPoint(targetCanvasPoint)) {
            return host.accept(
                "walk_target_click_point_unavailable",
                host.details(
                    "targetWorldX", resolvedTargetWorldX,
                    "targetWorldY", resolvedTargetWorldY,
                    "targetPlane", resolvedTargetPlane,
                    "localDistance", localDistance,
                    "walkClickMode", clickSelection.mode,
                    "minimapChancePct", clickSelection.minimapChancePct,
                    "targetAdjusted", targetAdjusted,
                    "requestedTargetWorldX", requestedWorldPoint.getX(),
                    "requestedTargetWorldY", requestedWorldPoint.getY()
                )
            );
        }

        MotorHandle handle = host.scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(targetCanvasPoint),
            MotorGestureType.MOVE_AND_CLICK,
            host.buildWalkMoveAndClickProfile(motion)
        );
        if (handle.status == MotorGestureStatus.COMPLETE) {
            host.noteInteractionActivityNow();
            noteDispatch(targetWorldPoint, now, localDistance, targetCanvasPoint);
            host.incrementClicksDispatched();
            return host.accept(
                "walk_left_click_dispatched",
                host.details(
                    "targetWorldX", resolvedTargetWorldX,
                    "targetWorldY", resolvedTargetWorldY,
                    "targetPlane", resolvedTargetPlane,
                    "localDistance", localDistance,
                    "canvasX", targetCanvasPoint.x,
                    "canvasY", targetCanvasPoint.y,
                    "walkClickMode", clickSelection.mode,
                    "minimapChancePct", clickSelection.minimapChancePct,
                    "motorGestureId", handle.id,
                    "targetAdjusted", targetAdjusted,
                    "requestedTargetWorldX", requestedWorldPoint.getX(),
                    "requestedTargetWorldY", requestedWorldPoint.getY(),
                    "reroutedFromSameTargetCooldown", reroutedFromSameTargetCooldown
                )
            );
        }
        if (handle.status == MotorGestureStatus.FAILED || handle.status == MotorGestureStatus.CANCELLED) {
            return host.reject("walk_motor_gesture_" + host.safeString(handle.reason));
        }
        if (handle.status == MotorGestureStatus.SCHEDULED) {
            noteDispatch(targetWorldPoint, now, localDistance, targetCanvasPoint);
        }
        return host.accept(
            "walk_motor_gesture_in_flight",
            host.details(
                "targetWorldX", resolvedTargetWorldX,
                    "targetWorldY", resolvedTargetWorldY,
                    "targetPlane", resolvedTargetPlane,
                    "localDistance", localDistance,
                    "canvasX", targetCanvasPoint.x,
                    "canvasY", targetCanvasPoint.y,
                    "walkClickMode", clickSelection.mode,
                    "minimapChancePct", clickSelection.minimapChancePct,
                    "motorGestureId", handle.id,
                    "motorStatus", handle.status.name(),
                    "motorReason", handle.reason,
                    "targetAdjusted", targetAdjusted,
                    "requestedTargetWorldX", requestedWorldPoint.getX(),
                    "requestedTargetWorldY", requestedWorldPoint.getY(),
                    "reroutedFromSameTargetCooldown", reroutedFromSameTargetCooldown
                )
            );
    }

    private WorldPoint resolveAlternateTargetForSameTargetCooldown(
        WorldPoint localWorldPoint,
        WorldPoint currentTargetWorldPoint,
        long now
    ) {
        if (localWorldPoint == null || currentTargetWorldPoint == null || WALK_SAME_TARGET_REROUTE_OFFSETS.length <= 0) {
            return null;
        }
        int startIndex = seededRerouteOffsetStartIndex(currentTargetWorldPoint, now);
        int step = seededRerouteOffsetStep(currentTargetWorldPoint, now);
        for (int i = 0; i < WALK_SAME_TARGET_REROUTE_OFFSETS.length; i++) {
            int[] offset = WALK_SAME_TARGET_REROUTE_OFFSETS[
                (startIndex + (i * step)) % WALK_SAME_TARGET_REROUTE_OFFSETS.length
            ];
            if (offset == null || offset.length < 2) {
                continue;
            }
            WorldPoint rerouteCandidate = new WorldPoint(
                currentTargetWorldPoint.getX() + offset[0],
                currentTargetWorldPoint.getY() + offset[1],
                currentTargetWorldPoint.getPlane()
            );
            WorldPoint reroutedWalkable = host.resolveNearestWalkableWorldPoint(localWorldPoint, rerouteCandidate, 0);
            if (reroutedWalkable == null) {
                continue;
            }
            if (worldPointsNear(reroutedWalkable, currentTargetWorldPoint, 0)) {
                continue;
            }
            if (worldPointsNear(reroutedWalkable, lastDispatchWorldPoint, WALK_TARGET_MATCH_RADIUS_TILES)) {
                continue;
            }
            if (wasRecentlyUsedRerouteTarget(reroutedWalkable)) {
                continue;
            }
            rememberRerouteTarget(reroutedWalkable);
            return reroutedWalkable;
        }
        return null;
    }

    private static int seededRerouteOffsetStartIndex(WorldPoint targetWorldPoint, long now) {
        if (targetWorldPoint == null || WALK_SAME_TARGET_REROUTE_OFFSETS.length <= 0) {
            return 0;
        }
        long seed = now;
        seed ^= ((long) targetWorldPoint.getX() * 73856093L);
        seed ^= ((long) targetWorldPoint.getY() * 19349663L);
        seed ^= ((long) targetWorldPoint.getPlane() * 83492791L);
        long positive = seed & Long.MAX_VALUE;
        return (int) (positive % WALK_SAME_TARGET_REROUTE_OFFSETS.length);
    }

    private int seededRerouteOffsetStep(WorldPoint targetWorldPoint, long now) {
        int length = WALK_SAME_TARGET_REROUTE_OFFSETS.length;
        if (targetWorldPoint == null || length <= 1) {
            return 1;
        }
        long seed = now ^ (lastDispatchAtMs * 31L);
        seed ^= ((long) targetWorldPoint.getX() * 83492791L);
        seed ^= ((long) targetWorldPoint.getY() * 19349663L);
        seed ^= ((long) targetWorldPoint.getPlane() * 73856093L);
        if (lastDispatchWorldPoint != null) {
            seed ^= ((long) lastDispatchWorldPoint.getX() * 2971215073L);
            seed ^= ((long) lastDispatchWorldPoint.getY() * 433494437L);
        }
        long positive = seed & Long.MAX_VALUE;
        int step = 1 + (int) (positive % Math.max(1, length - 1));
        while (greatestCommonDivisor(step, length) != 1) {
            step++;
            if (step >= length) {
                step = 1;
            }
        }
        return step;
    }

    private boolean wasRecentlyUsedRerouteTarget(WorldPoint targetWorldPoint) {
        long key = worldPointKey(targetWorldPoint);
        for (long recentKey : recentRerouteTargetKeys) {
            if (recentKey == key) {
                return true;
            }
        }
        return false;
    }

    private void rememberRerouteTarget(WorldPoint targetWorldPoint) {
        if (targetWorldPoint == null || recentRerouteTargetKeys.length <= 0) {
            return;
        }
        recentRerouteTargetKeys[recentRerouteTargetWriteIndex] = worldPointKey(targetWorldPoint);
        recentRerouteTargetWriteIndex = (recentRerouteTargetWriteIndex + 1) % recentRerouteTargetKeys.length;
    }

    private static int greatestCommonDivisor(int a, int b) {
        int x = Math.abs(a);
        int y = Math.abs(b);
        while (y != 0) {
            int tmp = x % y;
            x = y;
            y = tmp;
        }
        return x <= 0 ? 1 : x;
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

    private WalkClickSelection selectWalkClickSelection(
        JsonObject payload,
        WorldPoint targetWorldPoint,
        int localDistance,
        long now
    ) {
        String requestedMode = readString(payload, "walkClickMode", "");
        String normalizedRequestedMode = normalizeWalkClickMode(requestedMode);
        int minimapChancePct = resolveMixedMinimapChancePct(payload, localDistance);

        String selectedMode;
        if (WALK_CLICK_MODE_MINIMAP.equals(normalizedRequestedMode)) {
            selectedMode = WALK_CLICK_MODE_MINIMAP;
        } else if (WALK_CLICK_MODE_SCENE.equals(normalizedRequestedMode)) {
            selectedMode = WALK_CLICK_MODE_SCENE;
        } else {
            selectedMode = shouldUseMinimapForMixed(targetWorldPoint, now, minimapChancePct)
                ? WALK_CLICK_MODE_MINIMAP
                : WALK_CLICK_MODE_SCENE;
        }

        Point canvasPoint = WALK_CLICK_MODE_MINIMAP.equals(selectedMode)
            ? host.resolveWorldTileMinimapClickPoint(targetWorldPoint)
            : host.resolveWorldTileClickPoint(targetWorldPoint);
        return new WalkClickSelection(selectedMode, minimapChancePct, canvasPoint);
    }

    private void noteDispatch(WorldPoint targetWorldPoint, long now, int localDistance, Point canvasPoint) {
        lastAttemptWorldPoint = targetWorldPoint;
        approachWaitUntilMs = now + approachWaitDurationMs(localDistance);
        lastDispatchWorldPoint = targetWorldPoint;
        lastDispatchAtMs = now;
        lastDispatchCanvasPoint = canvasPoint == null ? null : new Point(canvasPoint);
    }

    private void clearTargetState(WorldPoint reachedTargetWorldPoint) {
        if (!worldPointsNear(reachedTargetWorldPoint, lastAttemptWorldPoint, WALK_TARGET_MATCH_RADIUS_TILES)) {
            return;
        }
        lastAttemptWorldPoint = null;
        approachWaitUntilMs = 0L;
        lastDispatchWorldPoint = null;
        lastDispatchAtMs = 0L;
        lastDispatchCanvasPoint = null;
    }

    private Point resolveRetrySafeCanvasPoint(WorldPoint targetWorldPoint, Point baseCanvasPoint) {
        if (baseCanvasPoint == null || !host.isUsableCanvasPoint(baseCanvasPoint)) {
            return baseCanvasPoint;
        }
        if (!worldPointsNear(targetWorldPoint, lastDispatchWorldPoint, WALK_TARGET_MATCH_RADIUS_TILES)) {
            return baseCanvasPoint;
        }
        Rectangle localBounds = new Rectangle(
            baseCanvasPoint.x - WALK_RETRY_POINT_SAMPLE_RADIUS_PX,
            baseCanvasPoint.y - WALK_RETRY_POINT_SAMPLE_RADIUS_PX,
            (WALK_RETRY_POINT_SAMPLE_RADIUS_PX * 2) + 1,
            (WALK_RETRY_POINT_SAMPLE_RADIUS_PX * 2) + 1
        );
        Point sampled = RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            localBounds,
            host::isUsableCanvasPoint,
            lastDispatchCanvasPoint,
            WALK_RETRY_POINT_REPEAT_EXCLUSION_PX,
            0,
            WALK_RETRY_POINT_SAMPLE_ATTEMPTS,
            baseCanvasPoint
        );
        if (sampled == null || !host.isUsableCanvasPoint(sampled)) {
            return baseCanvasPoint;
        }
        return sampled;
    }

    private static long approachWaitDurationMs(int localDistance) {
        int distance = Math.max(0, localDistance);
        long scaled = WALK_APPROACH_BASE_WAIT_MS + ((long) distance * WALK_APPROACH_WAIT_PER_TILE_MS);
        return Math.min(WALK_APPROACH_MAX_WAIT_MS, scaled);
    }

    private static long sameTargetReclickCooldownMs(int localDistance, long dispatchAtMs, WorldPoint targetWorldPoint) {
        long base = WALK_SAME_TARGET_RECLICK_BASE_MS;
        long distanceExtra = (long) Math.max(0, localDistance) * WALK_SAME_TARGET_RECLICK_PER_TILE_MS;
        long seeded = base + distanceExtra;
        seeded = Math.min(WALK_SAME_TARGET_RECLICK_MAX_MS, seeded);
        long seed = dispatchAtMs;
        if (targetWorldPoint != null) {
            seed ^= ((long) targetWorldPoint.getX() * 73856093L);
            seed ^= ((long) targetWorldPoint.getY() * 19349663L);
            seed ^= ((long) targetWorldPoint.getPlane() * 83492791L);
        }
        double unit = normalizedHashUnit(seed);
        double scale = 0.88 + (unit * 0.30);
        return Math.max(260L, Math.round(seeded * scale));
    }

    private static double normalizedHashUnit(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        long positive = z & Long.MAX_VALUE;
        return (double) positive / (double) Long.MAX_VALUE;
    }

    private static boolean worldPointsNear(WorldPoint a, WorldPoint b, int radiusTiles) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getPlane() != b.getPlane()) {
            return false;
        }
        int distance = a.distanceTo(b);
        int radius = Math.max(0, radiusTiles);
        return distance >= 0 && distance <= radius;
    }

    private static int readTargetAxis(JsonObject payload, String primaryKey, String secondaryKey) {
        if (payload == null) {
            return -1;
        }
        int primary = asInt(payload.get(primaryKey), -1);
        if (primary > 0) {
            return primary;
        }
        return asInt(payload.get(secondaryKey), -1);
    }

    private static String readString(JsonObject payload, String key, String fallback) {
        if (payload == null || key == null || key.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        JsonElement value = payload.get(key);
        if (value == null || value.isJsonNull()) {
            return fallback == null ? "" : fallback;
        }
        try {
            String raw = value.getAsString();
            return raw == null ? (fallback == null ? "" : fallback) : raw;
        } catch (Exception ignored) {
            return fallback == null ? "" : fallback;
        }
    }

    private static String normalizeWalkClickMode(String rawMode) {
        String normalized = rawMode == null
            ? ""
            : rawMode.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case WALK_CLICK_MODE_SCENE:
            case WALK_CLICK_MODE_MINIMAP:
            case WALK_CLICK_MODE_MIXED:
                return normalized;
            default:
                return WALK_CLICK_MODE_DEFAULT;
        }
    }

    private static int resolveMixedMinimapChancePct(JsonObject payload, int localDistance) {
        int configured = clamp(asInt(payload == null ? null : payload.get("minimapClickChancePct"), -1), -1, 100);
        if (configured >= 0) {
            return configured;
        }
        int distanceExtra = Math.max(0, localDistance - WALK_MINIMAP_MIXED_DISTANCE_PIVOT_TILES);
        int dynamicChance = WALK_MINIMAP_MIXED_BASE_CHANCE_PCT + (distanceExtra * WALK_MINIMAP_MIXED_DISTANCE_STEP_PCT);
        return clamp(dynamicChance, WALK_MINIMAP_MIXED_MIN_CHANCE_PCT, WALK_MINIMAP_MIXED_MAX_CHANCE_PCT);
    }

    private static boolean shouldUseMinimapForMixed(WorldPoint targetWorldPoint, long now, int minimapChancePct) {
        long seed = now;
        if (targetWorldPoint != null) {
            seed ^= ((long) targetWorldPoint.getX() * 73856093L);
            seed ^= ((long) targetWorldPoint.getY() * 19349663L);
            seed ^= ((long) targetWorldPoint.getPlane() * 83492791L);
        }
        double rollPct = normalizedHashUnit(seed) * 100.0;
        return rollPct < clamp(minimapChancePct, 0, 100);
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class WalkClickSelection {
        private final String mode;
        private final int minimapChancePct;
        private final Point canvasPoint;

        private WalkClickSelection(String mode, int minimapChancePct, Point canvasPoint) {
            this.mode = mode;
            this.minimapChancePct = minimapChancePct;
            this.canvasPoint = canvasPoint;
        }
    }
}
