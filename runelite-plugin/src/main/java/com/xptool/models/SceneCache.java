package com.xptool.models;

import java.util.List;
import net.runelite.api.TileObject;

public final class SceneCache {
    private final List<TileObject> treeObjects;
    private final List<TileObject> normalTreeObjects;
    private final List<TileObject> oakTreeObjects;
    private final List<TileObject> willowTreeObjects;
    private final List<TileObject> rockObjects;
    private final List<TileObject> bankObjects;

    private static final SceneCache EMPTY = new SceneCache(
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of()
    );

    public SceneCache(
        List<TileObject> treeObjects,
        List<TileObject> normalTreeObjects,
        List<TileObject> oakTreeObjects,
        List<TileObject> willowTreeObjects,
        List<TileObject> rockObjects,
        List<TileObject> bankObjects
    ) {
        this.treeObjects = treeObjects == null ? List.of() : treeObjects;
        this.normalTreeObjects = normalTreeObjects == null ? List.of() : normalTreeObjects;
        this.oakTreeObjects = oakTreeObjects == null ? List.of() : oakTreeObjects;
        this.willowTreeObjects = willowTreeObjects == null ? List.of() : willowTreeObjects;
        this.rockObjects = rockObjects == null ? List.of() : rockObjects;
        this.bankObjects = bankObjects == null ? List.of() : bankObjects;
    }

    public static SceneCache empty() {
        return EMPTY;
    }

    public List<TileObject> treeObjects() {
        return treeObjects;
    }

    public List<TileObject> normalTreeObjects() {
        return normalTreeObjects;
    }

    public List<TileObject> oakTreeObjects() {
        return oakTreeObjects;
    }

    public List<TileObject> willowTreeObjects() {
        return willowTreeObjects;
    }

    public List<TileObject> rockObjects() {
        return rockObjects;
    }

    public List<TileObject> bankObjects() {
        return bankObjects;
    }
}
