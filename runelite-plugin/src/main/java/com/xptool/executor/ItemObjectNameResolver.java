package com.xptool.executor;

import java.util.function.IntFunction;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.TileObject;

final class ItemObjectNameResolver {
    private final IntFunction<String> objectNameLookup;
    private final IntFunction<String> itemNameLookup;

    ItemObjectNameResolver(IntFunction<String> objectNameLookup, IntFunction<String> itemNameLookup) {
        this.objectNameLookup = objectNameLookup;
        this.itemNameLookup = itemNameLookup;
    }

    static ItemObjectNameResolver fromClient(Client client) {
        return new ItemObjectNameResolver(
            objectId -> {
                if (client == null || objectId <= 0) {
                    return "";
                }
                try {
                    return safeString(client.getObjectDefinition(objectId).getName());
                } catch (Exception ignored) {
                    return "";
                }
            },
            itemId -> {
                if (client == null || itemId <= 0) {
                    return "";
                }
                try {
                    ItemComposition itemComposition = client.getItemDefinition(itemId);
                    return itemComposition == null ? "" : safeString(itemComposition.getName());
                } catch (Exception ignored) {
                    return "";
                }
            }
        );
    }

    String resolveSceneObjectName(TileObject targetObject) {
        if (targetObject == null || targetObject.getId() <= 0) {
            return "";
        }
        try {
            return safeString(objectNameLookup.apply(targetObject.getId()));
        } catch (Exception ignored) {
            return "";
        }
    }

    String resolveGroundItemName(int itemId) {
        if (itemId <= 0) {
            return "";
        }
        try {
            return safeString(itemNameLookup.apply(itemId));
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
