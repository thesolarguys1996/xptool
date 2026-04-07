package com.xptool.activities.fishing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.core.motor.MotorDispatchResult;
import com.xptool.core.motor.MotorDispatchStatus;
import com.xptool.core.runtime.FatigueSnapshot;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public final class FishingCommandService {
    private static final long BUSY_ANIMATION_RECENT_DISPATCH_GRACE_MS = 1600L;
    private static final int LEVEL_UP_DISMISS_CONTINUE_CHANCE_PERCENT = 84;
    private static final long LEVEL_UP_PROMPT_RESET_GRACE_MS = 900L;
    private static final long LEVEL_UP_DISMISS_RETRY_MIN_MS = 220L;
    private static final long LEVEL_UP_DISMISS_RETRY_MAX_MS = 1240L;
    private static final long LEVEL_UP_INITIAL_DISMISS_DELAY_MIN_MS = 90L;
    private static final long LEVEL_UP_INITIAL_DISMISS_DELAY_MAX_MS = 420L;
    private static final long LEVEL_UP_FAST_REFISH_WINDOW_MIN_MS = 900L;
    private static final long LEVEL_UP_FAST_REFISH_WINDOW_MAX_MS = 2100L;
    private static final long LEVEL_UP_FAST_REFISH_SAME_TARGET_COOLDOWN_MIN_MS = 180L;
    private static final long LEVEL_UP_FAST_REFISH_SAME_TARGET_COOLDOWN_MAX_MS = 520L;
    private static final String LEVEL_UP_STRATEGY_DISMISS_CONTINUE = "dismiss_continue";
    private static final long POST_DISPATCH_HOLD_MIN_MS = 7000L;
    private static final long POST_DISPATCH_HOLD_MAX_MS = 17000L;
    private static final long COLD_START_HOLD_MIN_MS = 1100L;
    private static final long COLD_START_HOLD_MAX_MS = 2400L;
    private static final long DUPLICATE_DISPATCH_GUARD_MIN_MS = 260L;
    private static final long DUPLICATE_DISPATCH_GUARD_MAX_MS = 520L;
    private static final long SAME_TARGET_MIN_RECLICK_COOLDOWN_MS = 620L;
    private static final double SAME_TARGET_COOLDOWN_JITTER_MIN_SCALE = 0.84;
    private static final double SAME_TARGET_COOLDOWN_JITTER_MAX_SCALE = 1.22;
    private static final long SAME_TARGET_COOLDOWN_JITTER_MIN_MS = -140L;
    private static final long SAME_TARGET_COOLDOWN_JITTER_MAX_MS = 260L;
    private static final long POST_DROP_FAST_RETRY_GRACE_MS = 2200L;
    private static final long POST_DROP_SAME_TARGET_RECLICK_COOLDOWN_MIN_MS = 680L;
    private static final long POST_DROP_SAME_TARGET_RECLICK_COOLDOWN_MAX_MS = 1650L;
    private static final long POST_DROP_HOLD_BYPASS_MIN_SINCE_LAST_DISPATCH_MS = 1300L;
    private static final long LIKELY_MISS_RECOVERY_MIN_AGE_MS = 900L;
    private static final int LIKELY_MISS_FAST_RECOVERY_BASE_CHANCE_PERCENT = 44;
    private static final int LIKELY_MISS_FAST_RECOVERY_FATIGUE_BONUS_MAX_PERCENT = 20;
    private static final double LIKELY_MISS_RECOVERY_COOLDOWN_SCALE = 0.78;
    private static final long LIKELY_MISS_RECOVERY_MIN_RECLICK_COOLDOWN_MS = 940L;
    private static final long DEPLETED_SPOT_REACQUIRE_MIN_AGE_MS = 1100L;
    private static final long DEPLETED_SPOT_REACQUIRE_DELAY_MIN_MS = 180L;
    private static final long DEPLETED_SPOT_REACQUIRE_DELAY_MAX_MS = 1511L;
    private static final int FISHING_MICRO_HESITATION_BASE_CHANCE_PERCENT = 24;
    private static final int FISHING_MICRO_HESITATION_SAME_TARGET_BONUS_PERCENT = 18;
    private static final int FISHING_MICRO_HESITATION_LONG_WINDOW_CHANCE_PERCENT = 34;
    private static final long FISHING_MICRO_HESITATION_SHORT_MIN_MS = 70L;
    private static final long FISHING_MICRO_HESITATION_SHORT_MAX_MS = 320L;
    private static final long FISHING_MICRO_HESITATION_LONG_MIN_MS = 340L;
    private static final long FISHING_MICRO_HESITATION_LONG_MAX_MS = 860L;
    private static final int RECENT_TARGET_HISTORY_SIZE = 8;
    private static final int RECENT_CLICK_POINT_HISTORY_SIZE = 10;
    private static final int RECENT_CLICK_REGION_HISTORY_SIZE = 10;
    private static final double RECENT_CLICK_POINT_REPEAT_EXCLUSION_PX = 6.8;
    private static final int RECENT_CLICK_REGION_CELL_SIZE_PX = 22;
    private static final int NPC_CLICK_POINT_RESAMPLE_ATTEMPTS = 5;
    private static final int INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MIN = 7;
    private static final int INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MAX = 12;
    private static final String TARGET_SELECTION_MODE_NEAREST = "nearest";
    private static final int TARGET_SELECTION_TOP_N = 4;
    private static final int TARGET_SELECTION_TOP_K_MIN = 2;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_PERCENT = 78;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MIN_PERCENT = -22;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MAX_PERCENT = 12;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MIN_PERCENT = 45;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MAX_PERCENT = 92;
    private static final double TARGET_SELECTION_RANK_EXPONENT_MIN = 1.72;
    private static final double TARGET_SELECTION_RANK_EXPONENT_MAX = 2.26;
    private static final double TARGET_SELECTION_RANK_MULTIPLIER_MIN = 2.4;
    private static final double TARGET_SELECTION_RANK_MULTIPLIER_MAX = 3.8;
    private static final double TARGET_SELECTION_DISTANCE_MULTIPLIER_MIN = 0.74;
    private static final double TARGET_SELECTION_DISTANCE_MULTIPLIER_MAX = 1.32;
    private static final double TARGET_SELECTION_WEIGHT_JITTER_MIN = 0.82;
    private static final double TARGET_SELECTION_WEIGHT_JITTER_MAX = 1.23;
    private static final double TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MIN = 0.86;
    private static final double TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MAX = 0.97;
    private static final double TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MIN = 0.72;
    private static final double TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MAX = 0.90;
    private static final double TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER = 0.82;
    private static final String TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED = "nearest_top_n_weighted";
    private static final String TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED_RECENT = "nearest_top_n_weighted_recent";
    private static final String TARGET_SELECTION_POLICY_NEAREST_TOP_N_NEAREST_PRIORITY = "nearest_top_n_nearest_priority";
    private static final String TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED_COOLDOWN_REROUTE =
        "nearest_top_n_weighted_cooldown_reroute";
    private static final String TARGET_SELECTION_POLICY_NEAREST_TOP_N_COOLDOWN_DEFER = "nearest_top_n_cooldown_defer";
    private static final String TARGET_SELECTION_POLICY_NEAREST_LOCKED_FALLBACK = "nearest_locked_fallback";
    private static final String TARGET_SELECTION_POLICY_FALLBACK = "fallback";

    private enum LevelUpRecoveryMode {
        DISMISS_CONTINUE,
        FAST_REFISH
    }

    public interface Host {
        boolean isDropSweepSessionActive();

        void endDropSweepSession();

        long lastDropSweepSessionEndedAtMs();

        void extendFishingRetryWindow();

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        Player currentPlayer();

        boolean isFishingLevelUpPromptVisible();

        boolean dismissFishingLevelUpPrompt();

        void clearFishingOutcomeWaitWindow();

        void clearFishingTargetAttempt();

        Set<Integer> parsePreferredNpcIds(JsonElement targetNpcIdElement, JsonElement targetNpcIdsElement);

        long fishingOutcomeWaitUntilMs();

        int fishingLastAttemptNpcIndex();

        WorldPoint fishingLastAttemptWorldPoint();

        long fishingApproachWaitUntilMs();

        Optional<NPC> resolveLockedFishingTarget(Set<Integer> preferredNpcIds);

        Optional<NPC> resolveNearestFishingTarget(Player local, Set<Integer> preferredNpcIds);

        List<NPC> resolveNearestFishingTargets(Player local, Set<Integer> preferredNpcIds, int maxTargets);

        void lockFishingTarget(NPC npc);

        void clearFishingInteractionWindows();

        void clearFishingInteractionWindowsPreserveDispatchSignal();

        Point resolveNpcClickPoint(NPC npc);

        boolean isUsableCanvasPoint(Point point);

        void clearFishingTargetLock();

        void rememberInteractionAnchorForNpc(NPC npc, Point point);

        long fishingLastDispatchAtMs();

        WorldPoint fishingLastDispatchWorldPoint();

        int fishingLastDispatchNpcIndex();

        long fishingSameTargetReclickCooldownMs();

        MotorDispatchResult dispatchFishingMoveAndClick(Point canvasPoint, ClickMotionSettings motion);

        void noteInteractionActivityNow();

        void noteFishingTargetAttempt(Player local, NPC targetNpc);

        void noteFishingDispatchAttempt(NPC targetNpc, long now);

        void beginFishingOutcomeWaitWindow();

        void incrementClicksDispatched();

        FatigueSnapshot fatigueSnapshot();

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        RuntimeDecision accept(String reason, JsonObject details);

        RuntimeDecision reject(String reason);
    }

    private final Host host;
    private final FishingHoldDebugTelemetry holdDebugTelemetry = new FishingHoldDebugTelemetry();
    private boolean levelUpPromptEpisodeActive = false;
    private long levelUpPromptLastSeenAtMs = 0L;
    private long levelUpDismissRetryAtMs = 0L;
    private long levelUpFastRefishUntilMs = 0L;
    private LevelUpRecoveryMode levelUpRecoveryMode = LevelUpRecoveryMode.FAST_REFISH;
    private long postDispatchHoldForDispatchAtMs = Long.MIN_VALUE;
    private long postDispatchHoldUntilMs = 0L;
    private long duplicateDispatchGuardForDispatchAtMs = Long.MIN_VALUE;
    private long duplicateDispatchGuardUntilMs = 0L;
    private long sameTargetCooldownForDispatchAtMs = Long.MIN_VALUE;
    private int sameTargetCooldownForNpcIndex = Integer.MIN_VALUE;
    private boolean sameTargetCooldownLevelUpFastRefish = false;
    private long sameTargetCooldownSampleMs = Long.MIN_VALUE;
    private long microHesitationForDispatchAtMs = Long.MIN_VALUE;
    private int microHesitationForNpcIndex = Integer.MIN_VALUE;
    private boolean microHesitationSameDispatchTarget = false;
    private long microHesitationUntilMs = 0L;
    private boolean microHesitationSampledForContext = false;
    private long depletedSpotReacquireDelayForDispatchAtMs = Long.MIN_VALUE;
    private long depletedSpotReacquireDelayUntilMs = 0L;
    private boolean coldStartHoldAppliedForCurrentEpisode = false;
    private long coldStartHoldUntilMs = 0L;
    private long lastSeenDispatchSignalAtMs = Long.MIN_VALUE;
    private final long[] recentTargetHistory = new long[RECENT_TARGET_HISTORY_SIZE];
    private int recentTargetHistoryWriteIndex = 0;
    private final long[] recentClickPointHistory = new long[RECENT_CLICK_POINT_HISTORY_SIZE];
    private int recentClickPointHistoryWriteIndex = 0;
    private final long[] recentClickRegionHistory = new long[RECENT_CLICK_REGION_HISTORY_SIZE];
    private int recentClickRegionHistoryWriteIndex = 0;
    private volatile long idleOffscreenSuppressionUntilMs = 0L;

    public FishingCommandService(Host host) {
        this.host = host;
        initializeRecentHistories();
    }

    public boolean isOffscreenIdleSuppressedNow() {
        return idleOffscreenSuppressionRemainingMs() > 0L;
    }

    public long idleOffscreenSuppressionRemainingMs() {
        long now = System.currentTimeMillis();
        long remaining = idleOffscreenSuppressionUntilMs - now;
        return Math.max(0L, remaining);
    }

    public RuntimeDecision executeFishNearestSpot(JsonObject payload, MotionProfile motionProfile) {
        if (host.isDropSweepSessionActive()) {
            host.endDropSweepSession();
        }
        host.extendFishingRetryWindow();
        FishingExecutionContext baseContext = createExecutionContext(payload, motionProfile, 0);
        if (baseContext.local == null) {
            return host.reject("fishing_player_unavailable");
        }
        RuntimeDecision levelUpDecision = maybeHandleLevelUpPrompt(baseContext);
        if (levelUpDecision != null) {
            return levelUpDecision;
        }
        boolean levelUpFastRefishActive = isLevelUpFastRefishActive(baseContext.now);
        int animation = resolvePlayerAnimation(baseContext.local);
        boolean animationActive = FishingTimingModel.isAnimationActive(animation);
        boolean animationDeferralTargetAvailable =
            animationActive && isFishingTargetAvailableForAnimationDeferral(baseContext);
        Optional<NPC> interactingFishingNpc = resolveInteractingFishingNpc(baseContext);
        boolean busyAnimationRecentDispatchGraceActive = isBusyAnimationRecentDispatchGraceActive(baseContext);
        boolean busyAnimationDeferralSignal =
            interactingFishingNpc.isPresent() || busyAnimationRecentDispatchGraceActive;
        if (animationActive
            && !levelUpFastRefishActive
            && animationDeferralTargetAvailable
            && busyAnimationDeferralSignal) {
            host.clearFishingOutcomeWaitWindow();
            host.clearFishingTargetAttempt();
            return acceptProgress(
                baseContext,
                "fishing_busy_animation_active",
                host.details(
                    "animation", animation,
                    "targetAvailableDuringAnimation", true,
                    "busyAnimationDeferralSignal", true,
                    "busyAnimationInteractingNpcIndex", interactingFishingNpc.map(NPC::getIndex).orElse(-1),
                    "busyAnimationInteractingNpcId", interactingFishingNpc.map(NPC::getId).orElse(-1),
                    "busyAnimationRecentDispatchGraceActive", busyAnimationRecentDispatchGraceActive,
                    "busyAnimationRecentDispatchGraceMs", BUSY_ANIMATION_RECENT_DISPATCH_GRACE_MS
                )
            );
        }
        FishingExecutionContext context = new FishingExecutionContext(
            baseContext.payload,
            baseContext.motion,
            baseContext.now,
            baseContext.fatigue,
            baseContext.local,
            baseContext.preferredNpcIds,
            baseContext.lastDispatchAtMs,
            baseContext.sinceLastDispatchMs,
            baseContext.lastDispatchNpcIndex,
            0,
            levelUpFastRefishActive
        );
        RuntimeDecision preTargetDecision = maybeAcceptPreTargetDelay(context);
        if (preTargetDecision != null) {
            return preTargetDecision;
        }
        FishingResolvedTarget target;
        try {
            target = resolveTargetForDispatch(context);
        } catch (IllegalStateException ex) {
            return host.reject(ex.getMessage());
        }
        if (target == null) {
            return acceptProgress(
                context,
                "fishing_target_unavailable",
                host.details("preferredNpcIdCount", context.preferredNpcIds.size())
            );
        }
        RuntimeDecision preDispatchDecision = maybeAcceptPreDispatchDelay(context, target);
        if (preDispatchDecision != null) {
            return preDispatchDecision;
        }
        return dispatchToTarget(context, target);
    }

    private Optional<NPC> resolveInteractingFishingNpc(FishingExecutionContext context) {
        if (context == null || context.local == null) {
            return Optional.empty();
        }
        Object interacting = invokeInteractingActor(context.local);
        if (!(interacting instanceof NPC)) {
            return Optional.empty();
        }
        NPC interactingNpc = (NPC) interacting;
        if (context.preferredNpcIds == null || context.preferredNpcIds.isEmpty()) {
            return Optional.of(interactingNpc);
        }
        return context.preferredNpcIds.contains(interactingNpc.getId())
            ? Optional.of(interactingNpc)
            : Optional.empty();
    }

    private static Object invokeInteractingActor(Player local) {
        if (local == null) {
            return null;
        }
        try {
            return local.getClass().getMethod("getInteracting").invoke(local);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int resolvePlayerAnimation(Player local) {
        if (local == null) {
            return -1;
        }
        try {
            Object value = local.getClass().getMethod("getAnimation").invoke(local);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception ignored) {
            return -1;
        }
        return -1;
    }

    private static boolean isBusyAnimationRecentDispatchGraceActive(FishingExecutionContext context) {
        if (context == null || context.lastDispatchAtMs <= 0L || context.sinceLastDispatchMs < 0L) {
            return false;
        }
        return context.sinceLastDispatchMs <= BUSY_ANIMATION_RECENT_DISPATCH_GRACE_MS;
    }

    private FishingExecutionContext createExecutionContext(
        JsonObject payload,
        MotionProfile motionProfile,
        int noAnimationRetryStreak
    ) {
        ClickMotionSettings motion = host.resolveClickMotion(payload, motionProfile);
        long now = System.currentTimeMillis();
        FatigueSnapshot fatigue = fatigueSnapshot();
        JsonObject safePayload = payload == null ? new JsonObject() : payload;
        Set<Integer> preferredNpcIds = host.parsePreferredNpcIds(
            safePayload.get("targetNpcId"),
            safePayload.get("targetNpcIds")
        );
        long lastDispatchAtMs = host.fishingLastDispatchAtMs();
        return new FishingExecutionContext(
            safePayload,
            motion,
            now,
            fatigue,
            host.currentPlayer(),
            preferredNpcIds,
            lastDispatchAtMs,
            now - lastDispatchAtMs,
            host.fishingLastDispatchNpcIndex(),
            noAnimationRetryStreak,
            false
        );
    }

    private RuntimeDecision maybeHandleLevelUpPrompt(FishingExecutionContext context) {
        boolean promptVisible = host.isFishingLevelUpPromptVisible();
        long now = context.now;
        if (!promptVisible) {
            if (levelUpPromptEpisodeActive && (now - levelUpPromptLastSeenAtMs) > LEVEL_UP_PROMPT_RESET_GRACE_MS) {
                levelUpPromptEpisodeActive = false;
                levelUpDismissRetryAtMs = 0L;
                levelUpFastRefishUntilMs = 0L;
                levelUpRecoveryMode = LevelUpRecoveryMode.FAST_REFISH;
            }
            return null;
        }

        levelUpPromptLastSeenAtMs = now;
        if (!levelUpPromptEpisodeActive) {
            levelUpPromptEpisodeActive = true;
            levelUpRecoveryMode = pickLevelUpRecoveryMode();
            levelUpDismissRetryAtMs = now + sampleLevelUpInitialDismissDelayMs();
            if (levelUpRecoveryMode == LevelUpRecoveryMode.FAST_REFISH) {
                armLevelUpFastRefishWindow(now);
            }
        }

        if (levelUpRecoveryMode == LevelUpRecoveryMode.FAST_REFISH) {
            if (isLevelUpFastRefishActive(now)) {
                return null;
            }
            // Fast refish window expired while prompt is still present.
            // Escalate to dismiss flow so we don't get stuck.
            levelUpRecoveryMode = LevelUpRecoveryMode.DISMISS_CONTINUE;
            levelUpDismissRetryAtMs = Math.min(levelUpDismissRetryAtMs <= 0L ? now : levelUpDismissRetryAtMs, now);
        }

        if (levelUpRecoveryMode == LevelUpRecoveryMode.DISMISS_CONTINUE) {
            if (now >= levelUpDismissRetryAtMs && host.dismissFishingLevelUpPrompt()) {
                armLevelUpFastRefishWindow(now);
                levelUpDismissRetryAtMs = now + sampleLevelUpDismissRetryDelayMs();
                return acceptProgress(
                    context,
                    "fishing_level_up_continue_dismissed",
                    host.details(
                        "strategy", LEVEL_UP_STRATEGY_DISMISS_CONTINUE,
                        "fastRefishWindowMs", Math.max(0L, levelUpFastRefishUntilMs - now)
                    )
                );
            }
            levelUpDismissRetryAtMs = Math.max(levelUpDismissRetryAtMs, now + sampleLevelUpDismissRetryDelayMs());
            long waitMs = Math.max(0L, levelUpDismissRetryAtMs - now);
            return acceptHold(
                context,
                "fishing_level_up_continue_pending",
                waitMs,
                host.details(
                    "strategy", LEVEL_UP_STRATEGY_DISMISS_CONTINUE,
                    "waitMsRemaining", waitMs
                )
            );
        }
        return null;
    }

    private boolean isLevelUpFastRefishActive(long now) {
        return now > 0L && now <= levelUpFastRefishUntilMs;
    }

    private void armLevelUpFastRefishWindow(long now) {
        long durationMs = sampleLevelUpFastRefishWindowMs();
        levelUpFastRefishUntilMs = Math.max(levelUpFastRefishUntilMs, now + durationMs);
    }

    private static LevelUpRecoveryMode pickLevelUpRecoveryMode() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < LEVEL_UP_DISMISS_CONTINUE_CHANCE_PERCENT) {
            return LevelUpRecoveryMode.DISMISS_CONTINUE;
        }
        return LevelUpRecoveryMode.FAST_REFISH;
    }

    private static long sampleLevelUpDismissRetryDelayMs() {
        return randomBetweenInclusive(LEVEL_UP_DISMISS_RETRY_MIN_MS, LEVEL_UP_DISMISS_RETRY_MAX_MS);
    }

    private static long sampleLevelUpInitialDismissDelayMs() {
        return randomBetweenInclusive(
            LEVEL_UP_INITIAL_DISMISS_DELAY_MIN_MS,
            LEVEL_UP_INITIAL_DISMISS_DELAY_MAX_MS
        );
    }

    private static long sampleLevelUpFastRefishWindowMs() {
        return randomBetweenInclusive(LEVEL_UP_FAST_REFISH_WINDOW_MIN_MS, LEVEL_UP_FAST_REFISH_WINDOW_MAX_MS);
    }

    private boolean isFishingTargetAvailableForAnimationDeferral(FishingExecutionContext context) {
        if (context == null || context.local == null) {
            return true;
        }
        try {
            Optional<NPC> lockedTarget = host.resolveLockedFishingTarget(context.preferredNpcIds);
            if (lockedTarget.isPresent()) {
                return true;
            }
            return host.resolveNearestFishingTarget(context.local, context.preferredNpcIds).isPresent();
        } catch (Exception ignored) {
            // Fail-safe to preserving animation defer behavior if target probing errors.
            return true;
        }
    }

    private static long randomBetweenInclusive(long minInclusive, long maxInclusive) {
        long lo = Math.min(minInclusive, maxInclusive);
        long hi = Math.max(minInclusive, maxInclusive);
        if (lo >= hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static double randomBetweenInclusive(double minInclusive, double maxInclusive) {
        double lo = Math.min(minInclusive, maxInclusive);
        double hi = Math.max(minInclusive, maxInclusive);
        if (!(hi > lo)) {
            return lo;
        }
        return ThreadLocalRandom.current().nextDouble(lo, hi);
    }

    private static int clampInt(int value, int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        return Math.max(lo, Math.min(hi, value));
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private RuntimeDecision maybeAcceptPreTargetDelay(FishingExecutionContext context) {
        if (context == null || context.now <= 0L) {
            return null;
        }
        long lastDropEndedAtMs = host.lastDropSweepSessionEndedAtMs();
        boolean recentDropCompletion =
            lastDropEndedAtMs > 0L
                && context.now > 0L
                && (context.now - lastDropEndedAtMs) <= POST_DROP_FAST_RETRY_GRACE_MS;
        if (recentDropCompletion) {
            coldStartHoldAppliedForCurrentEpisode = true;
            coldStartHoldUntilMs = 0L;
            return null;
        }
        long dispatchSignalAtMs = Math.max(0L, context.lastDispatchAtMs);
        if (dispatchSignalAtMs > 0L) {
            lastSeenDispatchSignalAtMs = dispatchSignalAtMs;
            coldStartHoldAppliedForCurrentEpisode = false;
            coldStartHoldUntilMs = 0L;
            return null;
        }
        if (lastSeenDispatchSignalAtMs > 0L) {
            lastSeenDispatchSignalAtMs = 0L;
            coldStartHoldAppliedForCurrentEpisode = false;
            coldStartHoldUntilMs = 0L;
        }
        if (coldStartHoldAppliedForCurrentEpisode) {
            return null;
        }
        if (coldStartHoldUntilMs <= 0L) {
            coldStartHoldUntilMs = context.now + randomBetweenInclusive(COLD_START_HOLD_MIN_MS, COLD_START_HOLD_MAX_MS);
        }
        if (context.now < coldStartHoldUntilMs) {
            long waitMs = coldStartHoldUntilMs - context.now;
            return acceptHold(
                context,
                "fishing_cold_start_hold_window",
                waitMs,
                host.details(
                    "waitMsRemaining", waitMs,
                    "coldStartHoldWindowMinMs", COLD_START_HOLD_MIN_MS,
                    "coldStartHoldWindowMaxMs", COLD_START_HOLD_MAX_MS
                )
            );
        }
        coldStartHoldAppliedForCurrentEpisode = true;
        coldStartHoldUntilMs = 0L;
        return null;
    }

    private FishingResolvedTarget resolveTargetForDispatch(FishingExecutionContext context) {
        Optional<NPC> lockedTargetOpt = host.resolveLockedFishingTarget(context.preferredNpcIds);
        List<NPC> nearestTargets = host.resolveNearestFishingTargets(
            context.local,
            context.preferredNpcIds,
            TARGET_SELECTION_TOP_N
        );
        TargetSelectionResult selection = selectDispatchTarget(
            context,
            lockedTargetOpt.orElse(null),
            nearestTargets
        );
        NPC selectedNpc = selection.selectedNpc;
        if (selectedNpc == null) {
            host.clearFishingInteractionWindowsPreserveDispatchSignal();
            return null;
        }
        host.lockFishingTarget(selectedNpc);

        boolean initialDispatchSelection = context.lastDispatchAtMs <= 0L || context.sinceLastDispatchMs < 0L;
        Point targetCanvas = initialDispatchSelection
            ? resolveRetrySafeNpcClickPoint(selectedNpc, true)
            : resolveRetrySafeNpcClickPoint(selectedNpc);
        if (targetCanvas == null || !host.isUsableCanvasPoint(targetCanvas)) {
            host.clearFishingInteractionWindowsPreserveDispatchSignal();
            host.clearFishingTargetLock();
            throw new IllegalStateException("fishing_click_point_unavailable");
        }
        host.rememberInteractionAnchorForNpc(selectedNpc, targetCanvas);

        WorldPoint localWorldPoint = context.local.getWorldLocation();
        WorldPoint targetWorldPoint = selectedNpc.getWorldLocation();
        int localDistanceToTarget = -1;
        if (targetWorldPoint != null && localWorldPoint != null) {
            int dist = localWorldPoint.distanceTo(targetWorldPoint);
            if (dist >= 0) {
                localDistanceToTarget = dist;
            }
        }
        boolean sameSpotReacquired = FishingTimingModel.worldPointsNear(
            targetWorldPoint,
            host.fishingLastDispatchWorldPoint(),
            FishingTimingModel.WORLDPOINT_RECLICK_RADIUS_TILES
        );
        boolean sameDispatchTarget = (selectedNpc.getIndex() >= 0 && selectedNpc.getIndex() == context.lastDispatchNpcIndex)
            || sameSpotReacquired;
        return new FishingResolvedTarget(
            selectedNpc,
            targetCanvas,
            selectedNpc.getId(),
            selectedNpc.getIndex(),
            targetWorldPoint,
            localDistanceToTarget,
            sameSpotReacquired,
            sameDispatchTarget,
            TARGET_SELECTION_MODE_NEAREST,
            selection.poolSize,
            selection.policy
        );
    }

    private TargetSelectionResult selectDispatchTarget(
        FishingExecutionContext context,
        NPC lockedTarget,
        List<NPC> nearestTargets
    ) {
        int selectionPoolLimit = sampleSelectionPoolLimit(nearestTargets);
        List<NPC> pool = topNCandidates(nearestTargets, selectionPoolLimit);
        int poolSize = pool.size();
        SelectionTuning selectionTuning = sampleSelectionTuning();
        WorldPoint localWorldPoint = context == null || context.local == null ? null : context.local.getWorldLocation();
        List<WeightedTargetCandidate> weightedCandidates = new ArrayList<>();
        boolean blockedByCooldown = false;
        WeightedTargetCandidate nearestViableCandidate = null;
        for (int index = 0; index < pool.size(); index++) {
            NPC candidate = pool.get(index);
            if (candidate == null) {
                continue;
            }
            if (isSameTargetWithinReclickCooldown(context, candidate)) {
                blockedByCooldown = true;
                continue;
            }
            boolean recentCandidate = isRecentlyUsedTarget(candidate, candidate.getWorldLocation());
            double weight = selectionWeight(index, candidate, localWorldPoint, recentCandidate, selectionTuning);
            if (weight <= 0.0) {
                continue;
            }
            WeightedTargetCandidate weightedCandidate = new WeightedTargetCandidate(candidate, weight, recentCandidate);
            if (nearestViableCandidate == null) {
                nearestViableCandidate = weightedCandidate;
            }
            weightedCandidates.add(weightedCandidate);
        }
        if (!blockedByCooldown
            && nearestViableCandidate != null
            && ThreadLocalRandom.current().nextInt(100) < selectionTuning.nearestPriorityChancePercent) {
            return TargetSelectionResult.of(
                nearestViableCandidate.npc,
                poolSize,
                TARGET_SELECTION_POLICY_NEAREST_TOP_N_NEAREST_PRIORITY
            );
        }
        if (!weightedCandidates.isEmpty()) {
            WeightedTargetCandidate selected = selectWeightedCandidate(weightedCandidates);
            if (selected != null && selected.npc != null) {
                String policy = blockedByCooldown
                    ? TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED_COOLDOWN_REROUTE
                    : selected.recentCandidate
                        ? TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED_RECENT
                        : TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED;
                return TargetSelectionResult.of(
                    selected.npc,
                    poolSize,
                    policy
                );
            }
        }
        NPC nearestFallback = firstNonNullCandidate(pool);
        if (nearestFallback != null) {
            if (blockedByCooldown) {
                return TargetSelectionResult.of(
                    nearestFallback,
                    poolSize,
                    TARGET_SELECTION_POLICY_NEAREST_TOP_N_COOLDOWN_DEFER
                );
            }
            boolean recentCandidate = isRecentlyUsedTarget(nearestFallback, nearestFallback.getWorldLocation());
            return TargetSelectionResult.of(
                nearestFallback,
                poolSize,
                recentCandidate
                    ? TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED_RECENT
                    : TARGET_SELECTION_POLICY_NEAREST_TOP_N_WEIGHTED
            );
        }
        if (lockedTarget != null) {
            return TargetSelectionResult.of(
                lockedTarget,
                poolSize,
                TARGET_SELECTION_POLICY_NEAREST_LOCKED_FALLBACK
            );
        }
        return TargetSelectionResult.of(null, poolSize, TARGET_SELECTION_POLICY_FALLBACK);
    }

    private boolean isSameTargetWithinReclickCooldown(FishingExecutionContext context, NPC candidate) {
        if (context == null || candidate == null || context.lastDispatchAtMs <= 0L || context.now <= 0L) {
            return false;
        }
        WorldPoint candidateWorldPoint = candidate.getWorldLocation();
        boolean sameSpotReacquired = FishingTimingModel.worldPointsNear(
            candidateWorldPoint,
            host.fishingLastDispatchWorldPoint(),
            FishingTimingModel.WORLDPOINT_RECLICK_RADIUS_TILES
        );
        boolean sameDispatchTarget = (candidate.getIndex() >= 0 && candidate.getIndex() == context.lastDispatchNpcIndex)
            || sameSpotReacquired;
        if (!sameDispatchTarget) {
            return false;
        }
        long sinceLastSameTargetDispatchMs = context.now - context.lastDispatchAtMs;
        if (sinceLastSameTargetDispatchMs < 0L) {
            return true;
        }
        long sameTargetCooldownMs = sampleBaseSameTargetCooldownMs(context, candidate.getIndex());
        return sinceLastSameTargetDispatchMs < sameTargetCooldownMs;
    }

    private static NPC firstNonNullCandidate(List<NPC> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (NPC candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static int sampleSelectionPoolLimit(List<NPC> nearestTargets) {
        int candidateCount = nearestTargets == null ? 0 : nearestTargets.size();
        int upperBound = Math.max(1, Math.min(TARGET_SELECTION_TOP_N, candidateCount));
        int lowerBound = Math.min(TARGET_SELECTION_TOP_K_MIN, upperBound);
        if (upperBound <= lowerBound) {
            return upperBound;
        }
        return ThreadLocalRandom.current().nextInt(lowerBound, upperBound + 1);
    }

    private static SelectionTuning sampleSelectionTuning() {
        int nearestChanceJitter = (int) randomBetweenInclusive(
            TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MIN_PERCENT,
            TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MAX_PERCENT
        );
        int nearestPriorityChancePercent = clampInt(
            TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_PERCENT + nearestChanceJitter,
            TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MIN_PERCENT,
            TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MAX_PERCENT
        );
        double rankExponent = randomBetweenInclusive(
            TARGET_SELECTION_RANK_EXPONENT_MIN,
            TARGET_SELECTION_RANK_EXPONENT_MAX
        );
        double rankMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_RANK_MULTIPLIER_MIN,
            TARGET_SELECTION_RANK_MULTIPLIER_MAX
        );
        double distanceMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_DISTANCE_MULTIPLIER_MIN,
            TARGET_SELECTION_DISTANCE_MULTIPLIER_MAX
        );
        double weightJitter = randomBetweenInclusive(
            TARGET_SELECTION_WEIGHT_JITTER_MIN,
            TARGET_SELECTION_WEIGHT_JITTER_MAX
        );
        double recentNearestMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MIN,
            TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MAX
        );
        double recentMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MIN,
            TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MAX
        );
        return new SelectionTuning(
            nearestPriorityChancePercent,
            rankExponent,
            rankMultiplier,
            distanceMultiplier,
            weightJitter,
            recentNearestMultiplier,
            recentMultiplier
        );
    }

    private static double selectionWeight(
        int rank,
        NPC candidate,
        WorldPoint localWorldPoint,
        boolean recentCandidate,
        SelectionTuning tuning
    ) {
        if (candidate == null) {
            return 0.0;
        }
        SelectionTuning effectiveTuning = tuning == null ? SelectionTuning.defaults() : tuning;
        int boundedRank = Math.max(0, rank);
        double rankWeight =
            1.0 / Math.pow(1.0 + (double) boundedRank, Math.max(1.1, effectiveTuning.rankExponent));
        double distanceWeight = 0.0;
        if (localWorldPoint != null && candidate.getWorldLocation() != null) {
            int distance = FishingTimingModel.worldDistance(localWorldPoint, candidate.getWorldLocation());
            if (distance >= 0) {
                distanceWeight = 1.0 / (1.0 + (double) distance);
            }
        }
        double weight =
            (rankWeight * Math.max(0.1, effectiveTuning.rankMultiplier))
                + (distanceWeight * Math.max(0.1, effectiveTuning.distanceMultiplier));
        if (recentCandidate) {
            weight *= boundedRank == 0
                ? effectiveTuning.recentNearestMultiplier
                : effectiveTuning.recentMultiplier;
        }
        weight *= Math.max(0.20, effectiveTuning.weightJitter);
        return Math.max(0.0, weight);
    }

    private static WeightedTargetCandidate selectWeightedCandidate(List<WeightedTargetCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        double totalWeight = 0.0;
        WeightedTargetCandidate fallback = null;
        for (WeightedTargetCandidate candidate : candidates) {
            if (candidate == null || candidate.npc == null || candidate.weight <= 0.0) {
                continue;
            }
            totalWeight += candidate.weight;
            fallback = candidate;
        }
        if (fallback == null || totalWeight <= 0.0) {
            return fallback;
        }
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulativeWeight = 0.0;
        for (WeightedTargetCandidate candidate : candidates) {
            if (candidate == null || candidate.npc == null || candidate.weight <= 0.0) {
                continue;
            }
            cumulativeWeight += candidate.weight;
            if (roll < cumulativeWeight) {
                return candidate;
            }
        }
        return fallback;
    }

    private static List<NPC> topNCandidates(List<NPC> candidates, int maxCount) {
        List<NPC> out = new ArrayList<>();
        if (candidates == null || candidates.isEmpty() || maxCount <= 0) {
            return out;
        }
        for (NPC candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (containsEquivalentNpc(out, candidate)) {
                continue;
            }
            out.add(candidate);
            if (out.size() >= maxCount) {
                break;
            }
        }
        return out;
    }

    private static boolean containsEquivalentNpc(List<NPC> candidates, NPC target) {
        if (candidates == null || candidates.isEmpty() || target == null) {
            return false;
        }
        for (NPC candidate : candidates) {
            if (areEquivalentTargets(candidate, target)) {
                return true;
            }
        }
        return false;
    }

    private Point resolveRetrySafeNpcClickPoint(NPC npc) {
        return resolveRetrySafeNpcClickPoint(npc, false);
    }

    private Point resolveRetrySafeNpcClickPoint(NPC npc, boolean initialDispatchSelection) {
        Point fallback = null;
        List<Point> nonRecentCandidates = new ArrayList<>();
        List<Point> usableCandidates = new ArrayList<>();
        int attempts = Math.max(1, NPC_CLICK_POINT_RESAMPLE_ATTEMPTS);
        if (initialDispatchSelection) {
            attempts = randomIntInclusive(INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MIN, INITIAL_DISPATCH_CLICK_POINT_ATTEMPTS_MAX);
        }
        for (int i = 0; i < attempts; i++) {
            Point candidate = host.resolveNpcClickPoint(npc);
            if (candidate == null || !host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            fallback = candidate;
            if (!containsEquivalentPoint(usableCandidates, candidate, 0.9)) {
                usableCandidates.add(candidate);
            }
            if (!isRecentlyUsedClickPoint(candidate) && !containsEquivalentPoint(nonRecentCandidates, candidate, 0.9)) {
                nonRecentCandidates.add(candidate);
            }
        }
        if (!nonRecentCandidates.isEmpty()) {
            return copyPoint(selectDiverseClickPoint(nonRecentCandidates));
        }
        if (!usableCandidates.isEmpty()) {
            return copyPoint(selectDiverseClickPoint(usableCandidates));
        }
        return fallback;
    }

    private Point selectDiverseClickPoint(List<Point> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        double totalWeight = 0.0;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Point candidate = candidates.get(i);
            if (candidate == null) {
                continue;
            }
            double nearestRecentDistance = nearestRecentClickPointDistance(candidate);
            double weight = 1.0
                + Math.min(12.0, nearestRecentDistance * 0.85)
                + randomBetweenInclusive(0.10, 1.70);
            if (isRecentlyUsedClickRegion(candidate)) {
                weight *= 0.55;
            }
            weights[i] = Math.max(0.001, weight);
            totalWeight += weights[i];
        }
        if (!(totalWeight > 0.0)) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights[i];
            if (roll <= cumulative && candidates.get(i) != null) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private double nearestRecentClickPointDistance(Point point) {
        if (point == null) {
            return 0.0;
        }
        double nearest = Double.MAX_VALUE;
        for (long recentKey : recentClickPointHistory) {
            Point recentPoint = decodeClickPointKey(recentKey);
            if (recentPoint == null) {
                continue;
            }
            nearest = Math.min(nearest, pixelDistance(point, recentPoint));
        }
        return nearest == Double.MAX_VALUE ? 8.0 : nearest;
    }

    private static Point copyPoint(Point point) {
        return point == null ? null : new Point(point);
    }

    private static boolean containsEquivalentPoint(List<Point> points, Point candidate, double tolerancePx) {
        if (points == null || points.isEmpty() || candidate == null) {
            return false;
        }
        double tolerance = Math.max(0.0, tolerancePx);
        for (Point point : points) {
            if (point == null) {
                continue;
            }
            if (pixelDistance(point, candidate) <= tolerance) {
                return true;
            }
        }
        return false;
    }

    private static boolean areEquivalentTargets(NPC a, NPC b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getIndex() >= 0 && b.getIndex() >= 0 && a.getIndex() == b.getIndex()) {
            return true;
        }
        return FishingTimingModel.worldPointsNear(
            a.getWorldLocation(),
            b.getWorldLocation(),
            FishingTimingModel.WORLDPOINT_RECLICK_RADIUS_TILES
        );
    }

    private RuntimeDecision maybeAcceptPreDispatchDelay(
        FishingExecutionContext context,
        FishingResolvedTarget target
    ) {
        if (context == null || target == null) {
            return null;
        }
        boolean recentDropCompletion = isRecentDropCompletion(context);
        boolean likelyMissRecoveryCandidate = isLikelyMissRecoveryCandidate(context);
        boolean likelyMissFastRecovery = likelyMissRecoveryCandidate
            && shouldUseLikelyMissFastRecovery(context.fatigue);
        if (context.lastDispatchAtMs > 0L && context.now > 0L) {
            if (context.lastDispatchAtMs != postDispatchHoldForDispatchAtMs) {
                postDispatchHoldForDispatchAtMs = context.lastDispatchAtMs;
                postDispatchHoldUntilMs = context.lastDispatchAtMs + randomBetweenInclusive(
                    POST_DISPATCH_HOLD_MIN_MS,
                    POST_DISPATCH_HOLD_MAX_MS
                );
            }
            boolean bypassPostDispatchHoldForRecentDrop = recentDropCompletion
                && context.sinceLastDispatchMs >= POST_DROP_HOLD_BYPASS_MIN_SINCE_LAST_DISPATCH_MS;
            if (!context.levelUpFastRefishActive
                && !bypassPostDispatchHoldForRecentDrop
                && context.now < postDispatchHoldUntilMs) {
                long waitMs = postDispatchHoldUntilMs - context.now;
                return acceptHold(
                    context,
                    "fishing_post_dispatch_hold_window",
                    waitMs,
                    targetTelemetryDetails(
                        target,
                        context,
                        "npcId", target.targetId,
                        "npcIndex", target.targetIndex,
                        "waitMsRemaining", waitMs,
                        "bypassPostDispatchHoldForRecentDrop", false,
                        "recentDropCompletion", recentDropCompletion,
                        "likelyMissRecoveryCandidate", likelyMissRecoveryCandidate,
                        "likelyMissFastRecovery", likelyMissFastRecovery
                    )
                );
            }
        }
        RuntimeDecision depletedReacquireDelayDecision = maybeAcceptDepletedSpotReacquireDelay(
            context,
            target,
            recentDropCompletion
        );
        if (depletedReacquireDelayDecision != null) {
            return depletedReacquireDelayDecision;
        }
        if (!target.sameDispatchTarget || context.lastDispatchAtMs <= 0L || context.now <= 0L) {
            return null;
        }
        long sinceLastSameTargetDispatchMs = context.now - context.lastDispatchAtMs;
        if (sinceLastSameTargetDispatchMs >= 0L) {
            int fatigueReclickBiasMs = FishingTimingModel.fatigueReclickCooldownBiasMs(context.fatigue);
            long sameTargetCooldownMs = resolveSameTargetReclickCooldownMs(
                context,
                target,
                likelyMissRecoveryCandidate,
                recentDropCompletion,
                fatigueReclickBiasMs
            );
            if (sinceLastSameTargetDispatchMs < sameTargetCooldownMs) {
                long waitMs = sameTargetCooldownMs - sinceLastSameTargetDispatchMs;
                return acceptHold(
                    context,
                    "fishing_same_target_reclick_cooldown",
                    waitMs,
                    targetTelemetryDetails(
                        target,
                        context,
                        "npcId", target.targetId,
                        "npcIndex", target.targetIndex,
                        "sameDispatchTarget", true,
                        "waitMsRemaining", waitMs,
                        "sinceLastSameTargetDispatchMs", sinceLastSameTargetDispatchMs,
                        "sameTargetCooldownMs", sameTargetCooldownMs,
                        "fatigueReclickBiasMs", fatigueReclickBiasMs,
                        "likelyMissRecoveryCandidate", likelyMissRecoveryCandidate,
                        "likelyMissFastRecovery", likelyMissFastRecovery,
                        "recentDropCompletion", recentDropCompletion,
                        "levelUpFastRefishActive", context.levelUpFastRefishActive
                    )
                );
            }
        }
        if (context.lastDispatchAtMs != duplicateDispatchGuardForDispatchAtMs) {
            duplicateDispatchGuardForDispatchAtMs = context.lastDispatchAtMs;
            duplicateDispatchGuardUntilMs = context.lastDispatchAtMs + randomBetweenInclusive(
                DUPLICATE_DISPATCH_GUARD_MIN_MS,
                DUPLICATE_DISPATCH_GUARD_MAX_MS
            );
        }
        if (context.now < duplicateDispatchGuardUntilMs) {
            long waitMs = duplicateDispatchGuardUntilMs - context.now;
            return acceptHold(
                context,
                "fishing_duplicate_dispatch_guard",
                waitMs,
                targetTelemetryDetails(
                    target,
                    context,
                    "npcId", target.targetId,
                    "npcIndex", target.targetIndex,
                    "sameDispatchTarget", true,
                    "waitMsRemaining", waitMs,
                    "recentDropCompletion", recentDropCompletion,
                    "likelyMissRecoveryCandidate", likelyMissRecoveryCandidate,
                    "likelyMissFastRecovery", likelyMissFastRecovery
                )
            );
        }
        RuntimeDecision hesitationDecision = maybeAcceptMicroHesitationDelay(
            context,
            target,
            recentDropCompletion,
            likelyMissFastRecovery
        );
        if (hesitationDecision != null) {
            return hesitationDecision;
        }
        return null;
    }

    private RuntimeDecision maybeAcceptMicroHesitationDelay(
        FishingExecutionContext context,
        FishingResolvedTarget target,
        boolean recentDropCompletion,
        boolean likelyMissFastRecovery
    ) {
        if (context == null || target == null || context.now <= 0L || context.levelUpFastRefishActive) {
            return null;
        }
        if (recentDropCompletion) {
            return null;
        }
        boolean contextChanged = context.lastDispatchAtMs != microHesitationForDispatchAtMs
            || target.targetIndex != microHesitationForNpcIndex
            || target.sameDispatchTarget != microHesitationSameDispatchTarget;
        if (contextChanged) {
            microHesitationForDispatchAtMs = context.lastDispatchAtMs;
            microHesitationForNpcIndex = target.targetIndex;
            microHesitationSameDispatchTarget = target.sameDispatchTarget;
            microHesitationUntilMs = 0L;
            microHesitationSampledForContext = false;
        }
        if (!microHesitationSampledForContext) {
            int hesitationChance = FISHING_MICRO_HESITATION_BASE_CHANCE_PERCENT;
            if (target.sameDispatchTarget) {
                hesitationChance += FISHING_MICRO_HESITATION_SAME_TARGET_BONUS_PERCENT;
            }
            if (recentDropCompletion || likelyMissFastRecovery) {
                hesitationChance = Math.max(0, hesitationChance - 14);
            }
            hesitationChance = clampInt(hesitationChance, 0, 88);
            if (ThreadLocalRandom.current().nextInt(100) < hesitationChance) {
                microHesitationUntilMs = context.now + sampleFishingMicroHesitationDelayMs(target);
            }
            microHesitationSampledForContext = true;
        }
        if (microHesitationUntilMs > context.now) {
            long waitMs = microHesitationUntilMs - context.now;
            return acceptHold(
                context,
                "fishing_micro_hesitation_pause",
                waitMs,
                targetTelemetryDetails(
                    target,
                    context,
                    "npcId", target.targetId,
                    "npcIndex", target.targetIndex,
                    "sameDispatchTarget", target.sameDispatchTarget,
                    "waitMsRemaining", waitMs,
                    "recentDropCompletion", recentDropCompletion,
                    "likelyMissFastRecovery", likelyMissFastRecovery
                )
            );
        }
        return null;
    }

    private static long sampleFishingMicroHesitationDelayMs(FishingResolvedTarget target) {
        boolean longWindow =
            ThreadLocalRandom.current().nextInt(100) < FISHING_MICRO_HESITATION_LONG_WINDOW_CHANCE_PERCENT;
        long sample = longWindow
            ? randomBetweenInclusive(FISHING_MICRO_HESITATION_LONG_MIN_MS, FISHING_MICRO_HESITATION_LONG_MAX_MS)
            : randomBetweenInclusive(FISHING_MICRO_HESITATION_SHORT_MIN_MS, FISHING_MICRO_HESITATION_SHORT_MAX_MS);
        int distance = target == null ? -1 : target.localDistanceToTarget;
        if (distance > 0) {
            sample += Math.min(220L, (long) distance * 22L);
        }
        if (target != null && target.sameDispatchTarget) {
            sample += randomBetweenInclusive(40L, 170L);
        }
        return Math.max(FISHING_MICRO_HESITATION_SHORT_MIN_MS, sample);
    }

    private RuntimeDecision maybeAcceptDepletedSpotReacquireDelay(
        FishingExecutionContext context,
        FishingResolvedTarget target,
        boolean recentDropCompletion
    ) {
        if (context == null
            || target == null
            || target.sameDispatchTarget
            || context.levelUpFastRefishActive
            || recentDropCompletion
            || context.lastDispatchAtMs <= 0L
            || context.now <= 0L
            || context.sinceLastDispatchMs < DEPLETED_SPOT_REACQUIRE_MIN_AGE_MS
            || context.now <= host.fishingOutcomeWaitUntilMs()) {
            return null;
        }
        if (context.lastDispatchAtMs != depletedSpotReacquireDelayForDispatchAtMs) {
            depletedSpotReacquireDelayForDispatchAtMs = context.lastDispatchAtMs;
            depletedSpotReacquireDelayUntilMs =
                context.now + sampleDepletedSpotReacquireDelayMs(target.localDistanceToTarget);
        }
        if (context.now < depletedSpotReacquireDelayUntilMs) {
            long waitMs = depletedSpotReacquireDelayUntilMs - context.now;
            return acceptHold(
                context,
                "fishing_depleted_spot_reacquire_delay",
                waitMs,
                targetTelemetryDetails(
                    target,
                    context,
                    "npcId", target.targetId,
                    "npcIndex", target.targetIndex,
                    "waitMsRemaining", waitMs,
                    "sinceLastDispatchMs", context.sinceLastDispatchMs,
                    "delayMinMs", DEPLETED_SPOT_REACQUIRE_DELAY_MIN_MS,
                    "delayMaxMs", DEPLETED_SPOT_REACQUIRE_DELAY_MAX_MS
                )
            );
        }
        return null;
    }

    private static long sampleDepletedSpotReacquireDelayMs(int localDistanceToTarget) {
        long base = randomBetweenInclusive(
            DEPLETED_SPOT_REACQUIRE_DELAY_MIN_MS,
            DEPLETED_SPOT_REACQUIRE_DELAY_MAX_MS
        );
        int distance = Math.max(0, localDistanceToTarget);
        long distanceBias = Math.min(260L, (long) distance * 36L);
        return Math.max(DEPLETED_SPOT_REACQUIRE_DELAY_MIN_MS, base + distanceBias);
    }

    private boolean isRecentDropCompletion(FishingExecutionContext context) {
        if (context == null) {
            return false;
        }
        long lastDropEndedAtMs = host.lastDropSweepSessionEndedAtMs();
        return lastDropEndedAtMs > 0L
            && context.now > 0L
            && (context.now - lastDropEndedAtMs) <= POST_DROP_FAST_RETRY_GRACE_MS;
    }

    private boolean isLikelyMissRecoveryCandidate(FishingExecutionContext context) {
        if (context == null) {
            return false;
        }
        return !context.levelUpFastRefishActive
            && context.lastDispatchAtMs > 0L
            && context.sinceLastDispatchMs >= LIKELY_MISS_RECOVERY_MIN_AGE_MS
            && context.now > host.fishingOutcomeWaitUntilMs();
    }

    private long resolveSameTargetReclickCooldownMs(
        FishingExecutionContext context,
        FishingResolvedTarget target,
        boolean likelyMissRecoveryCandidate,
        boolean recentDropCompletion,
        int fatigueReclickBiasMs
    ) {
        if (context == null || target == null) {
            return SAME_TARGET_MIN_RECLICK_COOLDOWN_MS;
        }
        long sameTargetCooldownMs = sampleBaseSameTargetCooldownMs(context, target.targetIndex);
        sameTargetCooldownMs = Math.max(
            SAME_TARGET_MIN_RECLICK_COOLDOWN_MS,
            sameTargetCooldownMs + Math.max(0, fatigueReclickBiasMs)
        );
        if (context.levelUpFastRefishActive) {
            sameTargetCooldownMs = Math.max(
                LEVEL_UP_FAST_REFISH_SAME_TARGET_COOLDOWN_MIN_MS,
                Math.min(sameTargetCooldownMs, LEVEL_UP_FAST_REFISH_SAME_TARGET_COOLDOWN_MAX_MS)
            );
        }
        if (likelyMissRecoveryCandidate) {
            sameTargetCooldownMs = Math.max(
                LIKELY_MISS_RECOVERY_MIN_RECLICK_COOLDOWN_MS,
                Math.round((double) sameTargetCooldownMs * LIKELY_MISS_RECOVERY_COOLDOWN_SCALE)
            );
        }
        if (recentDropCompletion) {
            long postDropCooldownFloor = POST_DROP_SAME_TARGET_RECLICK_COOLDOWN_MIN_MS;
            if (context.sinceLastDispatchMs >= POST_DROP_HOLD_BYPASS_MIN_SINCE_LAST_DISPATCH_MS) {
                postDropCooldownFloor = Math.max(
                    SAME_TARGET_MIN_RECLICK_COOLDOWN_MS,
                    POST_DROP_SAME_TARGET_RECLICK_COOLDOWN_MIN_MS - 180L
                );
            }
            sameTargetCooldownMs = Math.max(
                postDropCooldownFloor,
                Math.min(
                    sameTargetCooldownMs,
                    POST_DROP_SAME_TARGET_RECLICK_COOLDOWN_MAX_MS
                )
            );
        }
        return sameTargetCooldownMs;
    }

    private long sampleBaseSameTargetCooldownMs(FishingExecutionContext context, int targetNpcIndex) {
        if (context == null || context.lastDispatchAtMs <= 0L) {
            return SAME_TARGET_MIN_RECLICK_COOLDOWN_MS;
        }
        boolean levelUpFastRefish = context.levelUpFastRefishActive;
        if (context.lastDispatchAtMs == sameTargetCooldownForDispatchAtMs
            && targetNpcIndex == sameTargetCooldownForNpcIndex
            && levelUpFastRefish == sameTargetCooldownLevelUpFastRefish
            && sameTargetCooldownSampleMs > 0L) {
            return sameTargetCooldownSampleMs;
        }
        long baseCooldownMs = levelUpFastRefish
            ? FishingTimingModel.variedGlobalReclickCooldownMs(
                host.fishingSameTargetReclickCooldownMs(),
                context.lastDispatchAtMs,
                targetNpcIndex
            )
            : FishingTimingModel.variedSameTargetReclickCooldownMs(
                host.fishingSameTargetReclickCooldownMs(),
                context.lastDispatchAtMs,
                targetNpcIndex
            );
        long sampled = sampleCooldownWithJitter(baseCooldownMs, levelUpFastRefish);
        sameTargetCooldownForDispatchAtMs = context.lastDispatchAtMs;
        sameTargetCooldownForNpcIndex = targetNpcIndex;
        sameTargetCooldownLevelUpFastRefish = levelUpFastRefish;
        sameTargetCooldownSampleMs = sampled;
        return sampled;
    }

    private static long sampleCooldownWithJitter(long baseCooldownMs, boolean levelUpFastRefish) {
        double scale = randomBetweenInclusive(
            SAME_TARGET_COOLDOWN_JITTER_MIN_SCALE,
            SAME_TARGET_COOLDOWN_JITTER_MAX_SCALE
        );
        long additiveJitterMs = randomBetweenInclusive(
            SAME_TARGET_COOLDOWN_JITTER_MIN_MS,
            SAME_TARGET_COOLDOWN_JITTER_MAX_MS
        );
        long sampled = Math.round((double) Math.max(0L, baseCooldownMs) * scale) + additiveJitterMs;
        sampled = Math.max(SAME_TARGET_MIN_RECLICK_COOLDOWN_MS, sampled);
        if (levelUpFastRefish) {
            sampled = Math.max(
                LEVEL_UP_FAST_REFISH_SAME_TARGET_COOLDOWN_MIN_MS,
                Math.min(sampled, LEVEL_UP_FAST_REFISH_SAME_TARGET_COOLDOWN_MAX_MS)
            );
        }
        return sampled;
    }

    private boolean shouldUseLikelyMissFastRecovery(com.xptool.core.runtime.FatigueSnapshot fatigue) {
        int chancePercent = LIKELY_MISS_FAST_RECOVERY_BASE_CHANCE_PERCENT;
        if (fatigue != null) {
            chancePercent += fatigue.fishingNoticeChanceBonusPercent(
                LIKELY_MISS_FAST_RECOVERY_FATIGUE_BONUS_MAX_PERCENT
            );
        }
        chancePercent = Math.max(0, Math.min(100, chancePercent));
        return ThreadLocalRandom.current().nextInt(100) < chancePercent;
    }

    private RuntimeDecision dispatchToTarget(
        FishingExecutionContext context,
        FishingResolvedTarget target
    ) {
        MotorDispatchResult dispatchResult = host.dispatchFishingMoveAndClick(target.canvasPoint, context.motion);
        MotorDispatchStatus status = dispatchResult.getStatus();
        if (status == MotorDispatchStatus.COMPLETE) {
            host.noteInteractionActivityNow();
            host.noteFishingDispatchAttempt(target.npc, context.now);
            rememberRecentDispatchTarget(target);
            host.incrementClicksDispatched();
            return acceptProgress(
                context,
                "fishing_left_click_dispatched",
                targetTelemetryDetails(
                    target,
                    context,
                    "target", "fishing_spot",
                    "npcId", target.targetId,
                    "npcIndex", target.targetIndex,
                    "sameDispatchTarget", target.sameDispatchTarget,
                    "selectionMode", target.selectionMode,
                    "levelUpFastRefishActive", context.levelUpFastRefishActive,
                    "motorGestureId", dispatchResult.getId()
                )
            );
        }
        if (status == MotorDispatchStatus.FAILED || status == MotorDispatchStatus.CANCELLED) {
            host.clearFishingInteractionWindows();
            host.clearFishingTargetLock();
            return host.reject("fishing_motor_gesture_" + safeString(dispatchResult.getReason()));
        }
        boolean scheduledNewDispatch = status == MotorDispatchStatus.SCHEDULED;
        if (scheduledNewDispatch) {
            host.noteFishingDispatchAttempt(target.npc, context.now);
            rememberRecentDispatchTarget(target);
        }
        return acceptProgress(
            context,
            "fishing_motor_gesture_in_flight",
            targetTelemetryDetails(
                target,
                context,
                "target", "fishing_spot",
                "npcId", target.targetId,
                "npcIndex", target.targetIndex,
                "sameDispatchTarget", target.sameDispatchTarget,
                "selectionMode", target.selectionMode,
                "levelUpFastRefishActive", context.levelUpFastRefishActive,
                "dispatchRecorded", scheduledNewDispatch,
                "motorGestureId", dispatchResult.getId(),
                "motorStatus", status.name(),
                "motorReason", dispatchResult.getReason()
            )
        );
    }

    private JsonObject targetTelemetryDetails(
        FishingResolvedTarget target,
        FishingExecutionContext context,
        Object... extraKvPairs
    ) {
        List<Object> pairs = new ArrayList<>();
        if (target != null) {
            pairs.add("selection_pool_size");
            pairs.add(target.selectionPoolSize);
            pairs.add("selection_policy");
            pairs.add(target.selectionPolicy);
        }
        if (context != null) {
            pairs.add("levelUpPromptEpisodeActive");
            pairs.add(levelUpPromptEpisodeActive);
            pairs.add("levelUpRecoveryMode");
            pairs.add(normalizeLevelUpRecoveryMode(levelUpRecoveryMode));
            pairs.add("levelUpFastRefishWindowRemainingMs");
            pairs.add(levelUpFastRefishWindowRemainingMs(context.now));
            boolean levelUpFastRefishHoldBypassed = isLevelUpFastRefishHoldBypassed(context);
            pairs.add("levelUpFastRefishHoldBypassed");
            pairs.add(levelUpFastRefishHoldBypassed);
            pairs.add("levelUpFastRefishHoldBypassRemainingMs");
            pairs.add(levelUpFastRefishHoldBypassed ? postDispatchHoldRemainingMs(context) : 0L);
        }
        if (extraKvPairs != null) {
            for (Object value : extraKvPairs) {
                pairs.add(value);
            }
        }
        return host.details(pairs.toArray());
    }

    private long levelUpFastRefishWindowRemainingMs(long now) {
        if (now <= 0L) {
            return 0L;
        }
        return Math.max(0L, levelUpFastRefishUntilMs - now);
    }

    private long postDispatchHoldRemainingMs(FishingExecutionContext context) {
        if (context == null || context.lastDispatchAtMs <= 0L || context.now <= 0L) {
            return 0L;
        }
        if (context.lastDispatchAtMs != postDispatchHoldForDispatchAtMs) {
            return 0L;
        }
        return Math.max(0L, postDispatchHoldUntilMs - context.now);
    }

    private boolean isLevelUpFastRefishHoldBypassed(FishingExecutionContext context) {
        return context != null
            && context.levelUpFastRefishActive
            && postDispatchHoldRemainingMs(context) > 0L;
    }

    private String normalizeLevelUpRecoveryMode(LevelUpRecoveryMode mode) {
        if (mode == null) {
            return "unknown";
        }
        return mode.name().trim().toLowerCase();
    }

    private void initializeRecentHistories() {
        for (int i = 0; i < recentTargetHistory.length; i++) {
            recentTargetHistory[i] = Long.MIN_VALUE;
        }
        for (int i = 0; i < recentClickPointHistory.length; i++) {
            recentClickPointHistory[i] = Long.MIN_VALUE;
        }
        for (int i = 0; i < recentClickRegionHistory.length; i++) {
            recentClickRegionHistory[i] = Long.MIN_VALUE;
        }
    }

    private void rememberRecentDispatchTarget(FishingResolvedTarget target) {
        if (target == null) {
            return;
        }
        rememberTargetHistoryKey(targetHistoryKey(target.targetIndex, target.targetWorldPoint));
        if (target.canvasPoint != null) {
            rememberClickPointHistoryKey(clickPointKey(target.canvasPoint));
            rememberClickRegionHistoryKey(clickRegionKey(target.canvasPoint));
        }
    }

    private boolean isRecentlyUsedTarget(NPC npc, WorldPoint worldPoint) {
        if (npc == null) {
            return false;
        }
        return isRecentlyUsedTarget(npc.getIndex(), worldPoint);
    }

    private boolean isRecentlyUsedTarget(int targetIndex, WorldPoint worldPoint) {
        long key = targetHistoryKey(targetIndex, worldPoint);
        if (key == Long.MIN_VALUE) {
            return false;
        }
        for (long recentKey : recentTargetHistory) {
            if (recentKey == key) {
                return true;
            }
        }
        return false;
    }

    private void rememberTargetHistoryKey(long key) {
        if (key == Long.MIN_VALUE || recentTargetHistory.length <= 0) {
            return;
        }
        recentTargetHistory[recentTargetHistoryWriteIndex] = key;
        recentTargetHistoryWriteIndex = (recentTargetHistoryWriteIndex + 1) % recentTargetHistory.length;
    }

    private boolean isRecentlyUsedClickPoint(Point point) {
        long key = clickPointKey(point);
        if (key == Long.MIN_VALUE) {
            return false;
        }
        if (isRecentlyUsedClickRegion(point)) {
            return true;
        }
        for (long recentKey : recentClickPointHistory) {
            if (recentKey == key) {
                return true;
            }
            Point recentPoint = decodeClickPointKey(recentKey);
            if (recentPoint != null && pixelDistance(recentPoint, point) <= RECENT_CLICK_POINT_REPEAT_EXCLUSION_PX) {
                return true;
            }
        }
        return false;
    }

    private boolean isRecentlyUsedClickRegion(Point point) {
        long regionKey = clickRegionKey(point);
        if (regionKey == Long.MIN_VALUE) {
            return false;
        }
        for (long recentRegionKey : recentClickRegionHistory) {
            if (recentRegionKey == regionKey) {
                return true;
            }
        }
        return false;
    }

    private void rememberClickPointHistoryKey(long key) {
        if (key == Long.MIN_VALUE || recentClickPointHistory.length <= 0) {
            return;
        }
        recentClickPointHistory[recentClickPointHistoryWriteIndex] = key;
        recentClickPointHistoryWriteIndex = (recentClickPointHistoryWriteIndex + 1) % recentClickPointHistory.length;
    }

    private void rememberClickRegionHistoryKey(long key) {
        if (key == Long.MIN_VALUE || recentClickRegionHistory.length <= 0) {
            return;
        }
        recentClickRegionHistory[recentClickRegionHistoryWriteIndex] = key;
        recentClickRegionHistoryWriteIndex = (recentClickRegionHistoryWriteIndex + 1)
            % recentClickRegionHistory.length;
    }

    private static long targetHistoryKey(int targetIndex, WorldPoint worldPoint) {
        long indexBits = targetIndex < 0 ? 0xFFFFFL : (targetIndex & 0xFFFFFL);
        long worldBits = worldPointKey(worldPoint);
        if (worldBits == Long.MIN_VALUE) {
            return (indexBits << 43) | 0x7FFFFFFFFFFL;
        }
        return (indexBits << 43) | (worldBits & 0x7FFFFFFFFFFL);
    }

    private static long worldPointKey(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return Long.MIN_VALUE;
        }
        long x = worldPoint.getX() & 0x1FFFFFL;
        long y = worldPoint.getY() & 0x1FFFFFL;
        long plane = worldPoint.getPlane() & 0x3L;
        return (plane << 42) | (x << 21) | y;
    }

    private static long clickPointKey(Point point) {
        if (point == null) {
            return Long.MIN_VALUE;
        }
        long x = ((long) point.x) & 0xFFFFFFFFL;
        long y = ((long) point.y) & 0xFFFFFFFFL;
        return (x << 32) | y;
    }

    private static long clickRegionKey(Point point) {
        if (point == null) {
            return Long.MIN_VALUE;
        }
        int regionSize = Math.max(1, RECENT_CLICK_REGION_CELL_SIZE_PX);
        int regionX = Math.floorDiv(point.x, regionSize);
        int regionY = Math.floorDiv(point.y, regionSize);
        long x = ((long) regionX) & 0xFFFFFFFFL;
        long y = ((long) regionY) & 0xFFFFFFFFL;
        return (x << 32) | y;
    }

    private static Point decodeClickPointKey(long key) {
        if (key == Long.MIN_VALUE) {
            return null;
        }
        int x = (int) (key >>> 32);
        int y = (int) key;
        return new Point(x, y);
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = (double) a.x - (double) b.x;
        double dy = (double) a.y - (double) b.y;
        return Math.hypot(dx, dy);
    }

    private static final class SelectionTuning {
        private final int nearestPriorityChancePercent;
        private final double rankExponent;
        private final double rankMultiplier;
        private final double distanceMultiplier;
        private final double weightJitter;
        private final double recentNearestMultiplier;
        private final double recentMultiplier;

        private SelectionTuning(
            int nearestPriorityChancePercent,
            double rankExponent,
            double rankMultiplier,
            double distanceMultiplier,
            double weightJitter,
            double recentNearestMultiplier,
            double recentMultiplier
        ) {
            this.nearestPriorityChancePercent = nearestPriorityChancePercent;
            this.rankExponent = rankExponent;
            this.rankMultiplier = rankMultiplier;
            this.distanceMultiplier = distanceMultiplier;
            this.weightJitter = weightJitter;
            this.recentNearestMultiplier = recentNearestMultiplier;
            this.recentMultiplier = recentMultiplier;
        }

        private static SelectionTuning defaults() {
            return new SelectionTuning(
                TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_PERCENT,
                2.0,
                3.0,
                1.0,
                1.0,
                0.94,
                TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER
            );
        }
    }

    private static final class WeightedTargetCandidate {
        private final NPC npc;
        private final double weight;
        private final boolean recentCandidate;

        private WeightedTargetCandidate(NPC npc, double weight, boolean recentCandidate) {
            this.npc = npc;
            this.weight = weight;
            this.recentCandidate = recentCandidate;
        }
    }

    private static final class TargetSelectionResult {
        private final NPC selectedNpc;
        private final int poolSize;
        private final String policy;

        private TargetSelectionResult(NPC selectedNpc, int poolSize, String policy) {
            this.selectedNpc = selectedNpc;
            this.poolSize = Math.max(0, poolSize);
            this.policy = policy == null ? TARGET_SELECTION_POLICY_FALLBACK : policy;
        }

        private static TargetSelectionResult of(NPC selectedNpc, int poolSize, String policy) {
            return new TargetSelectionResult(selectedNpc, poolSize, policy);
        }
    }

    private RuntimeDecision acceptHold(
        FishingExecutionContext context,
        String reason,
        long waitMs,
        JsonObject details
    ) {
        long nowMs = context == null ? System.currentTimeMillis() : context.now;
        noteIdleOffscreenSuppression(nowMs, waitMs);
        JsonObject enriched = holdDebugTelemetry.decorateHold(details, reason, nowMs, waitMs);
        return host.accept(reason, enriched);
    }

    private RuntimeDecision acceptProgress(
        FishingExecutionContext context,
        String reason,
        JsonObject details
    ) {
        long nowMs = context == null ? System.currentTimeMillis() : context.now;
        JsonObject enriched = holdDebugTelemetry.decorateProgress(details, reason, nowMs);
        return host.accept(reason, enriched);
    }

    private FatigueSnapshot fatigueSnapshot() {
        FatigueSnapshot snapshot = host.fatigueSnapshot();
        return snapshot == null ? FatigueSnapshot.neutral() : snapshot;
    }

    private void noteIdleOffscreenSuppression(long nowMs, long waitMs) {
        if (waitMs <= 0L) {
            return;
        }
        long baseNow = nowMs > 0L ? nowMs : System.currentTimeMillis();
        long until = baseNow + waitMs;
        if (until > idleOffscreenSuppressionUntilMs) {
            idleOffscreenSuppressionUntilMs = until;
        }
    }

    private String safeString(String value) {
        if (host != null) {
            return host.safeString(value);
        }
        return value == null ? "" : value;
    }
}
