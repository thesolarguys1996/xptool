package com.xptool.executor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.ItemLayer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

final class SceneQueryService {
    private final Client client;
    private final WorldViewResolver worldViewResolver;
    private final SceneObjectCandidateClassifier sceneObjectCandidateClassifier;
    private final int sceneObjectActionRadiusTiles;
    private final int groundItemActionRadiusTiles;

    SceneQueryService(
        Client client,
        WorldViewResolver worldViewResolver,
        SceneObjectCandidateClassifier sceneObjectCandidateClassifier,
        int sceneObjectActionRadiusTiles,
        int groundItemActionRadiusTiles
    ) {
        this.client = client;
        this.worldViewResolver = worldViewResolver;
        this.sceneObjectCandidateClassifier = sceneObjectCandidateClassifier;
        this.sceneObjectActionRadiusTiles = Math.max(0, sceneObjectActionRadiusTiles);
        this.groundItemActionRadiusTiles = Math.max(0, groundItemActionRadiusTiles);
    }

    Iterable<NPC> currentNpcs() {
        WorldView view = worldViewResolver.topLevel();
        if (view == null || view.npcs() == null) {
            return List.of();
        }
        ArrayList<NPC> npcs = new ArrayList<>();
        for (NPC npc : view.npcs()) {
            if (npc != null) {
                npcs.add(npc);
            }
        }
        return npcs;
    }

    Iterable<TileObject> currentNearbySceneObjects() {
        List<TileObject> out = new ArrayList<>();
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || localPlayer.getWorldLocation() == null) {
            return out;
        }
        WorldView view = worldViewResolver.topLevel();
        if (view == null || view.getScene() == null) {
            return out;
        }
        Tile[][][] tiles = view.getScene().getTiles();
        if (tiles == null || tiles.length == 0) {
            return out;
        }
        int plane = view.getPlane();
        if (plane < 0 || plane >= tiles.length) {
            return out;
        }
        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null) {
            return out;
        }
        WorldPoint localPos = localPlayer.getWorldLocation();
        Set<String> dedupe = new LinkedHashSet<>();
        for (Tile[] column : planeTiles) {
            if (column == null) {
                continue;
            }
            for (Tile tile : column) {
                if (tile == null) {
                    continue;
                }
                TileObject[] candidates = sceneObjectCandidateClassifier.collectAnyObjectCandidates(tile);
                for (TileObject candidate : candidates) {
                    if (candidate == null || candidate.getWorldLocation() == null) {
                        continue;
                    }
                    int distance = localPos.distanceTo(candidate.getWorldLocation());
                    if (distance < 0 || distance > sceneObjectActionRadiusTiles) {
                        continue;
                    }
                    WorldPoint worldPoint = candidate.getWorldLocation();
                    String key =
                        candidate.getId()
                            + ":"
                            + worldPoint.getX()
                            + ":"
                            + worldPoint.getY()
                            + ":"
                            + worldPoint.getPlane();
                    if (dedupe.add(key)) {
                        out.add(candidate);
                    }
                }
            }
        }
        return out;
    }

    Iterable<GroundItemRef> currentNearbyGroundItems() {
        List<GroundItemRef> out = new ArrayList<>();
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || localPlayer.getWorldLocation() == null) {
            return out;
        }
        WorldView view = worldViewResolver.topLevel();
        if (view == null || view.getScene() == null) {
            return out;
        }
        Tile[][][] tiles = view.getScene().getTiles();
        if (tiles == null || tiles.length == 0) {
            return out;
        }
        int plane = view.getPlane();
        if (plane < 0 || plane >= tiles.length) {
            return out;
        }
        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null) {
            return out;
        }
        WorldPoint localPos = localPlayer.getWorldLocation();
        Set<String> dedupe = new LinkedHashSet<>();
        for (Tile[] column : planeTiles) {
            if (column == null) {
                continue;
            }
            for (Tile tile : column) {
                if (tile == null || tile.getWorldLocation() == null) {
                    continue;
                }
                WorldPoint tileWorldPoint = tile.getWorldLocation();
                int distance = localPos.distanceTo(tileWorldPoint);
                if (distance < 0 || distance > groundItemActionRadiusTiles) {
                    continue;
                }
                ItemLayer itemLayer = tile.getItemLayer();
                if (itemLayer == null) {
                    continue;
                }
                List<TileItem> groundItems = tile.getGroundItems();
                if (groundItems == null || groundItems.isEmpty()) {
                    continue;
                }
                for (TileItem item : groundItems) {
                    if (item == null) {
                        continue;
                    }
                    int itemId = item.getId();
                    int quantity = item.getQuantity();
                    if (itemId <= 0 || quantity <= 0) {
                        continue;
                    }
                    String key = itemId
                        + ":"
                        + tileWorldPoint.getX()
                        + ":"
                        + tileWorldPoint.getY()
                        + ":"
                        + tileWorldPoint.getPlane();
                    if (!dedupe.add(key)) {
                        continue;
                    }
                    out.add(
                        new GroundItemRef(
                            tile,
                            item,
                            itemLayer,
                            itemId,
                            quantity,
                            tileWorldPoint.getX(),
                            tileWorldPoint.getY(),
                            tileWorldPoint.getPlane(),
                            distance
                        )
                    );
                }
            }
        }
        return out;
    }
}
