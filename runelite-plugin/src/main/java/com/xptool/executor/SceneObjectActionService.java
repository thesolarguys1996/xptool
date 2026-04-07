package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

final class SceneObjectActionService {
    private static final int DEFAULT_MAX_DISTANCE_TILES = 8;
    private static final int MAX_DISTANCE_TILES_CAP = 20;
    private static final String DEFAULT_OPTION_KEYWORD = "open";

    interface Host {
        Player localPlayer();

        Iterable<TileObject> nearbySceneObjects();

        String resolveSceneObjectName(TileObject targetObject);

        Point resolveSceneObjectClickPoint(TileObject targetObject);

        boolean isUsableCanvasPoint(Point point);

        boolean moveInteractionCursorToCanvasPoint(Point canvasPoint);

        boolean isCursorNearTarget(Point canvasPoint);

        boolean isTopMenuOptionOnObject(TileObject targetObject, String... optionKeywords);

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        boolean waitForMotorActionReady(long maxWaitMs);

        long interactionMotorReadyWaitMaxMs();

        boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion);

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        CommandExecutor.CommandDecision accept(String reason, JsonObject detailsJson);

        CommandExecutor.CommandDecision reject(String reason);
    }

    private final Host host;

    SceneObjectActionService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision execute(JsonObject payload, MotionProfile motionProfile) {
        Player local = host.localPlayer();
        if (local == null || local.getWorldLocation() == null) {
            return host.reject("scene_object_action_player_unavailable");
        }

        Criteria criteria = parseCriteria(payload);
        Optional<TargetCandidate> candidateOpt = resolveNearestCandidate(local, criteria);
        if (candidateOpt.isEmpty()) {
            return host.accept(
                "scene_object_action_target_unavailable",
                host.details(
                    "targetObjectId", criteria.targetObjectId,
                    "targetObjectNameContains", criteria.targetObjectNameContains,
                    "targetWorldX", criteria.targetWorldX,
                    "targetWorldY", criteria.targetWorldY,
                    "targetPlane", criteria.targetPlane,
                    "minWorldX", criteria.minWorldX,
                    "maxWorldX", criteria.maxWorldX,
                    "minWorldY", criteria.minWorldY,
                    "maxWorldY", criteria.maxWorldY,
                    "maxDistanceTiles", criteria.maxDistanceTiles,
                    "optionKeywordPrimary", criteria.optionKeywords[0]
                )
            );
        }

        TargetCandidate candidate = candidateOpt.get();
        Point targetPoint = host.resolveSceneObjectClickPoint(candidate.targetObject);
        if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
            return host.reject("scene_object_action_click_point_unavailable");
        }

        boolean cursorNearTarget = host.isCursorNearTarget(targetPoint);
        if (!cursorNearTarget) {
            if (!host.moveInteractionCursorToCanvasPoint(targetPoint)) {
                return host.accept(
                    "scene_object_action_cursor_move_pending",
                    candidateDetails(candidate, criteria, "cursorMovePending", true)
                );
            }
            cursorNearTarget = host.isCursorNearTarget(targetPoint);
        }
        if (!cursorNearTarget) {
            return host.accept(
                "scene_object_action_cursor_settling",
                candidateDetails(candidate, criteria, "cursorSettling", true)
            );
        }

        if (!host.isTopMenuOptionOnObject(candidate.targetObject, criteria.optionKeywords)) {
            return host.accept(
                "scene_object_action_top_menu_not_expected_option",
                candidateDetails(candidate, criteria)
            );
        }

        if (!host.waitForMotorActionReady(host.interactionMotorReadyWaitMaxMs())) {
            return host.accept(
                "scene_object_action_click_cooldown_deferred",
                candidateDetails(candidate, criteria)
            );
        }

        ClickMotionSettings motion = host.resolveClickMotion(payload, motionProfile);
        if (host.clickCanvasPoint(targetPoint, motion)) {
            return host.accept(
                "scene_object_action_dispatched",
                candidateDetails(candidate, criteria, "dispatchMode", "left_click_top_option")
            );
        }

        return host.accept(
            "scene_object_action_click_deferred",
            candidateDetails(candidate, criteria)
        );
    }

    private JsonObject candidateDetails(TargetCandidate candidate, Criteria criteria, Object... extraKvPairs) {
        Object[] kvPairs = new Object[] {
            "objectId", candidate.objectId,
            "objectName", candidate.objectName,
            "distanceTiles", candidate.distanceTiles,
            "worldX", candidate.worldX,
            "worldY", candidate.worldY,
            "plane", candidate.plane,
            "optionKeywordPrimary", criteria.optionKeywords[0],
            "targetObjectNameContains", criteria.targetObjectNameContains,
            "targetObjectId", criteria.targetObjectId,
            "minWorldX", criteria.minWorldX,
            "maxWorldX", criteria.maxWorldX,
            "minWorldY", criteria.minWorldY,
            "maxWorldY", criteria.maxWorldY,
            "maxDistanceTiles", criteria.maxDistanceTiles
        };
        if (extraKvPairs == null || extraKvPairs.length == 0) {
            return host.details(kvPairs);
        }
        Object[] merged = new Object[kvPairs.length + extraKvPairs.length];
        System.arraycopy(kvPairs, 0, merged, 0, kvPairs.length);
        System.arraycopy(extraKvPairs, 0, merged, kvPairs.length, extraKvPairs.length);
        return host.details(merged);
    }

    private Optional<TargetCandidate> resolveNearestCandidate(Player local, Criteria criteria) {
        WorldPoint localPoint = local.getWorldLocation();
        if (localPoint == null) {
            return Optional.empty();
        }
        TargetCandidate best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (TileObject candidate : host.nearbySceneObjects()) {
            if (candidate == null || candidate.getWorldLocation() == null) {
                continue;
            }
            int objectId = candidate.getId();
            if (criteria.targetObjectId > 0 && objectId != criteria.targetObjectId) {
                continue;
            }
            String objectName = normalizeToken(host.resolveSceneObjectName(candidate));
            if (!criteria.targetObjectNameContains.isBlank()
                && !objectName.contains(criteria.targetObjectNameContains)) {
                continue;
            }
            WorldPoint worldPoint = candidate.getWorldLocation();
            if (criteria.targetPlane >= 0 && worldPoint.getPlane() != criteria.targetPlane) {
                continue;
            }
            if (criteria.targetWorldX > 0 && worldPoint.getX() != criteria.targetWorldX) {
                continue;
            }
            if (criteria.targetWorldY > 0 && worldPoint.getY() != criteria.targetWorldY) {
                continue;
            }
            if (criteria.minWorldX > 0 && worldPoint.getX() < criteria.minWorldX) {
                continue;
            }
            if (criteria.maxWorldX > 0 && worldPoint.getX() > criteria.maxWorldX) {
                continue;
            }
            if (criteria.minWorldY > 0 && worldPoint.getY() < criteria.minWorldY) {
                continue;
            }
            if (criteria.maxWorldY > 0 && worldPoint.getY() > criteria.maxWorldY) {
                continue;
            }
            int distanceTiles = localPoint.distanceTo(worldPoint);
            if (distanceTiles < 0 || distanceTiles > criteria.maxDistanceTiles) {
                continue;
            }
            if (best == null || distanceTiles < bestDistance) {
                bestDistance = distanceTiles;
                best = new TargetCandidate(
                    candidate,
                    objectId,
                    host.safeString(objectName),
                    worldPoint.getX(),
                    worldPoint.getY(),
                    worldPoint.getPlane(),
                    distanceTiles
                );
            }
        }
        return Optional.ofNullable(best);
    }

    private Criteria parseCriteria(JsonObject payload) {
        String[] optionKeywords = parseOptionKeywords(payload == null ? null : payload.get("optionKeywords"));
        if (optionKeywords.length == 0) {
            String single = asString(payload == null ? null : payload.get("optionKeyword"));
            optionKeywords = new String[] {single.isBlank() ? DEFAULT_OPTION_KEYWORD : single};
        }
        return new Criteria(
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
        List<String> keywords = new ArrayList<>();
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

    private static final class Criteria {
        private final int targetObjectId;
        private final String targetObjectNameContains;
        private final int targetWorldX;
        private final int targetWorldY;
        private final int targetPlane;
        private final int minWorldX;
        private final int maxWorldX;
        private final int minWorldY;
        private final int maxWorldY;
        private final int maxDistanceTiles;
        private final String[] optionKeywords;

        private Criteria(
            int targetObjectId,
            String targetObjectNameContains,
            int targetWorldX,
            int targetWorldY,
            int targetPlane,
            int minWorldX,
            int maxWorldX,
            int minWorldY,
            int maxWorldY,
            int maxDistanceTiles,
            String[] optionKeywords
        ) {
            this.targetObjectId = targetObjectId;
            this.targetObjectNameContains = targetObjectNameContains == null ? "" : targetObjectNameContains;
            this.targetWorldX = targetWorldX;
            this.targetWorldY = targetWorldY;
            this.targetPlane = targetPlane;
            this.minWorldX = minWorldX;
            this.maxWorldX = maxWorldX;
            this.minWorldY = minWorldY;
            this.maxWorldY = maxWorldY;
            this.maxDistanceTiles = maxDistanceTiles;
            this.optionKeywords = optionKeywords == null || optionKeywords.length == 0
                ? new String[] {DEFAULT_OPTION_KEYWORD}
                : optionKeywords;
        }
    }

    private static final class TargetCandidate {
        private final TileObject targetObject;
        private final int objectId;
        private final String objectName;
        private final int worldX;
        private final int worldY;
        private final int plane;
        private final int distanceTiles;

        private TargetCandidate(
            TileObject targetObject,
            int objectId,
            String objectName,
            int worldX,
            int worldY,
            int plane,
            int distanceTiles
        ) {
            this.targetObject = targetObject;
            this.objectId = objectId;
            this.objectName = objectName;
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.distanceTiles = distanceTiles;
        }
    }
}
