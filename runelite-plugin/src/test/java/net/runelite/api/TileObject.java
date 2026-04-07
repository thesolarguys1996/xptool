package net.runelite.api;

import net.runelite.api.coords.WorldPoint;

public interface TileObject {
    int getId();

    WorldPoint getWorldLocation();
}
