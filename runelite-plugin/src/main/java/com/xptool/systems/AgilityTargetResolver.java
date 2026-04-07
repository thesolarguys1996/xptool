package com.xptool.systems;

import java.util.Locale;
import java.util.Optional;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class AgilityTargetResolver {
    public interface Host {
        Iterable<TileObject> nearbySceneObjects();

        String resolveSceneObjectName(TileObject targetObject);

        String safeString(String value);
    }

    public static final class Criteria {
        public final int targetObjectId;
        public final String targetObjectNameContains;
        public final int targetWorldX;
        public final int targetWorldY;
        public final int targetPlane;
        public final int minWorldX;
        public final int maxWorldX;
        public final int minWorldY;
        public final int maxWorldY;
        public final int maxDistanceTiles;
        public final String[] optionKeywords;

        public Criteria(
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
            this.optionKeywords = optionKeywords == null ? new String[0] : optionKeywords;
        }
    }

    public static final class TargetCandidate {
        public final TileObject targetObject;
        public final int objectId;
        public final String objectName;
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int distanceTiles;

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
            this.objectName = objectName == null ? "" : objectName;
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.distanceTiles = distanceTiles;
        }
    }

    public static final class SearchDebug {
        public int localPlayerPlane = -1;
        public int scannedObjects = 0;
        public int missingWorldLocation = 0;
        public int rejectedObjectId = 0;
        public int rejectedObjectName = 0;
        public int rejectedPlane = 0;
        public int rejectedTargetWorldX = 0;
        public int rejectedTargetWorldY = 0;
        public int rejectedBounds = 0;
        public int rejectedDistance = 0;
        public int withinDistanceCandidates = 0;
        public int closestDistanceSeen = Integer.MAX_VALUE;
    }

    public static final class SearchResult {
        public final Optional<TargetCandidate> candidate;
        public final SearchDebug debug;

        public SearchResult(Optional<TargetCandidate> candidate, SearchDebug debug) {
            this.candidate = candidate == null ? Optional.empty() : candidate;
            this.debug = debug == null ? new SearchDebug() : debug;
        }
    }

    private final Host host;

    public AgilityTargetResolver(Host host) {
        this.host = host;
    }

    public SearchResult resolveNearestCandidate(Player local, Criteria criteria) {
        SearchDebug debug = new SearchDebug();
        if (local == null || local.getWorldLocation() == null) {
            return new SearchResult(Optional.empty(), debug);
        }
        if (criteria == null) {
            return new SearchResult(Optional.empty(), debug);
        }
        WorldPoint localPoint = local.getWorldLocation();
        debug.localPlayerPlane = localPoint.getPlane();
        TargetCandidate best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (TileObject candidate : host.nearbySceneObjects()) {
            debug.scannedObjects++;
            if (candidate == null || candidate.getWorldLocation() == null) {
                debug.missingWorldLocation++;
                continue;
            }
            int objectId = candidate.getId();
            if (criteria.targetObjectId > 0 && objectId != criteria.targetObjectId) {
                debug.rejectedObjectId++;
                continue;
            }
            String objectName = normalizeToken(host.resolveSceneObjectName(candidate));
            if (!criteria.targetObjectNameContains.isBlank() && !objectName.contains(criteria.targetObjectNameContains)) {
                debug.rejectedObjectName++;
                continue;
            }
            WorldPoint worldPoint = candidate.getWorldLocation();
            if (criteria.targetPlane >= 0 && worldPoint.getPlane() != criteria.targetPlane) {
                debug.rejectedPlane++;
                continue;
            }
            if (criteria.targetWorldX > 0 && worldPoint.getX() != criteria.targetWorldX) {
                debug.rejectedTargetWorldX++;
                continue;
            }
            if (criteria.targetWorldY > 0 && worldPoint.getY() != criteria.targetWorldY) {
                debug.rejectedTargetWorldY++;
                continue;
            }
            if (criteria.minWorldX > 0 && worldPoint.getX() < criteria.minWorldX) {
                debug.rejectedBounds++;
                continue;
            }
            if (criteria.maxWorldX > 0 && worldPoint.getX() > criteria.maxWorldX) {
                debug.rejectedBounds++;
                continue;
            }
            if (criteria.minWorldY > 0 && worldPoint.getY() < criteria.minWorldY) {
                debug.rejectedBounds++;
                continue;
            }
            if (criteria.maxWorldY > 0 && worldPoint.getY() > criteria.maxWorldY) {
                debug.rejectedBounds++;
                continue;
            }
            int distanceTiles = localPoint.distanceTo(worldPoint);
            if (distanceTiles >= 0) {
                debug.closestDistanceSeen = Math.min(debug.closestDistanceSeen, distanceTiles);
            }
            if (distanceTiles < 0 || distanceTiles > criteria.maxDistanceTiles) {
                debug.rejectedDistance++;
                continue;
            }
            debug.withinDistanceCandidates++;
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
        if (debug.closestDistanceSeen == Integer.MAX_VALUE) {
            debug.closestDistanceSeen = -1;
        }
        return new SearchResult(Optional.ofNullable(best), debug);
    }

    private static String normalizeToken(String raw) {
        String safe = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return safe.replaceAll("<[^>]*>", "").trim();
    }
}
