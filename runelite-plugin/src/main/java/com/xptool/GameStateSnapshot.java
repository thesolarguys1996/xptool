package com.xptool;

public final class GameStateSnapshot {
    public final int schemaVersion;
    public final String source;
    public final long capturedAtUnixMillis;
    public final int tick;
    public final String gameState;
    public final boolean loggedIn;
    public final int worldId;
    public final PlayerSnapshot player;
    public final int playerAnimation;
    public final int hitpointsCurrent;
    public final int hitpointsMax;
    public final boolean bankOpen;
    public final boolean shopOpen;
    public final int inventorySlotsUsed;
    public final NearestTreeSnapshot nearestTree;
    public final InventorySlotSnapshot[] inventory;
    public final InventorySlotSnapshot[] bankInventory;
    public final InventorySlotSnapshot[] shopInventory;
    public final NearbyObjectSnapshot[] nearbyObjects;
    public final NearbyNpcSnapshot[] nearbyNpcs;
    public final NearbyGroundItemSnapshot[] nearbyGroundItems;

    public GameStateSnapshot(
        int schemaVersion,
        String source,
        long capturedAtUnixMillis,
        int tick,
        String gameState,
        boolean loggedIn,
        int worldId,
        PlayerSnapshot player,
        int playerAnimation,
        int hitpointsCurrent,
        int hitpointsMax,
        boolean bankOpen,
        boolean shopOpen,
        int inventorySlotsUsed,
        NearestTreeSnapshot nearestTree,
        InventorySlotSnapshot[] inventory,
        InventorySlotSnapshot[] bankInventory,
        InventorySlotSnapshot[] shopInventory,
        NearbyObjectSnapshot[] nearbyObjects,
        NearbyNpcSnapshot[] nearbyNpcs,
        NearbyGroundItemSnapshot[] nearbyGroundItems
    ) {
        this.schemaVersion = schemaVersion;
        this.source = source;
        this.capturedAtUnixMillis = capturedAtUnixMillis;
        this.tick = tick;
        this.gameState = gameState;
        this.loggedIn = loggedIn;
        this.worldId = worldId;
        this.player = player;
        this.playerAnimation = playerAnimation;
        this.hitpointsCurrent = hitpointsCurrent;
        this.hitpointsMax = hitpointsMax;
        this.bankOpen = bankOpen;
        this.shopOpen = shopOpen;
        this.inventorySlotsUsed = inventorySlotsUsed;
        this.nearestTree = nearestTree;
        this.inventory = inventory == null ? new InventorySlotSnapshot[0] : inventory;
        this.bankInventory = bankInventory == null ? new InventorySlotSnapshot[0] : bankInventory;
        this.shopInventory = shopInventory == null ? new InventorySlotSnapshot[0] : shopInventory;
        this.nearbyObjects = nearbyObjects == null ? new NearbyObjectSnapshot[0] : nearbyObjects;
        this.nearbyNpcs = nearbyNpcs == null ? new NearbyNpcSnapshot[0] : nearbyNpcs;
        this.nearbyGroundItems = nearbyGroundItems == null ? new NearbyGroundItemSnapshot[0] : nearbyGroundItems;
    }

    public static final class PlayerSnapshot {
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int regionId;

        public PlayerSnapshot(int worldX, int worldY, int plane, int regionId) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.regionId = regionId;
        }
    }

    public static final class InventorySlotSnapshot {
        public final int slot;
        public final int itemId;
        public final int quantity;

        public InventorySlotSnapshot(int slot, int itemId, int quantity) {
            this.slot = slot;
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    public static final class NearestTreeSnapshot {
        public final int id;
        public final int worldX;
        public final int worldY;
        public final int distance;
        public final boolean interactable;

        public NearestTreeSnapshot(int id, int worldX, int worldY, int distance, boolean interactable) {
            this.id = id;
            this.worldX = worldX;
            this.worldY = worldY;
            this.distance = distance;
            this.interactable = interactable;
        }
    }

    public static final class NearbyObjectSnapshot {
        public final int id;
        public final String name;
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int distance;

        public NearbyObjectSnapshot(int id, String name, int worldX, int worldY, int plane, int distance) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.distance = distance;
        }
    }

    public static final class NearbyNpcSnapshot {
        public final int id;
        public final int index;
        public final String name;
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int animationId;
        public final int distance;

        public NearbyNpcSnapshot(
            int id,
            int index,
            String name,
            int worldX,
            int worldY,
            int plane,
            int animationId,
            int distance
        ) {
            this.id = id;
            this.index = index;
            this.name = name == null ? "" : name;
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.animationId = animationId;
            this.distance = distance;
        }
    }

    public static final class NearbyGroundItemSnapshot {
        public final int id;
        public final String name;
        public final int quantity;
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int distance;

        public NearbyGroundItemSnapshot(
            int id,
            String name,
            int quantity,
            int worldX,
            int worldY,
            int plane,
            int distance
        ) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.quantity = quantity;
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.distance = distance;
        }
    }
}
