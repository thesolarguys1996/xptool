package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

final class BrutusCombatSystem {
    interface Host {
        boolean isAttackableNpc(NPC npc);

        boolean npcMatchesPreferredTarget(
            NPC npc,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint
        );

        boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance);

        boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance);

        Optional<NPC> resolveNearestCombatTarget(
            Player local,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            int maxChaseDistance,
            boolean requireAttackable
        );

        MotorProgram activeMotorProgram();

        void clearActiveMotorProgram();

        String normalizedMotorOwnerName(String owner);

        String interactionMotorOwner();

        void cancelMotorProgram(MotorProgram program, String reason);

        void clearPendingMouseMove();

        boolean isCombatCanvasPointUsable(Point point);

        MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile);

        MotorProfile buildCombatDodgeMoveAndClickProfile(ClickMotionSettings motion);

        String safeString(String value);

        int currentExecutorTick();

        void incrementCombatTargetUnavailableStreak();

        int combatTargetUnavailableStreak();

        void resetCombatTargetUnavailableStreak();

        void clearCombatOutcomeWaitWindow();

        void clearCombatTargetAttempt();

        void noteInteractionActivityNow();

        void incrementClicksDispatched();

        JsonObject details(Object... kvPairs);

        long combatRecenterCooldownUntilMs();

        boolean hasCombatBoundary();

        int combatBoundaryCenterX();

        int combatBoundaryCenterY();

        int combatBoundaryRadiusTiles();

        long combatRecenterMinCooldownMs();

        long combatRecenterMaxCooldownMs();

        long randomBetween(long minInclusive, long maxInclusive);

        void setCombatRecenterCooldownUntilMs(long atMs);

        CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject details);

        CommandExecutor.CommandDecision rejectDecision(String reason);
    }

    private final Client client;
    private final Host host;
    private final BrutusState brutusState = new BrutusState();
    private final BrutusEncounterResolver brutusEncounterResolver;
    private final BrutusThreatService brutusThreatService;
    private final BrutusPressureTracker brutusPressureTracker;
    private final BrutusDodgeCandidateBuilder brutusDodgeCandidateBuilder;
    private final BrutusDodgeTileSelector brutusDodgeTileSelector;
    private final BrutusDodgeTileSuppression brutusDodgeTileSuppression;
    private final BrutusDodgeProgressTracker brutusDodgeProgressTracker;
    private final BrutusNavigation brutusNavigation;
    private final BrutusMotorPreemptor brutusMotorPreemptor;
    private final BrutusDodgeService brutusDodgeService;

    BrutusCombatSystem(Client client, Host host) {
        this.client = client;
        this.host = host;
        this.brutusThreatService = new BrutusThreatService(client, host::isAttackableNpc, BrutusConstants.ENCOUNTER_PROFILE);
        this.brutusEncounterResolver = new BrutusEncounterResolver(createBrutusEncounterResolverHost());
        this.brutusPressureTracker = new BrutusPressureTracker();
        this.brutusDodgeCandidateBuilder = new BrutusDodgeCandidateBuilder();
        this.brutusDodgeTileSuppression = new BrutusDodgeTileSuppression();
        this.brutusDodgeProgressTracker = new BrutusDodgeProgressTracker(createBrutusDodgeProgressTrackerHost());
        this.brutusNavigation = new BrutusNavigation(client, host::isCombatCanvasPointUsable);
        this.brutusDodgeTileSelector = new BrutusDodgeTileSelector(createBrutusDodgeTileSelectorHost());
        this.brutusMotorPreemptor = new BrutusMotorPreemptor(createBrutusMotorPreemptorHost());
        this.brutusDodgeService = new BrutusDodgeService(createBrutusDodgeHost());
    }

    String encounterProfile() {
        return BrutusConstants.ENCOUNTER_PROFILE;
    }

    int eatPriorityWindowTicks() {
        return BrutusConstants.EAT_PRIORITY_WINDOW_TICKS;
    }

    long postDodgeHoldMs() {
        return BrutusConstants.POST_DODGE_HOLD_MS;
    }

    int nearbyScanRangeTiles() {
        return BrutusConstants.NEARBY_SCAN_RANGE_TILES;
    }

    int noSafeTileRecoveryWindowTicks() {
        return BrutusConstants.NO_SAFE_TILE_RECOVERY_WINDOW_TICKS;
    }

    int lastTelegraphTick() {
        return brutusState.lastTelegraphTick();
    }

    long lastDodgeAtMs() {
        return brutusState.lastDodgeAtMs();
    }

    int lastNoSafeTileTick() {
        return brutusPressureTracker.lastNoSafeTileTick();
    }

    int noSafeTileStreak() {
        return brutusPressureTracker.noSafeTileStreak();
    }

    boolean isNoSafeTilePressureActive() {
        return brutusPressureTracker.isNoSafeTilePressureActive(
            host.currentExecutorTick(),
            BrutusConstants.NO_SAFE_TILE_RECOVERY_WINDOW_TICKS
        );
    }

    boolean isDodgeProgressActive(long now) {
        return brutusDodgeProgressTracker.isDodgeProgressActive(
            brutusState.lastDodgeAtMs(),
            now,
            BrutusConstants.DODGE_STUCK_TIMEOUT_MS
        );
    }

    boolean isBrutusNpcNearby(Player local, int scanRangeTiles) {
        return brutusThreatService.isBrutusNpcNearby(local, scanRangeTiles);
    }

    boolean isBrutusNpc(NPC npc) {
        return brutusThreatService.isBrutusNpc(npc);
    }

    String detectNearbyTelegraphName(Player local) {
        return brutusThreatService
            .detectNearbyTelegraph(local, BrutusConstants.DODGE_THREAT_RANGE_TILES)
            .name()
            .toLowerCase(Locale.ROOT);
    }

    Optional<CommandExecutor.CommandDecision> maybeHandleDodgeFromEat(Player local, ClickMotionSettings motion, long now) {
        if (local == null) {
            return Optional.empty();
        }
        return brutusDodgeService.maybeHandleBrutusDodge(
            local,
            motion,
            -1,
            Set.of(),
            "",
            -1,
            -1,
            Math.max(18, BrutusConstants.DODGE_THREAT_RANGE_TILES),
            Math.max(10, BrutusConstants.DODGE_THREAT_RANGE_TILES),
            now
        );
    }

    Optional<CommandExecutor.CommandDecision> maybeHandleDodge(
        Player local,
        ClickMotionSettings motion,
        int preferredNpcId,
        Set<Integer> preferredNpcIds,
        String preferredNpcNameHint,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance,
        int maxChaseDistance,
        long now
    ) {
        return brutusDodgeService.maybeHandleBrutusDodge(
            local,
            motion,
            preferredNpcId,
            preferredNpcIds,
            preferredNpcNameHint,
            targetWorldX,
            targetWorldY,
            targetMaxDistance,
            maxChaseDistance,
            now
        );
    }

    void pruneDodgeTileSuppression(long now) {
        brutusDodgeTileSuppression.prune(now);
    }

    void updateDodgeProgressState(Player local, long now) {
        brutusDodgeProgressTracker.updateDodgeProgressState(
            local,
            now,
            brutusState.lastDodgeAtMs(),
            BrutusConstants.DODGE_PROGRESS_CHECK_MS,
            BrutusConstants.DODGE_STUCK_TIMEOUT_MS,
            BrutusConstants.DODGE_TILE_SUPPRESS_MS
        );
    }

    private BrutusEncounterResolver.Host createBrutusEncounterResolverHost() {
        return new BrutusEncounterResolver.Host() {
            @Override
            public boolean isBrutusNpc(NPC npc) {
                return brutusThreatService.isBrutusNpc(npc);
            }

            @Override
            public boolean npcMatchesPreferredTarget(
                NPC npc,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint
            ) {
                return host.npcMatchesPreferredTarget(npc, preferredNpcId, preferredNpcIds, preferredNpcNameHint);
            }

            @Override
            public boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return host.isNpcWithinCombatArea(npc, targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance) {
                return host.isNpcWithinCombatChaseDistance(local, npc, maxChaseDistance);
            }

            @Override
            public Optional<NPC> resolveNearestCombatTarget(
                Player local,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance,
                int maxChaseDistance,
                boolean requireAttackable
            ) {
                return host.resolveNearestCombatTarget(
                    local,
                    preferredNpcId,
                    preferredNpcIds,
                    preferredNpcNameHint,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance,
                    maxChaseDistance,
                    requireAttackable
                );
            }
        };
    }

    private BrutusMotorPreemptor.Host createBrutusMotorPreemptorHost() {
        return new BrutusMotorPreemptor.Host() {
            @Override
            public MotorProgram activeMotorProgram() {
                return host.activeMotorProgram();
            }

            @Override
            public void clearActiveMotorProgram() {
                host.clearActiveMotorProgram();
            }

            @Override
            public String normalizedMotorOwnerName(String owner) {
                return host.normalizedMotorOwnerName(owner);
            }

            @Override
            public String interactionMotorOwner() {
                return host.interactionMotorOwner();
            }

            @Override
            public void cancelMotorProgram(MotorProgram program, String reason) {
                host.cancelMotorProgram(program, reason);
            }

            @Override
            public void clearPendingMouseMove() {
                host.clearPendingMouseMove();
            }
        };
    }

    private BrutusDodgeService.Host createBrutusDodgeHost() {
        return new BrutusDodgeService.Host() {
            @Override
            public Optional<NPC> resolveBrutusEncounterNpc(
                Player local,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance,
                int maxChaseDistance
            ) {
                return brutusEncounterResolver.resolveBrutusEncounterNpc(
                    local,
                    preferredNpcId,
                    preferredNpcIds,
                    preferredNpcNameHint,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance,
                    maxChaseDistance
                );
            }

            @Override
            public BrutusTelegraph detectBrutusTelegraph(NPC npc) {
                return brutusThreatService.detectTelegraph(npc);
            }

            @Override
            public String brutusLastTelegraphType() {
                return brutusState.lastTelegraphType();
            }

            @Override
            public void setBrutusLastTelegraphType(String value) {
                brutusState.setLastTelegraphType(host.safeString(value));
            }

            @Override
            public long brutusLastDodgeAtMs() {
                return brutusState.lastDodgeAtMs();
            }

            @Override
            public void setBrutusLastDodgeAtMs(long value) {
                brutusState.setLastDodgeAtMs(value);
            }

            @Override
            public int currentExecutorTick() {
                return host.currentExecutorTick();
            }

            @Override
            public int brutusLastTelegraphTick() {
                return brutusState.lastTelegraphTick();
            }

            @Override
            public void setBrutusLastTelegraphTick(int value) {
                brutusState.setLastTelegraphTick(value);
            }

            @Override
            public long combatBrutusDodgeDebounceMs() {
                return BrutusConstants.DODGE_DEBOUNCE_MS;
            }

            @Override
            public long combatBrutusPostDodgeHoldMs() {
                return BrutusConstants.POST_DODGE_HOLD_MS;
            }

            @Override
            public int combatBrutusRepeatTelegraphGuardTicks() {
                return BrutusConstants.REPEAT_TELEGRAPH_GUARD_TICKS;
            }

            @Override
            public void noteBrutusNoSafeTile() {
                brutusPressureTracker.noteNoSafeTile(host.currentExecutorTick());
            }

            @Override
            public int brutusNoSafeTileStreak() {
                return brutusPressureTracker.noSafeTileStreak();
            }

            @Override
            public void incrementCombatTargetUnavailableStreak() {
                host.incrementCombatTargetUnavailableStreak();
            }

            @Override
            public int combatTargetUnavailableStreak() {
                return host.combatTargetUnavailableStreak();
            }

            @Override
            public List<WorldPoint> computeBrutusDodgeCandidates(
                WorldPoint localPos,
                WorldPoint brutusPos,
                BrutusTelegraph telegraph
            ) {
                return brutusDodgeCandidateBuilder.computeCandidates(localPos, brutusPos, telegraph);
            }

            @Override
            public Optional<WorldPoint> selectBestBrutusDodgeTile(
                List<WorldPoint> candidates,
                WorldPoint localPos,
                WorldPoint brutusPos,
                BrutusTelegraph telegraph,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance,
                long now
            ) {
                return brutusDodgeTileSelector.selectBestTile(
                    candidates,
                    localPos,
                    brutusPos,
                    telegraph,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance,
                    now
                );
            }

            @Override
            public Point resolveWorldTileClickPointForCombat(WorldPoint worldPoint) {
                return brutusNavigation.resolveWorldTileClickPoint(worldPoint);
            }

            @Override
            public boolean isCombatCanvasPointUsable(Point point) {
                return host.isCombatCanvasPointUsable(point);
            }

            @Override
            public void suppressBrutusDodgeTile(WorldPoint worldPoint, long durationMs) {
                brutusDodgeTileSuppression.suppress(worldPoint, durationMs);
            }

            @Override
            public long combatBrutusDodgeTileSuppressMs() {
                return BrutusConstants.DODGE_TILE_SUPPRESS_MS;
            }

            @Override
            public MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile) {
                return host.scheduleMotorGesture(point, type, profile);
            }

            @Override
            public MotorProfile buildCombatDodgeMoveAndClickProfile(ClickMotionSettings motion) {
                return host.buildCombatDodgeMoveAndClickProfile(motion);
            }

            @Override
            public String safeString(String value) {
                return host.safeString(value);
            }

            @Override
            public boolean preemptActiveInteractionMotorForBrutusDodge() {
                return brutusMotorPreemptor.preemptActiveInteractionMotorForDodge();
            }

            @Override
            public void resetCombatTargetUnavailableStreak() {
                host.resetCombatTargetUnavailableStreak();
            }

            @Override
            public void clearBrutusNoSafeTileState() {
                brutusPressureTracker.clearNoSafeTileState();
            }

            @Override
            public void clearCombatOutcomeWaitWindow() {
                host.clearCombatOutcomeWaitWindow();
            }

            @Override
            public void clearCombatTargetAttempt() {
                host.clearCombatTargetAttempt();
            }

            @Override
            public void setBrutusLastDodgeStartWorldPoint(WorldPoint worldPoint) {
                brutusDodgeProgressTracker.setLastDodgeStartWorldPoint(worldPoint);
            }

            @Override
            public void setBrutusLastDodgeTargetWorldPoint(WorldPoint worldPoint) {
                brutusDodgeProgressTracker.setLastDodgeTargetWorldPoint(worldPoint);
            }

            @Override
            public void noteInteractionActivityNow() {
                host.noteInteractionActivityNow();
            }

            @Override
            public void incrementClicksDispatched() {
                host.incrementClicksDispatched();
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return host.details(kvPairs);
            }

            @Override
            public long combatRecenterCooldownUntilMs() {
                return host.combatRecenterCooldownUntilMs();
            }

            @Override
            public WorldView topLevelWorldView() {
                return client.getTopLevelWorldView();
            }

            @Override
            public boolean hasCombatBoundary() {
                return host.hasCombatBoundary();
            }

            @Override
            public int combatBoundaryCenterX() {
                return host.combatBoundaryCenterX();
            }

            @Override
            public int combatBoundaryCenterY() {
                return host.combatBoundaryCenterY();
            }

            @Override
            public int combatBoundaryRadiusTiles() {
                return host.combatBoundaryRadiusTiles();
            }

            @Override
            public boolean isWorldPointWithinCombatArea(
                WorldPoint worldPoint,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance
            ) {
                return BrutusCombatArea.isWorldPointWithinTargetArea(
                    worldPoint,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance
                );
            }

            @Override
            public boolean isBrutusDodgeTileSuppressed(WorldPoint worldPoint, long now) {
                return brutusDodgeTileSuppression.isSuppressed(worldPoint, now);
            }

            @Override
            public boolean isWorldPointWalkable(WorldView view, WorldPoint worldPoint) {
                return brutusNavigation.isWorldPointWalkable(view, worldPoint);
            }

            @Override
            public boolean isBrutusPathWalkable(WorldView view, WorldPoint from, WorldPoint to) {
                return brutusNavigation.isPathWalkable(view, from, to);
            }

            @Override
            public int countBrutusEscapeExits(WorldView view, WorldPoint center) {
                return brutusNavigation.countEscapeExits(view, center);
            }

            @Override
            public boolean isBrutusFenceRiskTile(WorldView view, WorldPoint candidate, int escapeExits) {
                return brutusNavigation.isFenceRiskTile(view, candidate, escapeExits, BrutusConstants.MIN_ESCAPE_EXITS_STRICT);
            }

            @Override
            public int collisionFlagsForWorldPoint(WorldView view, WorldPoint worldPoint) {
                return brutusNavigation.collisionFlagsForWorldPoint(view, worldPoint);
            }

            @Override
            public int countBlockedCardinalEdges(int flags) {
                return BrutusNavigation.countBlockedCardinalEdges(flags);
            }

            @Override
            public WorldPoint brutusLastDodgeTargetWorldPoint() {
                return brutusDodgeProgressTracker.lastDodgeTargetWorldPoint();
            }

            @Override
            public long combatRecenterMinCooldownMs() {
                return host.combatRecenterMinCooldownMs();
            }

            @Override
            public long combatRecenterMaxCooldownMs() {
                return host.combatRecenterMaxCooldownMs();
            }

            @Override
            public long randomBetween(long minInclusive, long maxInclusive) {
                return host.randomBetween(minInclusive, maxInclusive);
            }

            @Override
            public int combatBrutusMinEscapeExitsStrict() {
                return BrutusConstants.MIN_ESCAPE_EXITS_STRICT;
            }

            @Override
            public void setCombatRecenterCooldownUntilMs(long atMs) {
                host.setCombatRecenterCooldownUntilMs(atMs);
            }

            @Override
            public CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject details) {
                return host.acceptDecision(reason, details);
            }

            @Override
            public CommandExecutor.CommandDecision rejectDecision(String reason) {
                return host.rejectDecision(reason);
            }
        };
    }

    private BrutusDodgeTileSelector.Host createBrutusDodgeTileSelectorHost() {
        return new BrutusDodgeTileSelector.Host() {
            @Override
            public WorldView topLevelWorldView() {
                return client.getTopLevelWorldView();
            }

            @Override
            public boolean isWorldPointWithinCombatArea(
                WorldPoint worldPoint,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance
            ) {
                return BrutusCombatArea.isWorldPointWithinTargetArea(
                    worldPoint,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance
                );
            }

            @Override
            public boolean isBrutusDodgeTileSuppressed(WorldPoint worldPoint, long now) {
                return brutusDodgeTileSuppression.isSuppressed(worldPoint, now);
            }

            @Override
            public boolean isWorldPointWalkable(WorldView view, WorldPoint worldPoint) {
                return brutusNavigation.isWorldPointWalkable(view, worldPoint);
            }

            @Override
            public boolean isBrutusPathWalkable(WorldView view, WorldPoint from, WorldPoint to) {
                return brutusNavigation.isPathWalkable(view, from, to);
            }

            @Override
            public int countBrutusEscapeExits(WorldView view, WorldPoint center) {
                return brutusNavigation.countEscapeExits(view, center);
            }

            @Override
            public int combatBrutusMinEscapeExits() {
                return BrutusConstants.MIN_ESCAPE_EXITS;
            }

            @Override
            public boolean isBrutusFenceRiskTile(WorldView view, WorldPoint candidate, int escapeExits) {
                return brutusNavigation.isFenceRiskTile(view, candidate, escapeExits, BrutusConstants.MIN_ESCAPE_EXITS_STRICT);
            }

            @Override
            public boolean hasCombatBoundary() {
                return host.hasCombatBoundary();
            }

            @Override
            public int combatBoundaryCenterX() {
                return host.combatBoundaryCenterX();
            }

            @Override
            public int combatBoundaryCenterY() {
                return host.combatBoundaryCenterY();
            }

            @Override
            public WorldPoint brutusLastDodgeTargetWorldPoint() {
                return brutusDodgeProgressTracker.lastDodgeTargetWorldPoint();
            }
        };
    }

    private BrutusDodgeProgressTracker.Host createBrutusDodgeProgressTrackerHost() {
        return new BrutusDodgeProgressTracker.Host() {
            @Override
            public WorldView topLevelWorldView() {
                return client.getTopLevelWorldView();
            }

            @Override
            public boolean isBrutusPathWalkable(WorldView view, WorldPoint from, WorldPoint to) {
                return brutusNavigation.isPathWalkable(view, from, to);
            }

            @Override
            public void suppressBrutusDodgeTile(WorldPoint worldPoint, long durationMs) {
                brutusDodgeTileSuppression.suppress(worldPoint, durationMs);
            }
        };
    }
}
