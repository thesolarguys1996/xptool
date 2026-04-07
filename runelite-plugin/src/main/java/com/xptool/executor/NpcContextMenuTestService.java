package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

final class NpcContextMenuTestService {
    private static final int DEFAULT_MAX_DISTANCE_TILES = 12;
    private static final int MAX_DISTANCE_TILES_CAP = 30;
    private static final String DEFAULT_OPTION_KEYWORD = "dismiss";
    private static final long CURSOR_READY_HOLD_MS = 180L;

    interface Host {
        Player localPlayer();

        Iterable<NPC> npcs();

        Point resolveVariedNpcClickPoint(NPC npc);

        boolean isUsableCanvasPoint(Point point);

        boolean moveInteractionCursorToCanvasPoint(Point canvasPoint);

        boolean isCursorNearTarget(Point canvasPoint);

        boolean isTopMenuOptionOnNpc(NPC npc, String... optionKeywords);

        boolean clickPrimaryAt(Point canvasPoint);

        boolean selectContextMenuOptionAt(Point canvasPoint, String... optionKeywords);

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        CommandExecutor.CommandDecision accept(String reason, JsonObject detailsJson);

        CommandExecutor.CommandDecision reject(String reason);
    }

    private final Host host;
    private int cursorReadyNpcIndex = -1;
    private long cursorReadySinceMs = Long.MIN_VALUE;
    private int stableTargetNpcIndex = -1;
    private Point stableTargetCanvasPoint = null;

    NpcContextMenuTestService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision execute(JsonObject payload) {
        long now = System.currentTimeMillis();
        boolean followupOnly = asBoolean(payload == null ? null : payload.get("followupOnly"), false);

        Player local = host.localPlayer();
        if (local == null) {
            clearCursorReadyHold();
            return host.reject("npc_context_menu_test_player_unavailable");
        }

        Criteria criteria = parseCriteria(payload);
        Optional<TargetCandidate> candidateOpt = resolveNearestCandidate(local, criteria);
        if (candidateOpt.isEmpty()) {
            clearCursorReadyHold();
            return host.accept(
                "npc_context_menu_test_target_unavailable",
                host.details(
                    "targetNpcId", criteria.targetNpcId,
                    "targetNpcNameContains", criteria.targetNpcNameContains,
                    "maxDistanceTiles", criteria.maxDistanceTiles,
                    "optionKeywordPrimary", criteria.optionKeywords[0],
                    "dismissMode", criteria.dismissMode,
                    "followupOnly", followupOnly
                )
            );
        }

        TargetCandidate candidate = candidateOpt.get();
        Point targetPoint = resolveStableTargetPoint(candidate.npc);
        if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
            clearCursorReadyHold();
            return host.reject("npc_context_menu_test_click_point_unavailable");
        }
        boolean cursorNearTarget = host.isCursorNearTarget(targetPoint);
        if (!cursorNearTarget) {
            if (!host.moveInteractionCursorToCanvasPoint(targetPoint)) {
                return host.accept(
                    "npc_context_menu_test_cursor_move_pending",
                    host.details(
                        "npcId", candidate.npcId,
                        "npcIndex", candidate.npcIndex,
                        "npcName", candidate.npcName,
                        "distanceTiles", candidate.distanceTiles,
                        "optionKeywordPrimary", criteria.optionKeywords[0],
                        "dismissMode", criteria.dismissMode,
                        "followupOnly", followupOnly
                    )
                );
            }
            cursorNearTarget = host.isCursorNearTarget(targetPoint);
        }
        if (!cursorNearTarget) {
            clearCursorReadyHold();
            return host.accept(
                "npc_context_menu_test_cursor_settling",
                host.details(
                    "npcId", candidate.npcId,
                    "npcIndex", candidate.npcIndex,
                    "npcName", candidate.npcName,
                    "distanceTiles", candidate.distanceTiles,
                    "optionKeywordPrimary", criteria.optionKeywords[0],
                    "dismissMode", criteria.dismissMode,
                    "followupOnly", followupOnly
                )
            );
        }
        if (shouldDeferDismissAfterCursorReady(candidate, now)) {
            return host.accept(
                "npc_context_menu_test_cursor_ready_hold",
                host.details(
                    "npcId", candidate.npcId,
                    "npcIndex", candidate.npcIndex,
                    "npcName", candidate.npcName,
                    "distanceTiles", candidate.distanceTiles,
                    "targetsLocal", candidate.targetsLocal,
                    "localTargetsNpc", candidate.localTargetsNpc,
                    "readyAgeMs", Math.max(0L, now - cursorReadySinceMs),
                    "requiredHoldMs", CURSOR_READY_HOLD_MS,
                    "optionKeywordPrimary", criteria.optionKeywords[0],
                    "dismissMode", criteria.dismissMode,
                    "followupOnly", followupOnly
                )
            );
        }
        boolean clicked;
        String dispatchMode;
        if (criteria.dismissMode) {
            clicked = host.selectContextMenuOptionAt(targetPoint, criteria.optionKeywords);
            dispatchMode = "right_click_menu_option";
        } else {
            boolean topMenuMatchesExpected = host.isTopMenuOptionOnNpc(candidate.npc, criteria.optionKeywords);
            boolean allowContinuationClick = candidate.targetsLocal || candidate.localTargetsNpc;
            if (!topMenuMatchesExpected && !allowContinuationClick) {
                return host.accept(
                    "npc_context_menu_test_top_menu_not_expected_option",
                    host.details(
                        "npcId", candidate.npcId,
                        "npcIndex", candidate.npcIndex,
                        "npcName", candidate.npcName,
                        "distanceTiles", candidate.distanceTiles,
                        "targetsLocal", candidate.targetsLocal,
                        "localTargetsNpc", candidate.localTargetsNpc,
                        "optionKeywordPrimary", criteria.optionKeywords[0],
                        "dismissMode", criteria.dismissMode,
                        "followupOnly", followupOnly
                    )
                );
            }
            clicked = host.clickPrimaryAt(targetPoint);
            dispatchMode = "left_click_top_option";
        }
        if (clicked) {
            clearCursorReadyHold();
            return host.accept(
                "npc_context_menu_test_dispatched",
                host.details(
                    "npcId", candidate.npcId,
                    "npcIndex", candidate.npcIndex,
                    "npcName", candidate.npcName,
                    "distanceTiles", candidate.distanceTiles,
                    "targetsLocal", candidate.targetsLocal,
                    "localTargetsNpc", candidate.localTargetsNpc,
                    "optionKeywordPrimary", criteria.optionKeywords[0],
                    "dismissMode", criteria.dismissMode,
                    "dispatchMode", dispatchMode,
                    "followupOnly", followupOnly
                )
            );
        }
        clearCursorReadyHold();
        String failureReason = criteria.dismissMode
            ? "npc_context_menu_test_dismiss_menu_select_failed"
            : "npc_context_menu_test_left_click_failed";
        return host.accept(
            failureReason,
            host.details(
                "npcId", candidate.npcId,
                "npcIndex", candidate.npcIndex,
                "npcName", candidate.npcName,
                "distanceTiles", candidate.distanceTiles,
                "targetsLocal", candidate.targetsLocal,
                "localTargetsNpc", candidate.localTargetsNpc,
                "optionKeywordPrimary", criteria.optionKeywords[0],
                "dismissMode", criteria.dismissMode,
                "dispatchMode", dispatchMode,
                "followupOnly", followupOnly
            )
        );
    }

    private boolean shouldDeferDismissAfterCursorReady(TargetCandidate candidate, long now) {
        if (candidate == null || candidate.npc == null) {
            clearCursorReadyHold();
            return true;
        }
        int npcIndex = candidate.npc.getIndex();
        if (npcIndex < 0) {
            clearCursorReadyHold();
            return true;
        }
        if (npcIndex != cursorReadyNpcIndex || cursorReadySinceMs <= 0L) {
            cursorReadyNpcIndex = npcIndex;
            cursorReadySinceMs = now;
            return true;
        }
        long readyAgeMs = Math.max(0L, now - cursorReadySinceMs);
        return readyAgeMs < CURSOR_READY_HOLD_MS;
    }

    private void clearCursorReadyHold() {
        cursorReadyNpcIndex = -1;
        cursorReadySinceMs = Long.MIN_VALUE;
        stableTargetNpcIndex = -1;
        stableTargetCanvasPoint = null;
    }

    private Point resolveStableTargetPoint(NPC npc) {
        if (npc == null) {
            clearCursorReadyHold();
            return null;
        }
        int npcIndex = npc.getIndex();
        if (npcIndex < 0) {
            clearCursorReadyHold();
            return null;
        }
        Point resolved = host.resolveVariedNpcClickPoint(npc);
        if (resolved != null && host.isUsableCanvasPoint(resolved)) {
            stableTargetNpcIndex = npcIndex;
            stableTargetCanvasPoint = new Point(resolved);
            return new Point(stableTargetCanvasPoint);
        }
        if (npcIndex == stableTargetNpcIndex && host.isUsableCanvasPoint(stableTargetCanvasPoint)) {
            return new Point(stableTargetCanvasPoint);
        }
        return null;
    }

    private Optional<TargetCandidate> resolveNearestCandidate(Player local, Criteria criteria) {
        WorldPoint localPoint = local.getWorldLocation();
        if (localPoint == null) {
            return Optional.empty();
        }
        Actor localInteracting = local.getInteracting();
        TargetCandidate best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (NPC npc : host.npcs()) {
            if (npc == null) {
                continue;
            }
            NPCComposition comp = npc.getTransformedComposition();
            if (comp == null) {
                comp = npc.getComposition();
            }
            if (!matchesNpcAction(comp, criteria.optionKeywords)) {
                continue;
            }
            int resolvedId = resolveNpcId(npc, comp);
            if (criteria.targetNpcId > 0 && resolvedId != criteria.targetNpcId) {
                continue;
            }
            String normalizedName = normalizedNpcName(comp);
            if (!criteria.targetNpcNameContains.isEmpty() && !normalizedName.contains(criteria.targetNpcNameContains)) {
                continue;
            }
            WorldPoint npcPoint = npc.getWorldLocation();
            if (npcPoint == null) {
                continue;
            }
            int distance = localPoint.distanceTo(npcPoint);
            if (distance < 0 || distance > criteria.maxDistanceTiles) {
                continue;
            }
            Actor npcInteracting = npc.getInteracting();
            boolean targetsLocal = npcInteracting == local;
            boolean localTargetsNpc = localInteracting == npc;
            if (criteria.dismissMode && !targetsLocal && !localTargetsNpc) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = new TargetCandidate(
                    npc,
                    resolvedId,
                    npc.getIndex(),
                    normalizedName,
                    distance,
                    targetsLocal,
                    localTargetsNpc
                );
            }
        }
        return Optional.ofNullable(best);
    }

    private Criteria parseCriteria(JsonObject payload) {
        JsonObject safePayload = payload == null ? new JsonObject() : payload;
        int targetNpcId = asInt(safePayload.get("targetNpcId"), -1);
        int maxDistanceTiles = clamp(
            asInt(safePayload.get("maxDistanceTiles"), DEFAULT_MAX_DISTANCE_TILES),
            1,
            MAX_DISTANCE_TILES_CAP
        );
        String targetNpcNameContains = normalizeToken(asString(safePayload.get("targetNpcNameContains")));
        String[] optionKeywords = parseOptionKeywords(safePayload.get("optionKeywords"), safePayload.get("optionKeyword"));
        boolean dismissMode = isDismissMode(optionKeywords);
        return new Criteria(targetNpcId, targetNpcNameContains, maxDistanceTiles, optionKeywords, dismissMode);
    }

    private String[] parseOptionKeywords(JsonElement optionKeywordsElement, JsonElement optionKeywordElement) {
        List<String> keywords = new ArrayList<>();
        if (optionKeywordsElement != null && optionKeywordsElement.isJsonArray()) {
            for (JsonElement element : optionKeywordsElement.getAsJsonArray()) {
                String token = normalizeToken(asString(element));
                if (!token.isEmpty()) {
                    keywords.add(token);
                }
            }
        }
        if (keywords.isEmpty()) {
            String single = asString(optionKeywordElement);
            for (String part : single.split(",")) {
                String token = normalizeToken(part);
                if (!token.isEmpty()) {
                    keywords.add(token);
                }
            }
        }
        if (keywords.isEmpty()) {
            keywords.add(DEFAULT_OPTION_KEYWORD);
        }
        return keywords.toArray(new String[0]);
    }

    private static boolean isDismissMode(String[] optionKeywords) {
        if (optionKeywords == null || optionKeywords.length == 0) {
            return true;
        }
        for (String keyword : optionKeywords) {
            String token = normalizeToken(keyword);
            if (token.equals("dismiss") || token.startsWith("dismiss ")) {
                return true;
            }
        }
        return false;
    }

    private int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean asBoolean(JsonElement element, boolean fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            if (element.isJsonPrimitive()) {
                String value = asString(element).trim().toLowerCase(Locale.ROOT);
                if ("true".equals(value) || "1".equals(value) || "yes".equals(value)) {
                    return true;
                }
                if ("false".equals(value) || "0".equals(value) || "no".equals(value)) {
                    return false;
                }
            }
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return host.safeString(element.getAsString());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean matchesNpcAction(NPCComposition comp, String[] optionKeywords) {
        if (comp == null || !comp.isInteractible()) {
            return false;
        }
        String[] actions = comp.getActions();
        if (actions == null || actions.length == 0) {
            return false;
        }
        if (optionKeywords == null || optionKeywords.length == 0) {
            return false;
        }
        for (String action : actions) {
            String normalizedAction = normalizeToken(action);
            if (normalizedAction.isEmpty()) {
                continue;
            }
            for (String keywordRaw : optionKeywords) {
                String keyword = normalizeToken(keywordRaw);
                if (keyword.isEmpty()) {
                    continue;
                }
                if (containsPhraseWithBoundaries(normalizedAction, keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int resolveNpcId(NPC npc, NPCComposition comp) {
        if (npc != null && npc.getId() > 0) {
            return npc.getId();
        }
        if (comp != null && comp.getId() > 0) {
            return comp.getId();
        }
        return -1;
    }

    private static String normalizedNpcName(NPCComposition comp) {
        if (comp == null) {
            return "";
        }
        return normalizeToken(comp.getName());
    }

    private static String normalizeToken(String value) {
        String safe = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return safe.replaceAll("<[^>]*>", "").trim();
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Criteria {
        private final int targetNpcId;
        private final String targetNpcNameContains;
        private final int maxDistanceTiles;
        private final String[] optionKeywords;
        private final boolean dismissMode;

        private Criteria(
            int targetNpcId,
            String targetNpcNameContains,
            int maxDistanceTiles,
            String[] optionKeywords,
            boolean dismissMode
        ) {
            this.targetNpcId = targetNpcId;
            this.targetNpcNameContains = targetNpcNameContains == null ? "" : targetNpcNameContains;
            this.maxDistanceTiles = maxDistanceTiles;
            this.optionKeywords = (optionKeywords == null || optionKeywords.length == 0)
                ? new String[] { DEFAULT_OPTION_KEYWORD }
                : optionKeywords;
            this.dismissMode = dismissMode;
        }
    }

    private static final class TargetCandidate {
        private final NPC npc;
        private final int npcId;
        private final int npcIndex;
        private final String npcName;
        private final int distanceTiles;
        private final boolean targetsLocal;
        private final boolean localTargetsNpc;

        private TargetCandidate(
            NPC npc,
            int npcId,
            int npcIndex,
            String npcName,
            int distanceTiles,
            boolean targetsLocal,
            boolean localTargetsNpc
        ) {
            this.npc = npc;
            this.npcId = npcId;
            this.npcIndex = npcIndex;
            this.npcName = npcName == null ? "" : npcName;
            this.distanceTiles = distanceTiles;
            this.targetsLocal = targetsLocal;
            this.localTargetsNpc = localTargetsNpc;
        }
    }
}
