package net.runelite.api;

import net.runelite.api.coords.WorldPoint;

public interface Actor {
    Actor getInteracting();

    WorldPoint getWorldLocation();
}
