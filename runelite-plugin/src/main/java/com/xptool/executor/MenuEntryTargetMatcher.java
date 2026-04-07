package com.xptool.executor;

import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

final class MenuEntryTargetMatcher {
    interface Host {
        MenuEntry[] currentMenuEntries();

        WorldView resolveWorldViewById(int worldViewId);
    }

    private final Host host;
    private final int worldPointMatchRadiusTiles;

    MenuEntryTargetMatcher(Host host, int worldPointMatchRadiusTiles) {
        this.host = host;
        this.worldPointMatchRadiusTiles = Math.max(0, worldPointMatchRadiusTiles);
    }

    boolean isTopMenuChopOnTree(TileObject targetObject) {
        MenuEntry[] entries = host.currentMenuEntries();
        if (entries == null || entries.length == 0) {
            return false;
        }
        for (MenuEntry entry : entries) {
            if (isChopEntryForTarget(entry, targetObject)) {
                return true;
            }
        }
        return false;
    }

    boolean isTopMenuMineOnRock(TileObject targetObject) {
        MenuEntry[] entries = host.currentMenuEntries();
        if (entries == null || entries.length == 0) {
            return false;
        }
        for (MenuEntry entry : entries) {
            if (isMineEntryForTarget(entry, targetObject)) {
                return true;
            }
        }
        return false;
    }

    boolean isTopMenuBankOnObject() {
        MenuEntry[] entries = host.currentMenuEntries();
        if (entries == null || entries.length == 0) {
            return false;
        }
        MenuEntry top = entries[entries.length - 1];
        if (top == null) {
            return false;
        }
        return MenuActionHeuristics.isBankOption(top.getOption())
            && MenuActionHeuristics.isGameObjectMenuAction(top.getType());
    }

    boolean isTopMenuOptionOnObject(TileObject targetObject, String... optionKeywords) {
        if (targetObject == null || targetObject.getWorldLocation() == null) {
            return false;
        }
        if (optionKeywords == null || optionKeywords.length == 0) {
            return false;
        }
        MenuEntry[] entries = host.currentMenuEntries();
        if (entries == null || entries.length == 0) {
            return false;
        }
        MenuEntry top = entries[entries.length - 1];
        if (top == null || !MenuActionHeuristics.isGameObjectMenuAction(top.getType())) {
            return false;
        }
        if (!matchesOption(top.getOption(), optionKeywords)) {
            return false;
        }
        WorldPoint targetWp = targetObject.getWorldLocation();
        WorldPoint entryWp = worldPointFromMenuEntry(top);
        if (entryWp != null) {
            return worldPointsMatch(entryWp, targetWp);
        }
        return targetObject.getId() > 0 && top.getIdentifier() == targetObject.getId();
    }

    boolean isTopMenuOptionOnGroundItem(GroundItemRef targetItem, String... optionKeywords) {
        if (targetItem == null) {
            return false;
        }
        if (optionKeywords == null || optionKeywords.length == 0) {
            return false;
        }
        MenuEntry[] entries = host.currentMenuEntries();
        if (entries == null || entries.length == 0) {
            return false;
        }
        MenuEntry top = entries[entries.length - 1];
        if (top == null || !MenuActionHeuristics.isGroundItemMenuAction(top.getType())) {
            return false;
        }
        if (!matchesOption(top.getOption(), optionKeywords)) {
            return false;
        }
        if (targetItem.itemId > 0 && top.getIdentifier() != targetItem.itemId) {
            return false;
        }
        WorldPoint entryWp = worldPointFromMenuEntry(top);
        if (entryWp != null) {
            return worldPointMatches(
                entryWp,
                targetItem.worldX,
                targetItem.worldY,
                targetItem.plane
            );
        }
        return targetItem.itemId > 0 && top.getIdentifier() == targetItem.itemId;
    }

    boolean hasAttackEntryOnNpc() {
        MenuEntry[] entries = host.currentMenuEntries();
        if (entries == null || entries.length == 0) {
            return false;
        }
        MenuEntry top = entries[entries.length - 1];
        return top != null
            && MenuActionHeuristics.isAttackOption(top.getOption())
            && MenuActionHeuristics.isNpcMenuAction(top.getType());
    }

    boolean isTopMenuDismissOnNpc(NPC npc) {
        return isTopMenuOptionOnNpc(npc, "dismiss");
    }

    boolean isTopMenuOptionOnNpc(NPC npc, String... optionKeywords) {
        if (npc == null || npc.getIndex() < 0) {
            return false;
        }
        if (optionKeywords == null || optionKeywords.length == 0) {
            return false;
        }
        MenuEntry[] entries = host.currentMenuEntries();
        if (entries == null || entries.length == 0) {
            return false;
        }
        MenuEntry top = entries[entries.length - 1];
        if (top == null) {
            return false;
        }
        if (!MenuActionHeuristics.isNpcMenuAction(top.getType())) {
            return false;
        }
        if (!matchesOption(top.getOption(), optionKeywords)) {
            return false;
        }
        if (top.getIdentifier() == npc.getIndex()) {
            return true;
        }
        String npcName = normalizedNpcName(npc);
        if (npcName.isBlank()) {
            return false;
        }
        String topTarget = normalizeMenuToken(top.getTarget());
        return containsPhraseWithBoundaries(topTarget, npcName);
    }

    private static String normalizedNpcName(NPC npc) {
        if (npc == null) {
            return "";
        }
        String raw = npc.getName();
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return normalizeMenuToken(raw);
    }

    private boolean isChopEntryForTarget(MenuEntry entry, TileObject targetObject) {
        if (entry == null || targetObject == null || targetObject.getWorldLocation() == null) {
            return false;
        }
        WorldPoint targetWp = targetObject.getWorldLocation();
        WorldPoint entryWp = worldPointFromMenuEntry(entry);
        if (entryWp != null) {
            if (!worldPointsMatch(entryWp, targetWp)) {
                return false;
            }
            return MenuActionHeuristics.isChopOption(entry.getOption())
                && MenuActionHeuristics.isGameObjectMenuAction(entry.getType());
        }

        // Optional fallback when entry tile metadata is unavailable.
        // Object ids are not stable across depletion/regrow state swaps.
        return MenuActionHeuristics.isChopOption(entry.getOption())
            && MenuActionHeuristics.isGameObjectMenuAction(entry.getType())
            && targetObject.getId() > 0
            && entry.getIdentifier() == targetObject.getId();
    }

    private boolean isMineEntryForTarget(MenuEntry entry, TileObject targetObject) {
        if (entry == null || targetObject == null || targetObject.getWorldLocation() == null) {
            return false;
        }
        WorldPoint targetWp = targetObject.getWorldLocation();
        WorldPoint entryWp = worldPointFromMenuEntry(entry);
        if (entryWp != null) {
            if (!worldPointsMatch(entryWp, targetWp)) {
                return false;
            }
            return MenuActionHeuristics.isMineOption(entry.getOption())
                && MenuActionHeuristics.isGameObjectMenuAction(entry.getType());
        }

        // When tile metadata is unavailable, prefer strict action validation only.
        // This avoids stale-id matches against depleted/regrowing variants.
        return MenuActionHeuristics.isMineOption(entry.getOption())
            && MenuActionHeuristics.isGameObjectMenuAction(entry.getType());
    }

    private WorldPoint worldPointFromMenuEntry(MenuEntry entry) {
        if (entry == null) {
            return null;
        }
        int sceneX = entry.getParam0();
        int sceneY = entry.getParam1();
        if (sceneX < 0 || sceneY < 0) {
            return null;
        }
        WorldView view = host.resolveWorldViewById(entry.getWorldViewId());
        if (view == null) {
            return null;
        }
        return WorldPoint.fromScene(view, sceneX, sceneY, view.getPlane());
    }

    private boolean worldPointsMatch(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return false;
        }
        int dist = a.distanceTo(b);
        return dist >= 0 && dist <= worldPointMatchRadiusTiles;
    }

    private boolean worldPointMatches(WorldPoint point, int worldX, int worldY, int plane) {
        if (point == null || worldX <= 0 || worldY <= 0 || plane < 0) {
            return false;
        }
        if (point.getPlane() != plane) {
            return false;
        }
        int distance = Math.max(Math.abs(point.getX() - worldX), Math.abs(point.getY() - worldY));
        return distance <= worldPointMatchRadiusTiles;
    }

    private static boolean matchesOption(String optionRaw, String... optionKeywords) {
        String option = normalizeMenuToken(optionRaw);
        if (option.isBlank() || optionKeywords == null || optionKeywords.length == 0) {
            return false;
        }
        for (String keywordRaw : optionKeywords) {
            String keyword = normalizeMenuToken(keywordRaw);
            if (keyword.isBlank()) {
                continue;
            }
            if (containsPhraseWithBoundaries(option, keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPhraseWithBoundaries(String corpus, String phrase) {
        if (corpus == null || phrase == null || corpus.isBlank() || phrase.isBlank()) {
            return false;
        }
        int from = 0;
        while (from <= corpus.length()) {
            int idx = corpus.indexOf(phrase, from);
            if (idx < 0) {
                return false;
            }
            int before = idx - 1;
            int after = idx + phrase.length();
            boolean leftBoundary = before < 0 || corpus.charAt(before) == ' ';
            boolean rightBoundary = after >= corpus.length() || corpus.charAt(after) == ' ';
            if (leftBoundary && rightBoundary) {
                return true;
            }
            from = idx + 1;
        }
        return false;
    }

    private static String normalizeMenuToken(String raw) {
        String safe = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        return safe.replaceAll("<[^>]*>", "").trim();
    }

}
