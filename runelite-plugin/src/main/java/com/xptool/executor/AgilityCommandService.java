package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.AgilityTargetResolver;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

final class AgilityCommandService {
    private static final String AGILITY_TAG = "agility";
    private static final long TICK_MS = 600L;
    private static final long STEP_RETRY_BLOCK_MIN_MS = 8L * TICK_MS;
    private static final long STEP_RETRY_BLOCK_MAX_MS = 10L * TICK_MS;
    private static final long STEP_NO_PROGRESS_RETRY_MS = 3L * TICK_MS;
    private static final int STEP_COMPLETION_EXIT_DISTANCE_TILES = 3;
    private static final long TIGHTROPE_HOLD_MIN_MS = 14L * TICK_MS;
    private static final long TIGHTROPE_HOLD_MAX_MS = 18L * TICK_MS;
    private static final long CLIMB_DOWN_CRATE_HOLD_MIN_MS = 14L * TICK_MS;
    private static final long CLIMB_DOWN_CRATE_HOLD_MAX_MS = 20L * TICK_MS;
    private static final int DEFAULT_MAX_DISTANCE_TILES = 8;
    private static final int MAX_DISTANCE_TILES_CAP = 20;
    private static final String DEFAULT_OPTION_KEYWORD = "climb";

    interface Host {
        Player localPlayer();

        Point resolveSceneObjectClickPoint(TileObject targetObject);

        boolean isUsableCanvasPoint(Point point);

        boolean moveInteractionCursorToCanvasPoint(Point canvasPoint);

        boolean isCursorNearTarget(Point canvasPoint);

        boolean isTopMenuOptionOnObject(TileObject targetObject, String... optionKeywords);

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        boolean waitForMotorActionReady(long maxWaitMs);

        long interactionMotorReadyWaitMaxMs();

        MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile);

        MotorProfile buildAgilityMoveAndClickProfile(ClickMotionSettings motion, TileObject targetObject);

        void noteInteractionActivityNow();

        void incrementClicksDispatched();

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        CommandExecutor.CommandDecision accept(String reason, JsonObject details);

        CommandExecutor.CommandDecision reject(String reason);
    }

    private final Host host;
    private final AgilityTargetResolver agilityTargetResolver;
    private final Set<Integer> completedStepIndexes = new HashSet<>();

    private int activeRouteStepCount = -1;
    private int expectedStepIndex = -1;
    private int lastDispatchedStepIndex = -1;
    private WorldPoint lastDispatchOrigin = null;
    private WorldPoint lastDispatchStepPoint = null;
    private long lastDispatchAtMs = Long.MIN_VALUE;
    private long retryBlockUntilMs = Long.MIN_VALUE;
    private long stepActionHoldUntilMs = Long.MIN_VALUE;
    private String lastDispatchActionKey = "";

    AgilityCommandService(Host host, AgilityTargetResolver agilityTargetResolver) {
        this.host = host;
        this.agilityTargetResolver = agilityTargetResolver;
    }

    CommandExecutor.CommandDecision executeAgilityObstacleAction(JsonObject payload, MotionProfile motionProfile) {
        JsonObject safePayload = payload == null ? new JsonObject() : payload;
        long now = System.currentTimeMillis();
        Player player = host.localPlayer();
        WorldPoint localWorld = player == null ? null : player.getWorldLocation();
        boolean animationActive = player != null && isAnimationActive(player.getAnimation());
        String plannerTag = normalizeTag(asString(safePayload.get("plannerTag")));
        if (!AGILITY_TAG.equals(plannerTag)) {
            return host.reject("agility_obstacle_invalid_planner_tag");
        }

        int stepIndex = asInt(safePayload.get("routeStepIndex"), -1);
        int routeStepCount = asInt(safePayload.get("routeStepCount"), -1);
        if (routeStepCount <= 0 || stepIndex < 0 || stepIndex >= routeStepCount) {
            return host.reject("agility_obstacle_missing_route_metadata");
        }

        syncRoute(stepIndex, routeStepCount);
        if (completedStepIndexes.contains(stepIndex)) {
            return host.accept(
                "agility_obstacle_completed_step_blocked",
                decisionDetails(
                    stepIndex,
                    routeStepCount,
                    localWorld,
                    animationActive,
                    now,
                    "completedStepCount", completedStepIndexes.size()
                )
            );
        }
        if (stepIndex != expectedStepIndex) {
            return host.accept(
                "agility_obstacle_out_of_order_deferred",
                decisionDetails(
                    stepIndex,
                    routeStepCount,
                    localWorld,
                    animationActive,
                    now,
                    "completedStepCount", completedStepIndexes.size(),
                    "lastDispatchedStepIndex", lastDispatchedStepIndex
                )
            );
        }

        if (stepIndex == lastDispatchedStepIndex) {
            if (isDispatchedStepCompleted(localWorld)) {
                completeStep(stepIndex);
                return host.accept(
                    "agility_obstacle_step_completed",
                    decisionDetails(
                        stepIndex,
                        routeStepCount,
                        localWorld,
                        animationActive,
                        now,
                        "nextExpectedStepIndex", expectedStepIndex
                    )
                );
            }
            if (animationActive || hasProgressSinceDispatch(localWorld)) {
                return host.accept(
                    "agility_obstacle_step_in_progress",
                    decisionDetails(
                        stepIndex,
                        routeStepCount,
                        localWorld,
                        animationActive,
                        now,
                        "hasProgressSinceDispatch", hasProgressSinceDispatch(localWorld)
                    )
                );
            }
            if (now < stepActionHoldUntilMs) {
                return host.accept(
                    "agility_obstacle_action_hold_window",
                    decisionDetails(
                        stepIndex,
                        routeStepCount,
                        localWorld,
                        animationActive,
                        now,
                        "holdWindowRemainingMs", Math.max(0L, stepActionHoldUntilMs - now),
                        "dispatchActionKey", lastDispatchActionKey
                    )
                );
            }
            if (now < retryBlockUntilMs) {
                return host.accept(
                    "agility_obstacle_retry_blocked",
                    decisionDetails(
                        stepIndex,
                        routeStepCount,
                        localWorld,
                        animationActive,
                        now,
                        "waitMsRemaining", Math.max(0L, retryBlockUntilMs - now)
                    )
                );
            }
            if ((now - lastDispatchAtMs) < STEP_NO_PROGRESS_RETRY_MS) {
                return host.accept(
                    "agility_obstacle_waiting_progress",
                    decisionDetails(
                        stepIndex,
                        routeStepCount,
                        localWorld,
                        animationActive,
                        now,
                        "waitMsRemaining", Math.max(0L, STEP_NO_PROGRESS_RETRY_MS - (now - lastDispatchAtMs))
                    )
                );
            }
        }

        AgilityTargetResolver.Criteria criteria = parseCriteria(safePayload);
        if (criteria.targetPlane < 0) {
            return host.reject("agility_obstacle_missing_target_plane");
        }
        AgilityTargetResolver.SearchResult searchResult = agilityTargetResolver.resolveNearestCandidate(player, criteria);
        if (searchResult.candidate.isEmpty()) {
            return host.accept(
                "agility_obstacle_target_unavailable",
                decisionDetails(
                    stepIndex,
                    routeStepCount,
                    localWorld,
                    animationActive,
                    now,
                    "targetObjectId", criteria.targetObjectId,
                    "targetObjectNameContains", criteria.targetObjectNameContains,
                    "targetPlane", criteria.targetPlane,
                    "minWorldX", criteria.minWorldX,
                    "maxWorldX", criteria.maxWorldX,
                    "minWorldY", criteria.minWorldY,
                    "maxWorldY", criteria.maxWorldY,
                    "maxDistanceTiles", criteria.maxDistanceTiles,
                    "searchLocalPlayerPlane", searchResult.debug.localPlayerPlane,
                    "searchScannedObjects", searchResult.debug.scannedObjects,
                    "searchMissingWorldLocation", searchResult.debug.missingWorldLocation,
                    "searchRejectedObjectId", searchResult.debug.rejectedObjectId,
                    "searchRejectedObjectName", searchResult.debug.rejectedObjectName,
                    "searchRejectedPlane", searchResult.debug.rejectedPlane,
                    "searchRejectedTargetWorldX", searchResult.debug.rejectedTargetWorldX,
                    "searchRejectedTargetWorldY", searchResult.debug.rejectedTargetWorldY,
                    "searchRejectedBounds", searchResult.debug.rejectedBounds,
                    "searchRejectedDistance", searchResult.debug.rejectedDistance,
                    "searchWithinDistanceCandidates", searchResult.debug.withinDistanceCandidates,
                    "searchClosestDistanceSeen", searchResult.debug.closestDistanceSeen
                )
            );
        }

        AgilityTargetResolver.TargetCandidate candidate = searchResult.candidate.get();
        Point targetPoint = host.resolveSceneObjectClickPoint(candidate.targetObject);
        if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
            return host.reject("agility_obstacle_click_point_unavailable");
        }
        boolean cursorNearTarget = host.isCursorNearTarget(targetPoint);
        if (!cursorNearTarget) {
            if (!host.moveInteractionCursorToCanvasPoint(targetPoint)) {
                return host.accept(
                    "agility_obstacle_cursor_move_pending",
                    decisionDetails(
                        stepIndex,
                        routeStepCount,
                        localWorld,
                        animationActive,
                        now,
                        "objectId", candidate.objectId,
                        "distanceTiles", candidate.distanceTiles,
                        "worldX", candidate.worldX,
                        "worldY", candidate.worldY,
                        "plane", candidate.plane,
                        "targetCanvasX", targetPoint.x,
                        "targetCanvasY", targetPoint.y,
                        "cursorMovePending", true
                    )
                );
            }
            cursorNearTarget = host.isCursorNearTarget(targetPoint);
        }
        if (!cursorNearTarget) {
            return host.accept(
                "agility_obstacle_cursor_settling",
                decisionDetails(
                    stepIndex,
                    routeStepCount,
                    localWorld,
                    animationActive,
                    now,
                    "objectId", candidate.objectId,
                    "distanceTiles", candidate.distanceTiles,
                    "worldX", candidate.worldX,
                    "worldY", candidate.worldY,
                    "plane", candidate.plane,
                    "targetCanvasX", targetPoint.x,
                    "targetCanvasY", targetPoint.y,
                    "cursorSettling", true
                )
            );
        }
        if (!host.isTopMenuOptionOnObject(candidate.targetObject, criteria.optionKeywords)) {
            return host.accept(
                "agility_obstacle_top_menu_not_expected_option",
                decisionDetails(
                    stepIndex,
                    routeStepCount,
                    localWorld,
                    animationActive,
                    now,
                    "objectId", candidate.objectId,
                    "objectName", candidate.objectName,
                    "distanceTiles", candidate.distanceTiles,
                    "worldX", candidate.worldX,
                    "worldY", candidate.worldY,
                    "plane", candidate.plane,
                    "optionKeywordPrimary", criteria.optionKeywords[0],
                    "targetCanvasX", targetPoint.x,
                    "targetCanvasY", targetPoint.y,
                    "searchScannedObjects", searchResult.debug.scannedObjects
                )
            );
        }
        if (!host.waitForMotorActionReady(host.interactionMotorReadyWaitMaxMs())) {
            return host.accept(
                "agility_obstacle_click_cooldown_deferred",
                decisionDetails(
                    stepIndex,
                    routeStepCount,
                    localWorld,
                    animationActive,
                    now,
                    "objectId", candidate.objectId,
                    "distanceTiles", candidate.distanceTiles,
                    "worldX", candidate.worldX,
                    "worldY", candidate.worldY,
                    "plane", candidate.plane,
                    "targetCanvasX", targetPoint.x,
                    "targetCanvasY", targetPoint.y
                )
            );
        }

        ClickMotionSettings motion = host.resolveClickMotion(safePayload, motionProfile);
        MotorHandle handle = host.scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(targetPoint),
            MotorGestureType.MOVE_AND_CLICK,
            host.buildAgilityMoveAndClickProfile(motion, candidate.targetObject)
        );
        if (handle.status == MotorGestureStatus.FAILED || handle.status == MotorGestureStatus.CANCELLED) {
            return host.reject("agility_obstacle_motor_gesture_" + safeString(handle.reason));
        }

        if (handle.status == MotorGestureStatus.COMPLETE || handle.status == MotorGestureStatus.SCHEDULED) {
            boolean countedDispatch = handle.status == MotorGestureStatus.COMPLETE;
            if (countedDispatch) {
                host.noteInteractionActivityNow();
                host.incrementClicksDispatched();
            }
            lastDispatchedStepIndex = stepIndex;
            lastDispatchAtMs = now;
            lastDispatchOrigin = localWorld;
            lastDispatchStepPoint = resolveStepWorldPoint(safePayload, localWorld);
            lastDispatchActionKey = resolveDispatchActionKey(safePayload, criteria);
            stepActionHoldUntilMs = now + resolveActionHoldWindowMs(lastDispatchActionKey);
            retryBlockUntilMs = now + randomLongInclusive(STEP_RETRY_BLOCK_MIN_MS, STEP_RETRY_BLOCK_MAX_MS);
            String dispatchReason = handle.status == MotorGestureStatus.COMPLETE
                ? "agility_obstacle_left_click_dispatched"
                : "agility_obstacle_motor_gesture_dispatched";
            return host.accept(
                dispatchReason,
                decisionDetails(
                    stepIndex,
                    routeStepCount,
                    localWorld,
                    animationActive,
                    now,
                    "objectId", candidate.objectId,
                    "distanceTiles", candidate.distanceTiles,
                    "worldX", candidate.worldX,
                    "worldY", candidate.worldY,
                    "plane", candidate.plane,
                    "targetCanvasX", targetPoint.x,
                    "targetCanvasY", targetPoint.y,
                    "motorGestureId", handle.id,
                    "motorStatus", handle.status.name(),
                    "motorReason", handle.reason,
                    "dispatchActionKey", lastDispatchActionKey,
                    "actionHoldWindowMs", Math.max(0L, stepActionHoldUntilMs - now)
                )
            );
        }

        return host.accept(
            "agility_obstacle_motor_gesture_in_flight",
            decisionDetails(
                stepIndex,
                routeStepCount,
                localWorld,
                animationActive,
                now,
                "objectId", candidate.objectId,
                "distanceTiles", candidate.distanceTiles,
                "worldX", candidate.worldX,
                "worldY", candidate.worldY,
                "plane", candidate.plane,
                "targetCanvasX", targetPoint.x,
                "targetCanvasY", targetPoint.y,
                "motorGestureId", handle.id,
                "motorStatus", handle.status.name(),
                "motorReason", handle.reason,
                "dispatchActionKey", lastDispatchActionKey
            )
        );
    }

    private JsonObject decisionDetails(
        int stepIndex,
        int routeStepCount,
        WorldPoint localWorld,
        boolean animationActive,
        long now,
        Object... extraKvPairs
    ) {
        List<Object> kvPairs = new ArrayList<>();
        kvPairs.add("routeStepIndex");
        kvPairs.add(stepIndex);
        kvPairs.add("expectedStepIndex");
        kvPairs.add(expectedStepIndex);
        kvPairs.add("routeStepCount");
        kvPairs.add(routeStepCount > 0 ? routeStepCount : activeRouteStepCount);
        kvPairs.add("activeRouteStepCount");
        kvPairs.add(activeRouteStepCount);
        kvPairs.add("lastDispatchedStepIndex");
        kvPairs.add(lastDispatchedStepIndex);
        kvPairs.add("retryBlockUntilMs");
        kvPairs.add(retryBlockUntilMs);
        kvPairs.add("retryBlockRemainingMs");
        kvPairs.add(retryBlockUntilMs == Long.MIN_VALUE ? 0L : Math.max(0L, retryBlockUntilMs - now));
        kvPairs.add("lastDispatchAtMs");
        kvPairs.add(lastDispatchAtMs);
        kvPairs.add("msSinceLastDispatch");
        kvPairs.add(lastDispatchAtMs == Long.MIN_VALUE ? -1L : Math.max(0L, now - lastDispatchAtMs));
        kvPairs.add("animationActive");
        kvPairs.add(animationActive);
        kvPairs.add("completedStepCount");
        kvPairs.add(completedStepIndexes.size());
        appendWorldPointDetails(kvPairs, "player", localWorld);
        appendWorldPointDetails(kvPairs, "lastDispatchOrigin", lastDispatchOrigin);
        appendWorldPointDetails(kvPairs, "lastDispatchStep", lastDispatchStepPoint);
        if (extraKvPairs != null && extraKvPairs.length > 0) {
            for (Object value : extraKvPairs) {
                kvPairs.add(value);
            }
        }
        return host.details(kvPairs.toArray());
    }

    private static void appendWorldPointDetails(List<Object> kvPairs, String prefix, WorldPoint point) {
        String safePrefix = prefix == null ? "point" : prefix;
        kvPairs.add(safePrefix + "WorldX");
        kvPairs.add(point == null ? -1 : point.getX());
        kvPairs.add(safePrefix + "WorldY");
        kvPairs.add(point == null ? -1 : point.getY());
        kvPairs.add(safePrefix + "Plane");
        kvPairs.add(point == null ? -1 : point.getPlane());
    }

    private AgilityTargetResolver.Criteria parseCriteria(JsonObject payload) {
        String[] optionKeywords = parseOptionKeywords(payload == null ? null : payload.get("optionKeywords"));
        if (optionKeywords.length == 0) {
            String single = asString(payload == null ? null : payload.get("optionKeyword"));
            optionKeywords = new String[] {single.isBlank() ? DEFAULT_OPTION_KEYWORD : single};
        }
        return new AgilityTargetResolver.Criteria(
            asInt(payload == null ? null : payload.get("targetObjectId"), -1),
            normalizeToken(asString(payload == null ? null : payload.get("targetObjectNameContains"))),
            asInt(payload == null ? null : payload.get("targetWorldX"), -1),
            asInt(payload == null ? null : payload.get("targetWorldY"), -1),
            asInt(payload == null ? null : payload.get("targetPlane"), -1),
            asInt(payload == null ? null : payload.get("minWorldX"), -1),
            asInt(payload == null ? null : payload.get("maxWorldX"), -1),
            asInt(payload == null ? null : payload.get("minWorldY"), -1),
            asInt(payload == null ? null : payload.get("maxWorldY"), -1),
            clamp(
                asInt(payload == null ? null : payload.get("maxDistanceTiles"), DEFAULT_MAX_DISTANCE_TILES),
                0,
                MAX_DISTANCE_TILES_CAP
            ),
            optionKeywords
        );
    }

    private static String[] parseOptionKeywords(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return new String[0];
        }
        java.util.List<String> keywords = new java.util.ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            String token = asString(item).trim();
            if (!token.isEmpty()) {
                keywords.add(token);
            }
        }
        return keywords.toArray(new String[0]);
    }

    private static String normalizeToken(String raw) {
        String safe = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return safe.replaceAll("<[^>]*>", "").trim();
    }

    private void syncRoute(int stepIndex, int routeStepCount) {
        if (activeRouteStepCount != routeStepCount || routeStepCount <= 0) {
            resetRouteState(routeStepCount);
        }
        if (expectedStepIndex < 0) {
            expectedStepIndex = stepIndex;
            return;
        }
        if (stepIndex == 0 && expectedStepIndex != 0) {
            // Planner may hard-reset to step 0 after a fall/off-route recovery.
            completedStepIndexes.clear();
            expectedStepIndex = 0;
            clearDispatchState();
            return;
        }
        if (stepIndex == expectedStepIndex) {
            return;
        }
        if (stepIndex == 0 && expectedStepIndex == activeRouteStepCount - 1) {
            completedStepIndexes.clear();
            expectedStepIndex = 0;
            return;
        }
        if (stepIndex > expectedStepIndex) {
            for (int idx = expectedStepIndex; idx < stepIndex; idx++) {
                completedStepIndexes.add(idx);
            }
            expectedStepIndex = stepIndex;
        }
    }

    private void resetRouteState(int routeStepCount) {
        activeRouteStepCount = routeStepCount;
        expectedStepIndex = -1;
        completedStepIndexes.clear();
        clearDispatchState();
    }

    private void clearDispatchState() {
        lastDispatchedStepIndex = -1;
        lastDispatchOrigin = null;
        lastDispatchStepPoint = null;
        lastDispatchAtMs = Long.MIN_VALUE;
        retryBlockUntilMs = Long.MIN_VALUE;
        stepActionHoldUntilMs = Long.MIN_VALUE;
        lastDispatchActionKey = "";
    }

    private void completeStep(int completedStepIndex) {
        if (activeRouteStepCount <= 0) {
            clearDispatchState();
            return;
        }
        completedStepIndexes.add(Math.max(0, completedStepIndex));
        expectedStepIndex = (completedStepIndex + 1) % activeRouteStepCount;
        if (expectedStepIndex == 0) {
            completedStepIndexes.clear();
        }
        clearDispatchState();
    }

    private boolean hasProgressSinceDispatch(WorldPoint localWorld) {
        if (localWorld == null || lastDispatchOrigin == null) {
            return false;
        }
        if (localWorld.getPlane() != lastDispatchOrigin.getPlane()) {
            return true;
        }
        return chebyshevDistance(localWorld, lastDispatchOrigin) >= 1;
    }

    private boolean isDispatchedStepCompleted(WorldPoint localWorld) {
        if (localWorld == null || lastDispatchStepPoint == null) {
            return false;
        }
        if (localWorld.getPlane() != lastDispatchStepPoint.getPlane()) {
            return true;
        }
        return chebyshevDistance(localWorld, lastDispatchStepPoint) >= STEP_COMPLETION_EXIT_DISTANCE_TILES;
    }

    private static int chebyshevDistance(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private static WorldPoint resolveStepWorldPoint(JsonObject payload, WorldPoint fallbackPlanePoint) {
        if (payload == null) {
            return null;
        }
        int plane = asInt(payload.get("targetPlane"), fallbackPlanePoint == null ? -1 : fallbackPlanePoint.getPlane());
        int worldX = asInt(payload.get("targetWorldX"), -1);
        int worldY = asInt(payload.get("targetWorldY"), -1);
        if (worldX > 0 && worldY > 0 && plane >= 0) {
            return new WorldPoint(worldX, worldY, plane);
        }
        int minWorldX = asInt(payload.get("minWorldX"), -1);
        int maxWorldX = asInt(payload.get("maxWorldX"), -1);
        int minWorldY = asInt(payload.get("minWorldY"), -1);
        int maxWorldY = asInt(payload.get("maxWorldY"), -1);
        if (minWorldX > 0 && maxWorldX > 0 && minWorldY > 0 && maxWorldY > 0 && plane >= 0) {
            int centerX = minWorldX + ((maxWorldX - minWorldX) / 2);
            int centerY = minWorldY + ((maxWorldY - minWorldY) / 2);
            return new WorldPoint(centerX, centerY, plane);
        }
        return null;
    }

    private static long randomLongInclusive(long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static boolean isAnimationActive(int animationId) {
        return animationId != -1 && animationId != 0;
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

    private static String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String safeString(String value) {
        return host == null ? (value == null ? "" : value) : host.safeString(value);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeTag(String raw) {
        return (raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
    }

    private static String resolveDispatchActionKey(JsonObject payload, AgilityTargetResolver.Criteria criteria) {
        String action = normalizeToken(asString(payload == null ? null : payload.get("action")));
        if (!action.isBlank()) {
            return action;
        }
        String objectNameContains = criteria == null ? "" : normalizeToken(criteria.targetObjectNameContains);
        if (!objectNameContains.isBlank()) {
            return objectNameContains;
        }
        if (criteria != null && criteria.optionKeywords != null) {
            for (String optionKeyword : criteria.optionKeywords) {
                String normalized = normalizeToken(optionKeyword);
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
        }
        return "";
    }

    private static long resolveActionHoldWindowMs(String actionKey) {
        String normalized = normalizeToken(actionKey);
        if (normalized.contains("tightrope")) {
            return randomLongInclusive(TIGHTROPE_HOLD_MIN_MS, TIGHTROPE_HOLD_MAX_MS);
        }
        if (normalized.contains("climb-down crate") || normalized.contains("crate")) {
            return randomLongInclusive(CLIMB_DOWN_CRATE_HOLD_MIN_MS, CLIMB_DOWN_CRATE_HOLD_MAX_MS);
        }
        return 0L;
    }

}
