package com.xptool;

import com.xptool.models.Snapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

public final class XPToolSnapshotService {
    private static final boolean COMPACT_SNAPSHOTS = true;
    private static final int SNAPSHOT_OBJECT_RADIUS_TILES = 14;
    private static final int SNAPSHOT_NPC_RADIUS_TILES = 16;
    private static final int SNAPSHOT_GROUND_ITEM_RADIUS_TILES = 16;
    private static final int SNAPSHOT_MAX_NEARBY_OBJECTS = 96;
    private static final int SNAPSHOT_MAX_NEARBY_NPCS = 64;
    private static final int SNAPSHOT_MAX_NEARBY_GROUND_ITEMS = 96;
    private static final int SNAPSHOT_MAX_MENU_ENTRIES = 24;

    private final Client client;
    private final SnapshotEmitter emitter;

    public XPToolSnapshotService(Client client, SnapshotEmitter emitter) {
        this.client = client;
        this.emitter = emitter;
    }

    public Snapshot captureAndEmit() {
        GameState gameState = client.getGameState();
        Player localPlayer = client.getLocalPlayer();
        WorldPoint worldPoint = localPlayer == null ? null : localPlayer.getWorldLocation();

        boolean loggedIn = gameState == GameState.LOGGED_IN && worldPoint != null;
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        Widget bankContainerWidget = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        Widget bankItemContainerWidget = client.getWidget(InterfaceID.Bankmain.ITEMS);
        Widget shopContainerWidget = client.getWidget(InterfaceID.Shopmain.UNIVERSE);
        Widget shopItemContainerWidget = client.getWidget(InterfaceID.Shopmain.ITEMS);
        boolean bankOpen = bank != null
            || (bankContainerWidget != null && !bankContainerWidget.isHidden())
            || (bankItemContainerWidget != null && !bankItemContainerWidget.isHidden());
        boolean shopOpen = (shopContainerWidget != null && !shopContainerWidget.isHidden())
            || (shopItemContainerWidget != null && !shopItemContainerWidget.isHidden());
        GameStateSnapshot.InventorySlotSnapshot[] inventorySnapshot = loggedIn
            ? toInventorySnapshot(inventory)
            : new GameStateSnapshot.InventorySlotSnapshot[0];
        GameStateSnapshot.InventorySlotSnapshot[] bankInventorySnapshot =
            (loggedIn && bankOpen)
                ? toBankSnapshot(client, bank, true)
                : new GameStateSnapshot.InventorySlotSnapshot[0];
        GameStateSnapshot.InventorySlotSnapshot[] shopInventorySnapshot =
            (loggedIn && shopOpen)
                ? toShopSnapshotFromWidget(shopItemContainerWidget)
                : new GameStateSnapshot.InventorySlotSnapshot[0];
        int worldId = loggedIn ? client.getWorld() : -1;
        int playerAnimation = localPlayer == null ? -1 : localPlayer.getAnimation();
        int hitpointsCurrent = loggedIn ? client.getBoostedSkillLevel(Skill.HITPOINTS) : -1;
        int hitpointsMax = loggedIn ? client.getRealSkillLevel(Skill.HITPOINTS) : -1;
        int inventorySlotsUsed = countUsedSlots(inventorySnapshot);
        GameStateSnapshot.NearestTreeSnapshot nearestTree = loggedIn ? resolveNearestNormalTree(localPlayer) : null;
        Snapshot.NearbyObject[] nearbyObjects = loggedIn ? resolveNearbyObjects(localPlayer) : new Snapshot.NearbyObject[0];
        Snapshot.NearbyNpc[] nearbyNpcs = loggedIn ? resolveNearbyNpcs(localPlayer) : new Snapshot.NearbyNpc[0];
        Snapshot.NearbyGroundItem[] nearbyGroundItems =
            loggedIn ? resolveNearbyGroundItems(localPlayer) : new Snapshot.NearbyGroundItem[0];

        GameStateSnapshot.PlayerSnapshot playerSnapshot = worldPoint == null
            ? null
            : new GameStateSnapshot.PlayerSnapshot(
                worldPoint.getX(),
                worldPoint.getY(),
                worldPoint.getPlane(),
                worldPoint.getRegionID()
            );

        GameStateSnapshot snapshot = new GameStateSnapshot(
            1,
            "xptool",
            System.currentTimeMillis(),
            client.getTickCount(),
            gameState.name(),
            loggedIn,
            worldId,
            playerSnapshot,
            playerAnimation,
            hitpointsCurrent,
            hitpointsMax,
            bankOpen,
            shopOpen,
            inventorySlotsUsed,
            nearestTree,
            inventorySnapshot,
            bankInventorySnapshot,
            shopInventorySnapshot,
            toGameStateNearbyObjects(nearbyObjects),
            toGameStateNearbyNpcs(nearbyNpcs),
            toGameStateNearbyGroundItems(nearbyGroundItems)
        );
        Snapshot runtimeSnapshot = buildRuntimeSnapshot(
            loggedIn,
            worldPoint,
            playerAnimation,
            inventorySnapshot,
            nearbyObjects,
            nearbyNpcs,
            nearbyGroundItems
        );

        emitter.emit(snapshot);
        return runtimeSnapshot;
    }

    private Snapshot buildRuntimeSnapshot(
        boolean loggedIn,
        WorldPoint worldPoint,
        int playerAnimation,
        GameStateSnapshot.InventorySlotSnapshot[] inventorySnapshot,
        Snapshot.NearbyObject[] nearbyObjects,
        Snapshot.NearbyNpc[] nearbyNpcs,
        Snapshot.NearbyGroundItem[] nearbyGroundItems
    ) {
        Snapshot.PlayerPosition playerPosition = worldPoint == null
            ? null
            : new Snapshot.PlayerPosition(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());

        Snapshot.AnimationState animationState = new Snapshot.AnimationState(playerAnimation);
        Snapshot.InventoryItem[] inventory = toRuntimeInventory(inventorySnapshot);
        Snapshot.CameraState cameraState = new Snapshot.CameraState(
            client.getCameraX(),
            client.getCameraY(),
            client.getCameraZ(),
            client.getCameraPitch(),
            client.getCameraYaw()
        );
        Snapshot.SnapshotMenuEntry[] menuEntries = toRuntimeMenuEntries(currentMenuEntries());

        return new Snapshot(
            System.currentTimeMillis(),
            client.getTickCount(),
            loggedIn,
            playerPosition,
            animationState,
            inventory,
            nearbyObjects,
            nearbyNpcs,
            nearbyGroundItems,
            cameraState,
            menuEntries
        );
    }

    private static Snapshot.InventoryItem[] toRuntimeInventory(
        GameStateSnapshot.InventorySlotSnapshot[] inventorySnapshot
    ) {
        if (inventorySnapshot == null || inventorySnapshot.length == 0) {
            return new Snapshot.InventoryItem[0];
        }
        Snapshot.InventoryItem[] out = new Snapshot.InventoryItem[inventorySnapshot.length];
        int idx = 0;
        for (GameStateSnapshot.InventorySlotSnapshot item : inventorySnapshot) {
            if (item == null) {
                continue;
            }
            out[idx++] = new Snapshot.InventoryItem(item.slot, item.itemId, item.quantity);
        }
        if (idx == out.length) {
            return out;
        }
        Snapshot.InventoryItem[] compact = new Snapshot.InventoryItem[idx];
        System.arraycopy(out, 0, compact, 0, idx);
        return compact;
    }

    private static GameStateSnapshot.NearbyObjectSnapshot[] toGameStateNearbyObjects(
        Snapshot.NearbyObject[] nearbyObjects
    ) {
        if (nearbyObjects == null || nearbyObjects.length == 0) {
            return new GameStateSnapshot.NearbyObjectSnapshot[0];
        }
        GameStateSnapshot.NearbyObjectSnapshot[] out =
            new GameStateSnapshot.NearbyObjectSnapshot[nearbyObjects.length];
        for (int i = 0; i < nearbyObjects.length; i++) {
            Snapshot.NearbyObject object = nearbyObjects[i];
            out[i] = object == null
                ? null
                : new GameStateSnapshot.NearbyObjectSnapshot(
                    object.id,
                    object.name,
                    object.worldX,
                    object.worldY,
                    object.plane,
                    object.distance
                );
        }
        return out;
    }

    private static GameStateSnapshot.NearbyNpcSnapshot[] toGameStateNearbyNpcs(
        Snapshot.NearbyNpc[] nearbyNpcs
    ) {
        if (nearbyNpcs == null || nearbyNpcs.length == 0) {
            return new GameStateSnapshot.NearbyNpcSnapshot[0];
        }
        GameStateSnapshot.NearbyNpcSnapshot[] out =
            new GameStateSnapshot.NearbyNpcSnapshot[nearbyNpcs.length];
        for (int i = 0; i < nearbyNpcs.length; i++) {
            Snapshot.NearbyNpc npc = nearbyNpcs[i];
            out[i] = npc == null
                ? null
                : new GameStateSnapshot.NearbyNpcSnapshot(
                    npc.id,
                    npc.index,
                    npc.name,
                    npc.worldX,
                    npc.worldY,
                    npc.plane,
                    npc.animationId,
                    npc.distance
                );
        }
        return out;
    }

    private static GameStateSnapshot.NearbyGroundItemSnapshot[] toGameStateNearbyGroundItems(
        Snapshot.NearbyGroundItem[] nearbyGroundItems
    ) {
        if (nearbyGroundItems == null || nearbyGroundItems.length == 0) {
            return new GameStateSnapshot.NearbyGroundItemSnapshot[0];
        }
        GameStateSnapshot.NearbyGroundItemSnapshot[] out =
            new GameStateSnapshot.NearbyGroundItemSnapshot[nearbyGroundItems.length];
        for (int i = 0; i < nearbyGroundItems.length; i++) {
            Snapshot.NearbyGroundItem item = nearbyGroundItems[i];
            out[i] = item == null
                ? null
                : new GameStateSnapshot.NearbyGroundItemSnapshot(
                    item.id,
                    item.name,
                    item.quantity,
                    item.worldX,
                    item.worldY,
                    item.plane,
                    item.distance
                );
        }
        return out;
    }

    private Snapshot.NearbyObject[] resolveNearbyObjects(Player localPlayer) {
        if (localPlayer == null || localPlayer.getWorldLocation() == null) {
            return new Snapshot.NearbyObject[0];
        }
        WorldView view = client.getTopLevelWorldView();
        if (view == null || view.getScene() == null) {
            return new Snapshot.NearbyObject[0];
        }
        Tile[][][] tiles = view.getScene().getTiles();
        if (tiles == null || tiles.length == 0) {
            return new Snapshot.NearbyObject[0];
        }
        int plane = view.getPlane();
        if (plane < 0 || plane >= tiles.length) {
            return new Snapshot.NearbyObject[0];
        }
        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null) {
            return new Snapshot.NearbyObject[0];
        }

        WorldPoint localPos = localPlayer.getWorldLocation();
        Set<String> dedupe = new LinkedHashSet<>();
        List<Snapshot.NearbyObject> out = new ArrayList<>();
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
                TileObject[] candidates = collectAnyObjectCandidates(tile);
                for (TileObject object : candidates) {
                    if (object == null || object.getWorldLocation() == null) {
                        continue;
                    }
                    int dist = localPos.distanceTo(object.getWorldLocation());
                    if (dist < 0 || dist > SNAPSHOT_OBJECT_RADIUS_TILES) {
                        continue;
                    }
                    String key = object.getId() + ":" + object.getWorldLocation().getX() + ":" + object.getWorldLocation().getY();
                    if (!dedupe.add(key)) {
                        continue;
                    }
                    ObjectComposition comp = client.getObjectDefinition(object.getId());
                    String name = comp == null ? "" : safeString(comp.getName());
                    out.add(
                        new Snapshot.NearbyObject(
                            object.getId(),
                            name,
                            object.getWorldLocation().getX(),
                            object.getWorldLocation().getY(),
                            object.getWorldLocation().getPlane(),
                            dist
                        )
                    );
                }
            }
        }
        out.sort(
            Comparator
                .comparingInt((Snapshot.NearbyObject object) -> object.distance)
                .thenComparingInt(object -> object.worldX)
                .thenComparingInt(object -> object.worldY)
                .thenComparingInt(object -> object.id)
        );
        if (out.size() > SNAPSHOT_MAX_NEARBY_OBJECTS) {
            out = new ArrayList<>(out.subList(0, SNAPSHOT_MAX_NEARBY_OBJECTS));
        }
        return out.toArray(new Snapshot.NearbyObject[0]);
    }

    private Snapshot.NearbyNpc[] resolveNearbyNpcs(Player localPlayer) {
        if (localPlayer == null || localPlayer.getWorldLocation() == null) {
            return new Snapshot.NearbyNpc[0];
        }
        WorldView view = client.getTopLevelWorldView();
        if (view == null) {
            return new Snapshot.NearbyNpc[0];
        }
        WorldPoint localPos = localPlayer.getWorldLocation();
        List<Snapshot.NearbyNpc> out = new ArrayList<>();
        for (NPC npc : view.npcs()) {
            if (npc == null || npc.getWorldLocation() == null) {
                continue;
            }
            int dist = localPos.distanceTo(npc.getWorldLocation());
            if (dist < 0 || dist > SNAPSHOT_NPC_RADIUS_TILES) {
                continue;
            }
            NPCComposition comp = npc.getTransformedComposition();
            if (comp == null) {
                comp = npc.getComposition();
            }
            String name = comp == null ? "" : safeString(comp.getName());
            WorldPoint wp = npc.getWorldLocation();
            out.add(
                new Snapshot.NearbyNpc(
                    npc.getId(),
                    npc.getIndex(),
                    name,
                    wp.getX(),
                    wp.getY(),
                    wp.getPlane(),
                    npc.getAnimation(),
                    dist
                )
            );
        }
        out.sort(
            Comparator
                .comparingInt((Snapshot.NearbyNpc npc) -> npc.distance)
                .thenComparingInt(npc -> npc.worldX)
                .thenComparingInt(npc -> npc.worldY)
                .thenComparingInt(npc -> npc.id)
        );
        if (out.size() > SNAPSHOT_MAX_NEARBY_NPCS) {
            out = new ArrayList<>(out.subList(0, SNAPSHOT_MAX_NEARBY_NPCS));
        }
        return out.toArray(new Snapshot.NearbyNpc[0]);
    }

    private Snapshot.NearbyGroundItem[] resolveNearbyGroundItems(Player localPlayer) {
        if (localPlayer == null || localPlayer.getWorldLocation() == null) {
            return new Snapshot.NearbyGroundItem[0];
        }
        WorldView view = client.getTopLevelWorldView();
        if (view == null || view.getScene() == null) {
            return new Snapshot.NearbyGroundItem[0];
        }
        Tile[][][] tiles = view.getScene().getTiles();
        if (tiles == null || tiles.length == 0) {
            return new Snapshot.NearbyGroundItem[0];
        }
        int plane = view.getPlane();
        if (plane < 0 || plane >= tiles.length) {
            return new Snapshot.NearbyGroundItem[0];
        }
        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null) {
            return new Snapshot.NearbyGroundItem[0];
        }

        WorldPoint localPos = localPlayer.getWorldLocation();
        Set<String> dedupe = new LinkedHashSet<>();
        List<Snapshot.NearbyGroundItem> out = new ArrayList<>();
        for (int x = 0; x < planeTiles.length; x++) {
            Tile[] col = planeTiles[x];
            if (col == null) {
                continue;
            }
            for (int y = 0; y < col.length; y++) {
                Tile tile = col[y];
                if (tile == null || tile.getWorldLocation() == null) {
                    continue;
                }
                int dist = localPos.distanceTo(tile.getWorldLocation());
                if (dist < 0 || dist > SNAPSHOT_GROUND_ITEM_RADIUS_TILES) {
                    continue;
                }
                List<net.runelite.api.TileItem> items = tile.getGroundItems();
                if (items == null || items.isEmpty()) {
                    continue;
                }
                for (net.runelite.api.TileItem item : items) {
                    if (item == null) {
                        continue;
                    }
                    int itemId = item.getId();
                    int quantity = item.getQuantity();
                    if (itemId <= 0 || quantity <= 0) {
                        continue;
                    }
                    WorldPoint wp = tile.getWorldLocation();
                    String key = itemId + ":" + wp.getX() + ":" + wp.getY() + ":" + wp.getPlane();
                    if (!dedupe.add(key)) {
                        continue;
                    }
                    String name = "";
                    try {
                        name = safeString(client.getItemDefinition(itemId).getName());
                    } catch (Exception ignored) {
                        name = "";
                    }
                    out.add(
                        new Snapshot.NearbyGroundItem(
                            itemId,
                            name,
                            quantity,
                            wp.getX(),
                            wp.getY(),
                            wp.getPlane(),
                            dist
                        )
                    );
                }
            }
        }
        out.sort(
            Comparator
                .comparingInt((Snapshot.NearbyGroundItem item) -> item.distance)
                .thenComparingInt(item -> item.worldX)
                .thenComparingInt(item -> item.worldY)
                .thenComparingInt(item -> item.id)
        );
        if (out.size() > SNAPSHOT_MAX_NEARBY_GROUND_ITEMS) {
            out = new ArrayList<>(out.subList(0, SNAPSHOT_MAX_NEARBY_GROUND_ITEMS));
        }
        return out.toArray(new Snapshot.NearbyGroundItem[0]);
    }

    private static Snapshot.SnapshotMenuEntry[] toRuntimeMenuEntries(MenuEntry[] entries) {
        if (entries == null || entries.length == 0) {
            return new Snapshot.SnapshotMenuEntry[0];
        }
        int start = Math.max(0, entries.length - SNAPSHOT_MAX_MENU_ENTRIES);
        List<Snapshot.SnapshotMenuEntry> out = new ArrayList<>();
        for (int i = start; i < entries.length; i++) {
            MenuEntry entry = entries[i];
            if (entry == null) {
                continue;
            }
            String type = entry.getType() == null ? "" : entry.getType().name();
            out.add(
                new Snapshot.SnapshotMenuEntry(
                    safeString(entry.getOption()),
                    safeString(entry.getTarget()),
                    type,
                    entry.getIdentifier(),
                    entry.getParam0(),
                    entry.getParam1()
                )
            );
        }
        return out.toArray(new Snapshot.SnapshotMenuEntry[0]);
    }

    @SuppressWarnings("deprecation")
    private MenuEntry[] currentMenuEntries() {
        return client.getMenuEntries();
    }

    private static GameStateSnapshot.InventorySlotSnapshot[] toInventorySnapshot(ItemContainer container) {
        if (container == null || container.getItems() == null) {
            return new GameStateSnapshot.InventorySlotSnapshot[0];
        }

        Item[] items = container.getItems();
        if (!COMPACT_SNAPSHOTS) {
            GameStateSnapshot.InventorySlotSnapshot[] result = new GameStateSnapshot.InventorySlotSnapshot[items.length];
            for (int slot = 0; slot < items.length; slot++) {
                Item item = items[slot];
                int itemId = item == null ? -1 : item.getId();
                int quantity = item == null ? 0 : item.getQuantity();
                result[slot] = new GameStateSnapshot.InventorySlotSnapshot(slot, itemId, quantity);
            }
            return result;
        }

        List<GameStateSnapshot.InventorySlotSnapshot> compact = new ArrayList<>();
        for (int slot = 0; slot < items.length; slot++) {
            Item item = items[slot];
            int itemId = item == null ? -1 : item.getId();
            int quantity = item == null ? 0 : item.getQuantity();
            if (itemId <= 0 || quantity <= 0) {
                continue;
            }
            compact.add(new GameStateSnapshot.InventorySlotSnapshot(slot, itemId, quantity));
        }
        return compact.toArray(new GameStateSnapshot.InventorySlotSnapshot[0]);
    }

    private static GameStateSnapshot.InventorySlotSnapshot[] toBankSnapshot(
        Client client,
        ItemContainer bankContainer,
        boolean bankOpen
    ) {
        GameStateSnapshot.InventorySlotSnapshot[] fromContainer = toInventorySnapshot(bankContainer);
        if (hasAnyConcreteItem(fromContainer)) {
            return fromContainer;
        }
        if (!bankOpen) {
            return fromContainer;
        }
        GameStateSnapshot.InventorySlotSnapshot[] fromWidget = toBankSnapshotFromWidget(client);
        if (fromWidget.length > 0) {
            return fromWidget;
        }
        return fromContainer;
    }

    private static boolean hasAnyConcreteItem(GameStateSnapshot.InventorySlotSnapshot[] slots) {
        if (slots == null || slots.length == 0) {
            return false;
        }
        for (GameStateSnapshot.InventorySlotSnapshot slot : slots) {
            if (slot == null) {
                continue;
            }
            if (slot.itemId > 0 && slot.quantity > 0) {
                return true;
            }
        }
        return false;
    }

    private static int countUsedSlots(GameStateSnapshot.InventorySlotSnapshot[] slots) {
        if (slots == null || slots.length == 0) {
            return 0;
        }
        int used = 0;
        for (GameStateSnapshot.InventorySlotSnapshot slot : slots) {
            if (slot == null) {
                continue;
            }
            if (slot.itemId > 0 && slot.quantity > 0) {
                used++;
            }
        }
        return used;
    }

    private static GameStateSnapshot.InventorySlotSnapshot[] toBankSnapshotFromWidget(Client client) {
        if (client == null) {
            return new GameStateSnapshot.InventorySlotSnapshot[0];
        }
        Widget bankItemContainer = client.getWidget(InterfaceID.Bankmain.ITEMS);
        if (bankItemContainer == null) {
            return new GameStateSnapshot.InventorySlotSnapshot[0];
        }

        Widget[] dynamic = bankItemContainer.getDynamicChildren();
        if (dynamic == null || dynamic.length == 0) {
            return new GameStateSnapshot.InventorySlotSnapshot[0];
        }

        List<GameStateSnapshot.InventorySlotSnapshot> slots = new ArrayList<>();
        int slot = 0;
        for (Widget child : dynamic) {
            if (child == null) {
                continue;
            }
            int itemId = child.getItemId();
            int quantity = child.getItemQuantity();
            if (itemId <= 0 || quantity <= 0) {
                continue;
            }
            slots.add(new GameStateSnapshot.InventorySlotSnapshot(slot++, itemId, quantity));
        }
        return slots.toArray(new GameStateSnapshot.InventorySlotSnapshot[0]);
    }

    private static GameStateSnapshot.InventorySlotSnapshot[] toShopSnapshotFromWidget(Widget shopItemContainer) {
        if (shopItemContainer == null || shopItemContainer.isHidden()) {
            return new GameStateSnapshot.InventorySlotSnapshot[0];
        }

        List<GameStateSnapshot.InventorySlotSnapshot> slots = new ArrayList<>();
        Widget[] dynamic = shopItemContainer.getDynamicChildren();
        if (dynamic != null && dynamic.length > 0) {
            int slot = 0;
            for (Widget child : dynamic) {
                if (child == null || child.isHidden()) {
                    continue;
                }
                int itemId = child.getItemId();
                int quantity = child.getItemQuantity();
                if (itemId <= 0 || quantity <= 0) {
                    slot++;
                    continue;
                }
                slots.add(new GameStateSnapshot.InventorySlotSnapshot(slot, itemId, quantity));
                slot++;
            }
            return slots.toArray(new GameStateSnapshot.InventorySlotSnapshot[0]);
        }

        Widget[] children = shopItemContainer.getChildren();
        if (children == null || children.length == 0) {
            return new GameStateSnapshot.InventorySlotSnapshot[0];
        }
        for (int slot = 0; slot < children.length; slot++) {
            Widget child = children[slot];
            if (child == null || child.isHidden()) {
                continue;
            }
            int itemId = child.getItemId();
            int quantity = child.getItemQuantity();
            if (itemId <= 0 || quantity <= 0) {
                continue;
            }
            slots.add(new GameStateSnapshot.InventorySlotSnapshot(slot, itemId, quantity));
        }
        return slots.toArray(new GameStateSnapshot.InventorySlotSnapshot[0]);
    }

    private GameStateSnapshot.NearestTreeSnapshot resolveNearestNormalTree(Player localPlayer) {
        if (localPlayer == null || localPlayer.getWorldLocation() == null) {
            return null;
        }
        WorldView view = client.getTopLevelWorldView();
        if (view == null || view.getScene() == null) {
            return null;
        }
        Tile[][][] tiles = view.getScene().getTiles();
        if (tiles == null || tiles.length == 0) {
            return null;
        }
        int plane = view.getPlane();
        if (plane < 0 || plane >= tiles.length) {
            return null;
        }
        Tile[][] planeTiles = tiles[plane];
        if (planeTiles == null) {
            return null;
        }

        WorldPoint localPos = localPlayer.getWorldLocation();
        TileObject best = null;
        int bestDist = Integer.MAX_VALUE;
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
                TileObject[] candidates = collectTreeObjectCandidates(tile);
                for (TileObject candidate : candidates) {
                    if (!isNormalTreeObjectCandidate(candidate) || candidate.getWorldLocation() == null) {
                        continue;
                    }
                    int dist = localPos.distanceTo(candidate.getWorldLocation());
                    if (dist < 0 || dist >= bestDist) {
                        continue;
                    }
                    bestDist = dist;
                    best = candidate;
                }
            }
        }

        if (best == null || best.getWorldLocation() == null) {
            return null;
        }
        WorldPoint wp = best.getWorldLocation();
        return new GameStateSnapshot.NearestTreeSnapshot(
            best.getId(),
            wp.getX(),
            wp.getY(),
            bestDist,
            true
        );
    }

    private TileObject[] collectTreeObjectCandidates(Tile tile) {
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

    private static TileObject[] collectAnyObjectCandidates(Tile tile) {
        List<TileObject> out = new ArrayList<>();
        if (tile == null) {
            return new TileObject[0];
        }
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

    private boolean isTreeObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        ObjectComposition comp = client.getObjectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        if (hasChopAction(comp.getActions())) {
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

    private boolean isNormalTreeObjectCandidate(TileObject obj) {
        if (obj == null) {
            return false;
        }
        ObjectComposition comp = client.getObjectDefinition(obj.getId());
        if (comp == null) {
            return false;
        }
        if (!hasChopAction(comp.getActions())) {
            return false;
        }
        String name = safeString(comp.getName()).trim().toLowerCase(Locale.ROOT);
        return "tree".equals(name);
    }

    private static boolean hasChopAction(String[] actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            if (action == null) {
                continue;
            }
            String normalized = action.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("chop down") || normalized.equals("chop") || normalized.startsWith("chop ")) {
                return true;
            }
        }
        return false;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
