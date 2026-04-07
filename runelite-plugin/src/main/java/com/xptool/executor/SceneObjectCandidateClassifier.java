package com.xptool.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;

final class SceneObjectCandidateClassifier {
    private static final Set<Integer> MINING_MINEABLE_OBJECT_IDS = Set.of(11364, 11365);

    interface Host {
        ObjectComposition objectDefinition(int objectId);
    }

    private final Host host;

    SceneObjectCandidateClassifier(Host host) {
        this.host = host;
    }

    TileObject[] collectBankObjectCandidates(Tile tile) {
        List<TileObject> out = new ArrayList<>();
        WallObject wall = tile.getWallObject();
        if (isBankObjectCandidate(wall)) {
            out.add(wall);
        }
        DecorativeObject deco = tile.getDecorativeObject();
        if (isBankObjectCandidate(deco)) {
            out.add(deco);
        }
        GameObject[] gameObjects = tile.getGameObjects();
        if (gameObjects != null) {
            for (GameObject obj : gameObjects) {
                if (isBankObjectCandidate(obj)) {
                    out.add(obj);
                }
            }
        }
        return out.toArray(new TileObject[0]);
    }

    TileObject[] collectAnyObjectCandidates(Tile tile) {
        List<TileObject> out = new ArrayList<>();
        GroundObject ground = tile.getGroundObject();
        if (ground != null) {
            out.add(ground);
        }
        WallObject wall = tile.getWallObject();
        if (wall != null) {
            out.add(wall);
        }
        DecorativeObject deco = tile.getDecorativeObject();
        if (deco != null) {
            out.add(deco);
        }
        GameObject[] gameObjects = tile.getGameObjects();
        if (gameObjects != null) {
            for (GameObject obj : gameObjects) {
                if (obj != null) {
                    out.add(obj);
                }
            }
        }
        return out.toArray(new TileObject[0]);
    }

    TileObject[] collectTreeObjectCandidates(Tile tile) {
        List<TileObject> out = new ArrayList<>();
        WallObject wall = tile.getWallObject();
        if (isTreeObjectCandidate(wall)) {
            out.add(wall);
        }
        DecorativeObject deco = tile.getDecorativeObject();
        if (isTreeObjectCandidate(deco)) {
            out.add(deco);
        }
        GameObject[] gameObjects = tile.getGameObjects();
        if (gameObjects != null) {
            for (GameObject obj : gameObjects) {
                if (isTreeObjectCandidate(obj)) {
                    out.add(obj);
                }
            }
        }
        return out.toArray(new TileObject[0]);
    }

    TileObject[] collectRockObjectCandidates(Tile tile) {
        List<TileObject> out = new ArrayList<>();
        WallObject wall = tile.getWallObject();
        if (isRockObjectCandidate(wall)) {
            out.add(wall);
        }
        DecorativeObject deco = tile.getDecorativeObject();
        if (isRockObjectCandidate(deco)) {
            out.add(deco);
        }
        GameObject[] gameObjects = tile.getGameObjects();
        if (gameObjects != null) {
            for (GameObject obj : gameObjects) {
                if (isRockObjectCandidate(obj)) {
                    out.add(obj);
                }
            }
        }
        return out.toArray(new TileObject[0]);
    }

    boolean isNormalTreeObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        ObjectComposition comp = host.objectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        if (!MenuActionHeuristics.hasChopAction(comp.getActions())) {
            return false;
        }
        String name = safeString(comp.getName()).trim().toLowerCase(Locale.ROOT);
        return "tree".equals(name);
    }

    boolean isOakTreeObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        ObjectComposition comp = host.objectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        if (!MenuActionHeuristics.hasChopAction(comp.getActions())) {
            return false;
        }
        String name = safeString(comp.getName()).trim().toLowerCase(Locale.ROOT);
        return name.contains("oak");
    }

    boolean isWillowTreeObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        ObjectComposition comp = host.objectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        if (!MenuActionHeuristics.hasChopAction(comp.getActions())) {
            return false;
        }
        String name = safeString(comp.getName()).trim().toLowerCase(Locale.ROOT);
        return name.contains("willow");
    }

    private boolean isBankObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        ObjectComposition comp = host.objectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        if (WidgetActionResolver.actionIndex(comp.getActions(), "Bank") >= 0) {
            return true;
        }
        String name = safeString(comp.getName()).toLowerCase(Locale.ROOT);
        return (name.contains("bank booth") || name.contains("bank chest"))
            && WidgetActionResolver.actionIndex(comp.getActions(), "Use") < 0;
    }

    private boolean isTreeObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        ObjectComposition comp = host.objectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        if (MenuActionHeuristics.hasChopAction(comp.getActions())) {
            return true;
        }
        String name = safeString(comp.getName()).toLowerCase(Locale.ROOT);
        if (name.contains("stump")) {
            return false;
        }
        return name.contains("tree")
            || name.contains("oak")
            || name.contains("willow")
            || name.contains("maple")
            || name.contains("yew")
            || name.contains("magic")
            || name.contains("teak")
            || name.contains("mahogany");
    }

    boolean isRockObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        if (!MINING_MINEABLE_OBJECT_IDS.contains(obj.getId())) {
            return false;
        }
        ObjectComposition comp = host.objectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        // Treat only currently mineable objects as valid targets so depleted
        // rocks are not repeatedly re-clicked.
        return MenuActionHeuristics.hasMineAction(comp.getActions());
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
