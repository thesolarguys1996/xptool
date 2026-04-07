package net.runelite.api;

public interface NPC extends Actor {
    int getIndex();

    int getId();

    NPCComposition getTransformedComposition();

    NPCComposition getComposition();
}
