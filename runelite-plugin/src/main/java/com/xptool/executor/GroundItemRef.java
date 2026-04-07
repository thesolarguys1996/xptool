package com.xptool.executor;

import net.runelite.api.ItemLayer;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;

final class GroundItemRef {
    final Tile tile;
    final TileItem item;
    final ItemLayer itemLayer;
    final int itemId;
    final int quantity;
    final int worldX;
    final int worldY;
    final int plane;
    final int distanceTiles;

    GroundItemRef(
        Tile tile,
        TileItem item,
        ItemLayer itemLayer,
        int itemId,
        int quantity,
        int worldX,
        int worldY,
        int plane,
        int distanceTiles
    ) {
        this.tile = tile;
        this.item = item;
        this.itemLayer = itemLayer;
        this.itemId = itemId;
        this.quantity = Math.max(0, quantity);
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
        this.distanceTiles = distanceTiles;
    }
}
