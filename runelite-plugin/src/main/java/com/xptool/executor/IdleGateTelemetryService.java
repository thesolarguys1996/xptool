package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.util.Locale;
import net.runelite.api.GameState;

final class IdleGateTelemetryService {
    interface Host {
        boolean isIdleRuntimeEnabled();
        boolean isIdleRuntimeArmedForCurrentContext();
        boolean isIdleRuntimeArmedForContext(IdleSkillContext context);
        boolean isClientWindowForegroundEligible();
        boolean isLogoutRuntimeActive();
        boolean shouldSuppressIdleForLogin();
        GameState gameState();
        boolean isBankOpen();
        boolean shouldSuppressIdleForTraversalOrBank();
        boolean isMouseMovePending();
        boolean isIdleMenuBlockActive();
        boolean isMenuOpen();
        boolean shouldOwnInteractionSession();
        IdleSkillContext resolveIdleSkillContext();
        boolean hasPendingCommandRows();
        boolean isIdleAnimationActiveNow();
        boolean hasActiveMotorProgramForOwner(String owner);
        boolean isClientCanvasFocused();
        FishingIdleMode resolveFishingIdleMode(IdleSkillContext context);
        boolean fishingIdleModeOverrideEnabled();
        IdleArmingService idleArmingService();
        boolean strictForegroundWindowGating();
        long suppressIdleForLoginUntilMs();
        boolean isLoginRuntimeActive();
        boolean isPrimaryLoginSubmitPromptVisible();
        boolean isSecondaryLoginSubmitPromptVisible();
        long currentTimeMs();
        long remainingFutureMs(long untilMs, long nowMs);
        IdleTraversalBankSuppressionGate idleTraversalBankSuppressionGate();
        void emit(String status, CommandRow row, String reason, JsonObject details, String eventType);
    }

    private final Host host;

    IdleGateTelemetryService(Host host) {
        this.host = host;
    }

    boolean isIdleActionWindowOpen() {
        if (!host.isIdleRuntimeEnabled()) {
            return false;
        }
        if (!host.isIdleRuntimeArmedForCurrentContext()) {
            return false;
        }
        if (!host.isClientWindowForegroundEligible()) {
            return false;
        }
        if (host.isLogoutRuntimeActive()) {
            return false;
        }
        if (host.shouldSuppressIdleForLogin()) {
            return false;
        }
        if (host.gameState() != GameState.LOGGED_IN) {
            return false;
        }
        if (host.isBankOpen()) {
            return false;
        }
        if (host.shouldSuppressIdleForTraversalOrBank()) {
            return false;
        }
        if (host.isMouseMovePending()) {
            return false;
        }
        return !host.isIdleMenuBlockActive();
    }

    boolean isIdleCameraWindowOpen() {
        if (!host.isIdleRuntimeEnabled()) {
            return false;
        }
        if (!host.isIdleRuntimeArmedForCurrentContext()) {
            return false;
        }
        if (!host.isClientWindowForegroundEligible()) {
            return false;
        }
        if (host.isLogoutRuntimeActive()) {
            return false;
        }
        if (host.shouldSuppressIdleForLogin()) {
            return false;
        }
        if (host.gameState() != GameState.LOGGED_IN) {
            return false;
        }
        if (host.isBankOpen()) {
            return false;
        }
        if (host.shouldSuppressIdleForTraversalOrBank()) {
            return false;
        }
        return !host.isIdleMenuBlockActive();
    }

    boolean isIdleInterActionWindowOpen() {
        if (!isIdleActionWindowOpen()) {
            return false;
        }
        if (!host.shouldOwnInteractionSession()) {
            return false;
        }
        IdleSkillContext idleContext = host.resolveIdleSkillContext();
        boolean skillingIdleContextActive =
            idleContext == IdleSkillContext.FISHING || idleContext == IdleSkillContext.WOODCUTTING;
        if (host.hasPendingCommandRows()
            && !host.isIdleAnimationActiveNow()
            && !skillingIdleContextActive) {
            return false;
        }
        return !host.hasActiveMotorProgramForOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
    }

    JsonObject idleWindowGateSnapshot() {
        IdleSkillContext idleContext = host.resolveIdleSkillContext();
        String idleActivityKey = IdleArmingService.activityKeyFromContext(idleContext);
        JsonObject details = new JsonObject();
        IdleArmingService idleArmingService = host.idleArmingService();
        details.addProperty("idleContext", idleActivityKey);
        details.addProperty("idleRuntimeArmed", idleArmingService.isArmedForActivity(idleActivityKey));
        details.addProperty(
            "idleOffscreenRuntimeArmed",
            idleArmingService.isOffscreenArmedForActivity(idleActivityKey)
        );
        details.addProperty("idleArmSource", idleArmingService.armSourceForActivity(idleActivityKey));
        details.addProperty(
            "idleOffscreenArmSource",
            idleArmingService.offscreenArmSourceForActivity(idleActivityKey)
        );
        details.addProperty("idleRuntimeArmedEffective", host.isIdleRuntimeArmedForContext(idleContext));
        details.addProperty("idleArmedActivityCount", idleArmingService.armedActivityCount());
        details.addProperty(
            "idleOffscreenArmedActivityCount",
            idleArmingService.offscreenArmedActivityCount()
        );
        details.addProperty("strictForegroundWindowGating", host.strictForegroundWindowGating());
        details.addProperty("clientCanvasFocused", host.isClientCanvasFocused());
        details.addProperty("idleBlockedByUnfocusedWindow", !host.isClientWindowForegroundEligible());
        details.addProperty(
            "configuredFishingIdleMode",
            String.valueOf(host.resolveFishingIdleMode(idleContext)).toLowerCase(Locale.ROOT)
        );
        details.addProperty("fishingIdleModeOverrideEnabled", host.fishingIdleModeOverrideEnabled());
        details.addProperty("gameState", String.valueOf(host.gameState()));
        details.addProperty("menuOpen", host.isMenuOpen());
        details.addProperty("mouseMovePending", host.isMouseMovePending());
        details.addProperty("loginRuntimeActive", host.isLoginRuntimeActive());
        details.addProperty("loginPrimaryPromptVisible", host.isPrimaryLoginSubmitPromptVisible());
        details.addProperty("loginSecondaryPromptVisible", host.isSecondaryLoginSubmitPromptVisible());
        long now = host.currentTimeMs();
        details.addProperty("loginIdleSuppressUntilMs", host.suppressIdleForLoginUntilMs());
        details.addProperty(
            "loginIdleSuppressRemainingMs",
            host.remainingFutureMs(host.suppressIdleForLoginUntilMs(), now)
        );
        details.addProperty("shouldOwnInteractionSession", host.shouldOwnInteractionSession());
        details.addProperty(
            "interactionMotorProgramActive",
            host.hasActiveMotorProgramForOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION)
        );
        boolean bankOpen = host.isBankOpen();
        boolean traversalBankSuppressed = host.shouldSuppressIdleForTraversalOrBank();
        details.addProperty("bankOpen", bankOpen);
        details.addProperty("idleBlockedByBankOpen", bankOpen);
        details.addProperty("idleBlockedByTraversalBank", traversalBankSuppressed);
        details.addProperty("idleBlockedByWalkingBanking", bankOpen || traversalBankSuppressed);
        details.addProperty(
            "idleTraversalBankSuppressUntilMs",
            host.idleTraversalBankSuppressionGate().timedSuppressUntilMs()
        );
        details.addProperty(
            "idleTraversalBankSuppressRemainingMs",
            host.remainingFutureMs(host.idleTraversalBankSuppressionGate().timedSuppressUntilMs(), now)
        );
        details.addProperty(
            "idleTraversalBankHardSuppressUntilMs",
            host.idleTraversalBankSuppressionGate().fishingHardSuppressUntilMs()
        );
        details.addProperty(
            "idleTraversalBankHardSuppressRemainingMs",
            host.remainingFutureMs(host.idleTraversalBankSuppressionGate().fishingHardSuppressUntilMs(), now)
        );
        details.addProperty(
            "idleBlockedByFishingWalkBankHardSuppression",
            host.idleTraversalBankSuppressionGate().isFishingHardSuppressedNow()
        );
        return details;
    }

    void emitIdleEvent(String reason, JsonObject details) {
        JsonObject safeDetails = details == null ? new JsonObject() : details;
        boolean bankOpen = host.isBankOpen();
        boolean traversalBankSuppressed = host.shouldSuppressIdleForTraversalOrBank();
        safeDetails.addProperty("idleBlockedByBankOpen", bankOpen);
        safeDetails.addProperty("idleBlockedByTraversalBank", traversalBankSuppressed);
        safeDetails.addProperty("idleBlockedByWalkingBanking", bankOpen || traversalBankSuppressed);
        host.emit("executed", null, reason, safeDetails, "IDLE");
    }
}
