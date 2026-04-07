package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.sessions.BankSession;
import com.xptool.sessions.DropSession;
import com.xptool.sessions.InteractionSession;
import com.xptool.sessions.idle.FishingIdleMode;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.GameState;

final class CommandDispatchService {
    interface Host {
        int currentExecutorTick();
        CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject details);
        CommandExecutor.CommandDecision rejectDecision(String reason);
        JsonObject details(Object... kvPairs);
        String safeString(String value);

        CommandExecutor.CommandDecision stopAllRuntime();

        boolean loginBreakRuntimeEnabled();
        void applyIdleCadenceTuningFromPayload(JsonObject payload);
        boolean hasManualMetricsRuntimeSignalFor(String consumer, boolean emitWhenMissing);
        GameState gameState();
        boolean isLoginRuntimeActive();
        String loginRuntimeStateName();
        boolean startLoginRuntime();

        boolean startLogoutRuntime();
        void advanceLogoutRuntimeOnCurrentTick();
        boolean isLogoutRuntimeFailedHardStop();
        boolean isLogoutRuntimeSuccessful();
        String logoutRuntimeStateName();
        boolean isLogoutRuntimeActive();
        String logoutRuntimeLastFailureReason();

        Optional<FishingIdleMode> tryParseFishingIdleMode(String raw);
        FishingIdleMode configuredFishingIdleMode();
        void setConfiguredFishingIdleMode(FishingIdleMode mode);
        void setFishingIdleModeOverrideEnabled(boolean enabled);
        boolean idleActivityGateEnabled();
        Set<String> idleActivityAllowlist();
        IdleArmingService idleArmingService();
        String idleArmSourceFishingModeOverride();

        String pushMotorOwnerContext(String owner);
        String pushClickTypeContext(String clickType);
        void popMotorOwnerContext(String previousContext);
        void popClickTypeContext(String previousContext);
        boolean acquireOrRenewMotorOwner(String owner, long leaseMs);
        long motorLeaseMsForOwner(String owner);

        BankSession bankSession();
        DropSession dropSession();
        InteractionSession interactionSession();

        CommandExecutor.CommandDecision rejectUnsupportedCommandType();
    }

    private final Host host;

    CommandDispatchService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision execute(CommandRow row) {
        String commandType = row == null ? "" : row.commandType;
        String normalizedCommandType = normalizeCommandType(commandType);
        JsonObject commandPayload = row == null || row.commandPayload == null ? new JsonObject() : row.commandPayload;
        if ("STOP_ALL_RUNTIME".equals(normalizedCommandType)) {
            return host.stopAllRuntime();
        }
        if ("LOGIN_START_TEST".equals(normalizedCommandType)) {
            return executeLoginStartTest(commandPayload);
        }
        if ("LOGOUT_SAFE".equals(normalizedCommandType)) {
            return executeLogoutSafe(commandPayload);
        }
        if ("SET_FISHING_IDLE_MODE_SAFE".equals(normalizedCommandType)) {
            return executeSetFishingIdleModeSafe(commandPayload);
        }
        String motorOwner = CommandRoutingPolicy.resolveMotorOwnerForCommandType(
            commandType,
            ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP,
            ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK,
            ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
            host.dropSession()::supports,
            host.bankSession()::supports,
            host.interactionSession()::supports
        );
        String clickType = CommandRoutingPolicy.resolveClickTypeForCommandType(
            commandType,
            ExecutorMotorProfileCatalog.CLICK_TYPE_WOODCUT_WORLD,
            ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD
        );
        String previousMotorOwner = host.pushMotorOwnerContext(motorOwner);
        String previousClickType = host.pushClickTypeContext(clickType);
        try {
            if (!motorOwner.isEmpty() && !host.acquireOrRenewMotorOwner(motorOwner, host.motorLeaseMsForOwner(motorOwner))) {
                return host.acceptDecision(
                    "motor_lock_unavailable",
                    host.details("owner", motorOwner, "tick", host.currentExecutorTick())
                );
            }
            MotionProfile motionProfile = MotionProfile.resolveForCommandType(commandType);
            return CommandFamilyRouter.route(
                commandType,
                commandPayload,
                motionProfile,
                host.bankSession(),
                host.dropSession(),
                host.interactionSession(),
                host::rejectUnsupportedCommandType
            );
        } catch (Exception ex) {
            return host.rejectDecision("execution_exception:" + ex.getMessage());
        } finally {
            host.popClickTypeContext(previousClickType);
            host.popMotorOwnerContext(previousMotorOwner);
        }
    }

    private CommandExecutor.CommandDecision executeLoginStartTest(JsonObject payload) {
        if (!host.loginBreakRuntimeEnabled()) {
            return host.rejectDecision("login_break_runtime_disabled");
        }
        host.applyIdleCadenceTuningFromPayload(payload);
        if (!host.hasManualMetricsRuntimeSignalFor("login_runtime", true)) {
            return host.rejectDecision("login_start_test_manual_metrics_signal_missing");
        }
        GameState gameState = host.gameState();
        if (gameState == GameState.LOGGED_IN) {
            return host.acceptDecision(
                "login_start_test_ignored_logged_in",
                host.details("gameState", host.safeString(gameState == null ? "" : gameState.name()))
            );
        }
        if (host.isLoginRuntimeActive()) {
            return host.acceptDecision(
                "login_start_test_already_active",
                host.details(
                    "state", host.loginRuntimeStateName(),
                    "prefilled", true,
                    "credentialEntryEnabled", false
                )
            );
        }
        boolean started = host.startLoginRuntime();
        if (!started) {
            return host.rejectDecision("login_start_test_dispatch_failed");
        }
        return host.acceptDecision(
            "login_start_test_dispatched",
            host.details(
                "prefilled", true,
                "credentialEntryEnabled", false
            )
        );
    }

    private CommandExecutor.CommandDecision executeLogoutSafe(JsonObject payload) {
        host.applyIdleCadenceTuningFromPayload(payload);
        if (!host.hasManualMetricsRuntimeSignalFor("logout_runtime", true)) {
            return host.rejectDecision("logout_safe_manual_metrics_signal_missing");
        }
        String plannerTag = parsePlannerTag(payload, false);
        GameState gameState = host.gameState();
        if (gameState != GameState.LOGGED_IN && gameState != GameState.LOGGING_IN) {
            return host.acceptDecision(
                "logout_safe_ignored_not_logged_in",
                host.details(
                    "plannerTag", plannerTag,
                    "gameState", host.safeString(gameState == null ? "" : gameState.name()),
                    "tick", host.currentExecutorTick()
                )
            );
        }
        boolean started = host.startLogoutRuntime();
        if (!started) {
            return host.rejectDecision("logout_safe_start_failed");
        }
        host.advanceLogoutRuntimeOnCurrentTick();
        gameState = host.gameState();
        if (gameState != GameState.LOGGED_IN && gameState != GameState.LOGGING_IN) {
            return host.acceptDecision(
                "logout_safe_complete",
                host.details(
                    "plannerTag", plannerTag,
                    "state", host.logoutRuntimeStateName(),
                    "active", host.isLogoutRuntimeActive(),
                    "successful", host.isLogoutRuntimeSuccessful(),
                    "lastFailureReason", host.safeString(host.logoutRuntimeLastFailureReason()),
                    "tick", host.currentExecutorTick(),
                    "gameState", host.safeString(gameState == null ? "" : gameState.name())
                )
            );
        }
        if (host.isLogoutRuntimeFailedHardStop()) {
            return host.rejectDecision("logout_safe_runtime_failed");
        }
        String reason = host.isLogoutRuntimeSuccessful()
            ? "logout_safe_complete"
            : "logout_safe_dispatched";
        return host.acceptDecision(
            reason,
            host.details(
                "plannerTag", plannerTag,
                "state", host.logoutRuntimeStateName(),
                "active", host.isLogoutRuntimeActive(),
                "successful", host.isLogoutRuntimeSuccessful(),
                "lastFailureReason", host.safeString(host.logoutRuntimeLastFailureReason()),
                "tick", host.currentExecutorTick()
            )
        );
    }

    private CommandExecutor.CommandDecision executeSetFishingIdleModeSafe(JsonObject payload) {
        String modeRaw = parseString(payload, "mode", false);
        String plannerTag = parsePlannerTag(payload, true);
        Optional<FishingIdleMode> parsedMode = host.tryParseFishingIdleMode(modeRaw);
        if (parsedMode.isEmpty()) {
            return host.rejectDecision("set_fishing_idle_mode_invalid");
        }
        FishingIdleMode previousMode = host.configuredFishingIdleMode();
        FishingIdleMode nextMode = parsedMode.get();
        host.setFishingIdleModeOverrideEnabled(true);
        host.setConfiguredFishingIdleMode(nextMode);
        IdleArmingService idleArmingService = host.idleArmingService();
        String targetActivity = plannerTag.isBlank()
            ? ActivityIdlePolicyRegistry.ACTIVITY_FISHING
            : IdleArmingService.activityKeyFromPlannerTag(plannerTag);
        boolean allowArmForActivity = !host.idleActivityGateEnabled()
            || host.idleActivityAllowlist().contains(targetActivity);
        if (nextMode == FishingIdleMode.OFF) {
            idleArmingService.disarmActivity(targetActivity);
        } else if (allowArmForActivity) {
            idleArmingService.armActivity(
                targetActivity,
                nextMode,
                host.idleArmSourceFishingModeOverride()
            );
        }
        return host.acceptDecision(
            "set_fishing_idle_mode_applied",
            host.details(
                "mode", nextMode.name(),
                "previousMode", previousMode == null ? "" : previousMode.name(),
                "fishingIdleModeOverrideEnabled", true,
                "plannerTag", plannerTag,
                "idleModeActivity", targetActivity,
                "idleRuntimeArmed", idleArmingService.isArmedForActivity(targetActivity),
                "idleOffscreenRuntimeArmed", idleArmingService.isOffscreenArmedForActivity(targetActivity),
                "idleArmSource", idleArmingService.armSourceForActivity(targetActivity),
                "idleOffscreenArmSource", idleArmingService.offscreenArmSourceForActivity(targetActivity),
                "tick", host.currentExecutorTick()
            )
        );
    }

    private static String normalizeCommandType(String rawCommandType) {
        return safeString(rawCommandType).trim().toUpperCase(Locale.ROOT);
    }

    private static String parsePlannerTag(JsonObject payload, boolean normalizeLower) {
        return parseString(payload, "plannerTag", normalizeLower);
    }

    private static String parseString(JsonObject payload, String key, boolean normalizeLower) {
        if (payload == null || key == null || key.isBlank() || !payload.has(key)) {
            return "";
        }
        JsonElement element = payload.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return "";
        }
        String value = safeString(element.getAsString());
        if (normalizeLower) {
            return value.trim().toLowerCase(Locale.ROOT);
        }
        return value;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
