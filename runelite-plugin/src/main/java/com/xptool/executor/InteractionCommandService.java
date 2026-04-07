package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import net.runelite.api.Player;

final class InteractionCommandService {
    interface Host {
        CommandExecutor.CommandDecision accept(String reason, JsonObject details);

        CommandExecutor.CommandDecision reject(String reason);

        JsonObject details(Object... kvPairs);

        Player localPlayer();

        Optional<CommandExecutor.CommandDecision> maybeHandleBrutusDodgeFromEat(JsonObject payload, long now);

        int playerEatAnimationId();

        long combatLastEatDispatchAtMs();

        void setCombatLastEatDispatchAtMs(long atMs);

        long combatEatDispatchMinIntervalMs();

        String detectNearbyBrutusTelegraphName(Player local);

        int currentExecutorTick();

        int brutusLastTelegraphTick();

        int combatBrutusEatPriorityWindowTicks();

        long brutusLastDodgeAtMs();

        long combatBrutusPostDodgeHoldMs();

        int brutusLastNoSafeTileTick();

        boolean isBrutusNoSafeTilePressureActive();

        boolean isBrutusDodgeProgressActive(long now);

        int targetUnavailableStreak();

        int combatRecenterUnavailableStreakMin();

        boolean isBrutusNpcNearby(Player local, int scanRangeTiles);

        int combatBrutusNearbyScanRangeTiles();

        int brutusNoSafeTileStreak();

        int combatBrutusNoSafeTileRecoveryWindowTicks();

        int asInt(JsonElement element, int fallback);

        Optional<Integer> findInventorySlot(int itemId);

        boolean canPerformMotorActionNow();

        boolean clickInventorySlot(int slot);

        boolean nudgeCameraYawLeft();

        boolean nudgeCameraYawRight();

        boolean nudgeCameraPitchUp();

        boolean nudgeCameraPitchDown();

        JsonObject cameraLastNudgeDetails();
    }

    private final Host host;

    InteractionCommandService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision executeCameraNudgeSafe(JsonObject payload) {
        String direction = safeDirection(payload);
        boolean moved;
        switch (direction) {
            case "YAW_LEFT":
                moved = host.nudgeCameraYawLeft();
                break;
            case "YAW_RIGHT":
                moved = host.nudgeCameraYawRight();
                break;
            case "PITCH_UP":
                moved = host.nudgeCameraPitchUp();
                break;
            case "PITCH_DOWN":
                moved = host.nudgeCameraPitchDown();
                break;
            default:
                return host.reject("camera_nudge_direction_invalid");
        }
        if (!moved) {
            return host.accept(
                "camera_nudge_deferred",
                mergeDetails(host.details("direction", direction), host.cameraLastNudgeDetails())
            );
        }
        return host.accept(
            "camera_nudge_dispatched",
            mergeDetails(host.details("direction", direction), host.cameraLastNudgeDetails())
        );
    }

    CommandExecutor.CommandDecision executeEatFoodSafe(JsonObject payload) {
        Player local = host.localPlayer();
        long now = System.currentTimeMillis();
        Optional<CommandExecutor.CommandDecision> brutusDodgeDecision = host.maybeHandleBrutusDodgeFromEat(payload, now);
        if (brutusDodgeDecision.isPresent()) {
            return brutusDodgeDecision.get();
        }
        if (local != null && local.getAnimation() == host.playerEatAnimationId()) {
            return host.accept("eat_food_waiting_animation", host.details("animation", host.playerEatAnimationId()));
        }
        long sinceLastEatDispatchMs = now - host.combatLastEatDispatchAtMs();
        if (host.combatLastEatDispatchAtMs() > 0L && sinceLastEatDispatchMs < host.combatEatDispatchMinIntervalMs()) {
            return host.accept(
                "eat_food_cooldown_active",
                host.details("waitMsRemaining", host.combatEatDispatchMinIntervalMs() - sinceLastEatDispatchMs)
            );
        }

        String nearbyBrutusTelegraph = host.detectNearbyBrutusTelegraphName(local);
        int recentTelegraphTickDelta = host.currentExecutorTick() - host.brutusLastTelegraphTick();
        boolean recentBrutusTelegraphActive =
            host.brutusLastTelegraphTick() != Integer.MIN_VALUE
                && recentTelegraphTickDelta >= 0
                && recentTelegraphTickDelta <= host.combatBrutusEatPriorityWindowTicks();
        long sinceLastDodgeMs = now - host.brutusLastDodgeAtMs();
        boolean postDodgeHoldActive =
            sinceLastDodgeMs >= 0L && sinceLastDodgeMs < host.combatBrutusPostDodgeHoldMs();
        if (!"none".equals(nearbyBrutusTelegraph) || postDodgeHoldActive || recentBrutusTelegraphActive) {
            int waitTicksRemaining = recentBrutusTelegraphActive
                ? (host.combatBrutusEatPriorityWindowTicks() - recentTelegraphTickDelta)
                : 0;
            return host.accept(
                "eat_food_deferred_brutus_dodge_priority",
                host.details(
                    "telegraph", nearbyBrutusTelegraph,
                    "recentTelegraphActive", recentBrutusTelegraphActive,
                    "recentTelegraphTickDelta", recentTelegraphTickDelta,
                    "recentTelegraphWaitTicksRemaining", Math.max(0, waitTicksRemaining),
                    "postDodgeHoldActive", postDodgeHoldActive,
                    "waitMsRemaining", postDodgeHoldActive
                        ? (host.combatBrutusPostDodgeHoldMs() - sinceLastDodgeMs)
                        : 0L
                )
            );
        }

        int noSafeTileTickDelta = host.brutusLastNoSafeTileTick() == Integer.MIN_VALUE
            ? Integer.MAX_VALUE
            : host.currentExecutorTick() - host.brutusLastNoSafeTileTick();
        boolean noSafeTilePressureActive = host.isBrutusNoSafeTilePressureActive();
        boolean dodgeProgressActive = host.isBrutusDodgeProgressActive(now);
        boolean unavailablePressureActive = host.targetUnavailableStreak() >= host.combatRecenterUnavailableStreakMin();
        boolean brutusNearby = host.isBrutusNpcNearby(local, host.combatBrutusNearbyScanRangeTiles());
        if (brutusNearby && (noSafeTilePressureActive || dodgeProgressActive || unavailablePressureActive)) {
            int noSafeTicksRemaining = noSafeTilePressureActive
                ? (host.combatBrutusNoSafeTileRecoveryWindowTicks() - noSafeTileTickDelta)
                : 0;
            return host.accept(
                "eat_food_deferred_brutus_mobility_priority",
                host.details(
                    "noSafeTilePressureActive", noSafeTilePressureActive,
                    "noSafeTileTickDelta", noSafeTileTickDelta,
                    "noSafeTileWaitTicksRemaining", Math.max(0, noSafeTicksRemaining),
                    "noSafeTileStreak", host.brutusNoSafeTileStreak(),
                    "dodgeProgressActive", dodgeProgressActive,
                    "unavailablePressureActive", unavailablePressureActive,
                    "unavailableStreak", host.targetUnavailableStreak(),
                    "brutusNearby", brutusNearby
                )
            );
        }

        int itemId = host.asInt(payload == null ? null : payload.get("itemId"), -1);
        if (itemId <= 0) {
            return host.reject("eat_food_item_id_invalid");
        }
        Optional<Integer> slotOpt = host.findInventorySlot(itemId);
        if (slotOpt.isEmpty()) {
            return host.accept(
                "eat_food_unavailable",
                host.details("itemId", itemId)
            );
        }
        if (!host.canPerformMotorActionNow()) {
            return host.accept("eat_food_waiting_motor_cooldown", null);
        }
        int slot = slotOpt.get();
        if (!host.clickInventorySlot(slot)) {
            return host.accept(
                "eat_food_click_deferred",
                host.details("itemId", itemId, "slot", slot)
            );
        }
        host.setCombatLastEatDispatchAtMs(now);
        return host.accept(
            "eat_food_dispatched",
            host.details("itemId", itemId, "slot", slot)
        );
    }

    private static String safeDirection(JsonObject payload) {
        if (payload == null || !payload.has("direction")) {
            return "";
        }
        JsonElement direction = payload.get("direction");
        if (direction == null || direction.isJsonNull()) {
            return "";
        }
        return direction.getAsString().trim().toUpperCase();
    }

    private static JsonObject mergeDetails(JsonObject base, JsonObject extra) {
        JsonObject merged = base == null ? new JsonObject() : base.deepCopy();
        if (extra == null) {
            return merged;
        }
        for (String key : extra.keySet()) {
            merged.add(key, extra.get(key));
        }
        return merged;
    }
}
