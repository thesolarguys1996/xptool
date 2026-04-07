package com.xptool.systems;

import com.xptool.models.SceneCache;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;

public final class SceneCacheScanner {
    public interface Host {
        Client client();

        TileObject[] collectTreeObjectCandidates(Tile tile);

        boolean isNormalTreeObjectCandidate(TileObject object);

        boolean isOakTreeObjectCandidate(TileObject object);

        boolean isWillowTreeObjectCandidate(TileObject object);

        TileObject[] collectRockObjectCandidates(Tile tile);

        TileObject[] collectBankObjectCandidates(Tile tile);
    }

    private final Host host;

    public SceneCacheScanner(Host host) {
        this.host = host;
    }

    public SceneCache scanTopLevelPlane() {
        if (host == null) {
            return SceneCache.empty();
        }
        Client client = host.client();
        if (client == null) {
            return SceneCache.empty();
        }
        WorldView view = client.getTopLevelWorldView();
        if (view == null || view.getScene() == null) {
            return SceneCache.empty();
        }
        Tile[][][] tiles = view.getScene().getTiles();
        if (tiles == null || tiles.length == 0) {
            return SceneCache.empty();
        }
        int plane = view.getPlane();
        if (plane < 0 || plane >= tiles.length) {
            return SceneCache.empty();
        }
        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null) {
            return SceneCache.empty();
        }

        List<TileObject> trees = new ArrayList<>();
        List<TileObject> normalTrees = new ArrayList<>();
        List<TileObject> oakTrees = new ArrayList<>();
        List<TileObject> willowTrees = new ArrayList<>();
        List<TileObject> rocks = new ArrayList<>();
        List<TileObject> bankTargets = new ArrayList<>();

        for (int x = 0; x < planeTiles.length; x++) {
            Tile[] col = planeTiles[x];
            if (col == null) {
                continue;
            }
            for (int y = 0; y < col.length; y++) {
                Tile tile = col[y];
                if (tile == null) {
                    continue;
                }
                for (TileObject tree : host.collectTreeObjectCandidates(tile)) {
                    if (tree == null || tree.getWorldLocation() == null) {
                        continue;
                    }
                    trees.add(tree);
                    if (host.isNormalTreeObjectCandidate(tree)) {
                        normalTrees.add(tree);
                    }
                    if (host.isOakTreeObjectCandidate(tree)) {
                        oakTrees.add(tree);
                    }
                    if (host.isWillowTreeObjectCandidate(tree)) {
                        willowTrees.add(tree);
                    }
                }
                for (TileObject bank : host.collectBankObjectCandidates(tile)) {
                    if (bank != null && bank.getWorldLocation() != null) {
                        bankTargets.add(bank);
                    }
                }
                for (TileObject rock : host.collectRockObjectCandidates(tile)) {
                    if (rock != null && rock.getWorldLocation() != null) {
                        rocks.add(rock);
                    }
                }
            }
        }

        return new SceneCache(trees, normalTrees, oakTrees, willowTrees, rocks, bankTargets);
    }
}
