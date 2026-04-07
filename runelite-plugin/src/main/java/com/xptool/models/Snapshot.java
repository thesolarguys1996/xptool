package com.xptool.models;

import java.util.Arrays;

public final class Snapshot {
    private static final InventoryItem[] EMPTY_INVENTORY = new InventoryItem[0];
    private static final NearbyObject[] EMPTY_OBJECTS = new NearbyObject[0];
    private static final NearbyNpc[] EMPTY_NPCS = new NearbyNpc[0];
    private static final NearbyGroundItem[] EMPTY_GROUND_ITEMS = new NearbyGroundItem[0];
    private static final SnapshotMenuEntry[] EMPTY_MENU_ENTRIES = new SnapshotMenuEntry[0];

    public final long capturedAtUnixMillis;
    public final int tick;
    public final boolean loggedIn;
    public final PlayerPosition playerPosition;
    public final AnimationState animationState;
    public final InventoryItem[] inventory;
    public final NearbyObject[] nearbyObjects;
    public final NearbyNpc[] npcs;
    public final NearbyGroundItem[] nearbyGroundItems;
    public final CameraState cameraState;
    public final SnapshotMenuEntry[] menuEntries;

    public Snapshot(
        long capturedAtUnixMillis,
        int tick,
        boolean loggedIn,
        PlayerPosition playerPosition,
        AnimationState animationState,
        InventoryItem[] inventory,
        NearbyObject[] nearbyObjects,
        NearbyNpc[] npcs,
        NearbyGroundItem[] nearbyGroundItems,
        CameraState cameraState,
        SnapshotMenuEntry[] menuEntries
    ) {
        this.capturedAtUnixMillis = capturedAtUnixMillis;
        this.tick = tick;
        this.loggedIn = loggedIn;
        this.playerPosition = playerPosition;
        this.animationState = animationState == null ? new AnimationState(-1) : animationState;
        this.inventory = inventory == null ? EMPTY_INVENTORY : Arrays.copyOf(inventory, inventory.length);
        this.nearbyObjects = nearbyObjects == null ? EMPTY_OBJECTS : Arrays.copyOf(nearbyObjects, nearbyObjects.length);
        this.npcs = npcs == null ? EMPTY_NPCS : Arrays.copyOf(npcs, npcs.length);
        this.nearbyGroundItems = nearbyGroundItems == null
            ? EMPTY_GROUND_ITEMS
            : Arrays.copyOf(nearbyGroundItems, nearbyGroundItems.length);
        this.cameraState = cameraState == null ? new CameraState(0, 0, 0, 0, 0) : cameraState;
        this.menuEntries = menuEntries == null ? EMPTY_MENU_ENTRIES : Arrays.copyOf(menuEntries, menuEntries.length);
    }

    public static Snapshot empty() {
        return new Snapshot(
            System.currentTimeMillis(),
            Integer.MIN_VALUE,
            false,
            null,
            new AnimationState(-1),
            EMPTY_INVENTORY,
            EMPTY_OBJECTS,
            EMPTY_NPCS,
            EMPTY_GROUND_ITEMS,
            new CameraState(0, 0, 0, 0, 0),
            EMPTY_MENU_ENTRIES
        );
    }

    public int inventorySlotsUsed() {
        int used = 0;
        for (InventoryItem item : inventory) {
            if (item == null) {
                continue;
            }
            if (item.itemId > 0 && item.quantity > 0) {
                used++;
            }
        }
        return used;
    }

    public boolean isInventoryFull() {
        return inventorySlotsUsed() >= 28;
    }

    public boolean hasInventoryItem(int itemId) {
        if (itemId <= 0) {
            return false;
        }
        for (InventoryItem item : inventory) {
            if (item == null) {
                continue;
            }
            if (item.itemId == itemId && item.quantity > 0) {
                return true;
            }
        }
        return false;
    }

    public static final class PlayerPosition {
        public final int worldX;
        public final int worldY;
        public final int plane;

        public PlayerPosition(int worldX, int worldY, int plane) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
        }
    }

    public static final class AnimationState {
        public final int animationId;

        public AnimationState(int animationId) {
            this.animationId = animationId;
        }
    }

    public static final class InventoryItem {
        public final int slot;
        public final int itemId;
        public final int quantity;

        public InventoryItem(int slot, int itemId, int quantity) {
            this.slot = slot;
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    public static final class NearbyObject {
        public final int id;
        public final String name;
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int distance;

        public NearbyObject(int id, String name, int worldX, int worldY, int plane, int distance) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.distance = distance;
        }
    }

    public static final class NearbyNpc {
        public final int id;
        public final int index;
        public final String name;
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int animationId;
        public final int distance;

        public NearbyNpc(
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

    public static final class NearbyGroundItem {
        public final int id;
        public final String name;
        public final int quantity;
        public final int worldX;
        public final int worldY;
        public final int plane;
        public final int distance;

        public NearbyGroundItem(int id, String name, int quantity, int worldX, int worldY, int plane, int distance) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.quantity = quantity;
            this.worldX = worldX;
            this.worldY = worldY;
            this.plane = plane;
            this.distance = distance;
        }
    }

    public static final class CameraState {
        public final int cameraX;
        public final int cameraY;
        public final int cameraZ;
        public final int pitch;
        public final int yaw;

        public CameraState(int cameraX, int cameraY, int cameraZ, int pitch, int yaw) {
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
            this.pitch = pitch;
            this.yaw = yaw;
        }
    }

    public static final class SnapshotMenuEntry {
        public final String option;
        public final String target;
        public final String type;
        public final int identifier;
        public final int param0;
        public final int param1;

        public SnapshotMenuEntry(
            String option,
            String target,
            String type,
            int identifier,
            int param0,
            int param1
        ) {
            this.option = option == null ? "" : option;
            this.target = target == null ? "" : target;
            this.type = type == null ? "" : type;
            this.identifier = identifier;
            this.param0 = param0;
            this.param1 = param1;
        }
    }
}
