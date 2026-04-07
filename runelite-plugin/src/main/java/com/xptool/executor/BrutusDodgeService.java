package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

final class BrutusDodgeService {
    interface Host {
        Optional<NPC> resolveBrutusEncounterNpc(
            Player local,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            int maxChaseDistance
        );

        BrutusTelegraph detectBrutusTelegraph(NPC npc);

        String brutusLastTelegraphType();

        void setBrutusLastTelegraphType(String value);

        long brutusLastDodgeAtMs();

        void setBrutusLastDodgeAtMs(long value);

        int currentExecutorTick();

        int brutusLastTelegraphTick();

        void setBrutusLastTelegraphTick(int value);

        long combatBrutusDodgeDebounceMs();

        long combatBrutusPostDodgeHoldMs();

        int combatBrutusRepeatTelegraphGuardTicks();

        void noteBrutusNoSafeTile();

        int brutusNoSafeTileStreak();

        void incrementCombatTargetUnavailableStreak();

        int combatTargetUnavailableStreak();

        List<WorldPoint> computeBrutusDodgeCandidates(
            WorldPoint localPos,
            WorldPoint brutusPos,
            BrutusTelegraph telegraph
        );

        Optional<WorldPoint> selectBestBrutusDodgeTile(
            List<WorldPoint> candidates,
            WorldPoint localPos,
            WorldPoint brutusPos,
            BrutusTelegraph telegraph,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            long now
        );

        Point resolveWorldTileClickPointForCombat(WorldPoint worldPoint);

        boolean isCombatCanvasPointUsable(Point point);

        void suppressBrutusDodgeTile(WorldPoint worldPoint, long durationMs);

        long combatBrutusDodgeTileSuppressMs();

        MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile);

        MotorProfile buildCombatDodgeMoveAndClickProfile(ClickMotionSettings motion);

        String safeString(String value);

        boolean preemptActiveInteractionMotorForBrutusDodge();

        void resetCombatTargetUnavailableStreak();

        void clearBrutusNoSafeTileState();

        void clearCombatOutcomeWaitWindow();

        void clearCombatTargetAttempt();

        void setBrutusLastDodgeStartWorldPoint(WorldPoint worldPoint);

        void setBrutusLastDodgeTargetWorldPoint(WorldPoint worldPoint);

        void noteInteractionActivityNow();

        void incrementClicksDispatched();

        JsonObject details(Object... kvPairs);

        long combatRecenterCooldownUntilMs();

        WorldView topLevelWorldView();

        boolean hasCombatBoundary();

        int combatBoundaryCenterX();

        int combatBoundaryCenterY();

        int combatBoundaryRadiusTiles();

        boolean isWorldPointWithinCombatArea(
            WorldPoint worldPoint,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance
        );

        boolean isBrutusDodgeTileSuppressed(WorldPoint worldPoint, long now);

        boolean isWorldPointWalkable(WorldView view, WorldPoint worldPoint);

        boolean isBrutusPathWalkable(WorldView view, WorldPoint from, WorldPoint to);

        int countBrutusEscapeExits(WorldView view, WorldPoint center);

        boolean isBrutusFenceRiskTile(WorldView view, WorldPoint candidate, int escapeExits);

        int collisionFlagsForWorldPoint(WorldView view, WorldPoint worldPoint);

        int countBlockedCardinalEdges(int flags);

        WorldPoint brutusLastDodgeTargetWorldPoint();

        long combatRecenterMinCooldownMs();

        long combatRecenterMaxCooldownMs();

        long randomBetween(long minInclusive, long maxInclusive);

        int combatBrutusMinEscapeExitsStrict();

        void setCombatRecenterCooldownUntilMs(long atMs);

        CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject details);

        CommandExecutor.CommandDecision rejectDecision(String reason);
    }

    private final Host host;

    BrutusDodgeService(Host host) {
        this.host = host;
    }

    Optional<CommandExecutor.CommandDecision> maybeHandleBrutusDodge(
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
        if (local == null) {
            return Optional.empty();
        }
        Optional<NPC> brutusNpcOpt = host.resolveBrutusEncounterNpc(
            local,
            preferredNpcId,
            preferredNpcIds,
            preferredNpcNameHint,
            targetWorldX,
            targetWorldY,
            targetMaxDistance,
            maxChaseDistance
        );
        if (brutusNpcOpt.isEmpty()) {
            return Optional.empty();
        }
        NPC brutusNpc = brutusNpcOpt.get();
        BrutusTelegraph telegraph = host.detectBrutusTelegraph(brutusNpc);
        if (telegraph == BrutusTelegraph.NONE) {
            return Optional.empty();
        }
        boolean sameTelegraphAsLast = telegraph.name().equals(host.brutusLastTelegraphType());
        long sinceLastDodgeMs = now - host.brutusLastDodgeAtMs();
        if (sameTelegraphAsLast && sinceLastDodgeMs < host.combatBrutusDodgeDebounceMs()) {
            if (sinceLastDodgeMs >= host.combatBrutusPostDodgeHoldMs()) {
                return Optional.empty();
            }
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_dodge_cooldown",
                    host.details(
                        "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                        "waitMsRemaining", Math.max(0L, host.combatBrutusPostDodgeHoldMs() - sinceLastDodgeMs)
                    )
                )
            );
        }
        if (sameTelegraphAsLast) {
            int tickDelta = host.currentExecutorTick() - host.brutusLastTelegraphTick();
            if (tickDelta >= 0 && tickDelta <= host.combatBrutusRepeatTelegraphGuardTicks()) {
                return Optional.empty();
            }
        }
        if (host.brutusLastTelegraphTick() == host.currentExecutorTick()
            && telegraph.name().equals(host.brutusLastTelegraphType())) {
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_telegraph_debounced",
                    host.details("telegraph", telegraph.name().toLowerCase(Locale.ROOT))
                )
            );
        }

        WorldPoint localPos = local.getWorldLocation();
        WorldPoint brutusPos = brutusNpc.getWorldLocation();
        if (localPos == null || brutusPos == null) {
            return Optional.empty();
        }

        List<WorldPoint> candidates = host.computeBrutusDodgeCandidates(localPos, brutusPos, telegraph);
        Optional<WorldPoint> bestTile = host.selectBestBrutusDodgeTile(
            candidates,
            localPos,
            brutusPos,
            telegraph,
            targetWorldX,
            targetWorldY,
            targetMaxDistance,
            now
        );
        if (bestTile.isEmpty()) {
            host.setBrutusLastTelegraphTick(host.currentExecutorTick());
            host.setBrutusLastTelegraphType(telegraph.name());
            host.noteBrutusNoSafeTile();
            host.incrementCombatTargetUnavailableStreak();
            Optional<CommandExecutor.CommandDecision> emergencyReposition = maybeDispatchBrutusEmergencyReposition(
                localPos,
                brutusPos,
                motion,
                telegraph,
                now,
                "no_safe_tile"
            );
            if (emergencyReposition.isPresent()) {
                return emergencyReposition;
            }
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_dodge_no_safe_tile",
                    host.details(
                        "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                        "candidateCount", candidates.size(),
                        "unavailableStreak", host.combatTargetUnavailableStreak(),
                        "noSafeTileStreak", host.brutusNoSafeTileStreak()
                    )
                )
            );
        }

        Point dodgeCanvas = host.resolveWorldTileClickPointForCombat(bestTile.get());
        if (dodgeCanvas == null || !host.isCombatCanvasPointUsable(dodgeCanvas)) {
            host.suppressBrutusDodgeTile(bestTile.get(), host.combatBrutusDodgeTileSuppressMs());
            host.setBrutusLastTelegraphTick(host.currentExecutorTick());
            host.setBrutusLastTelegraphType(telegraph.name());
            host.noteBrutusNoSafeTile();
            host.incrementCombatTargetUnavailableStreak();
            Optional<CommandExecutor.CommandDecision> emergencyReposition = maybeDispatchBrutusEmergencyReposition(
                localPos,
                brutusPos,
                motion,
                telegraph,
                now,
                "tile_unusable"
            );
            if (emergencyReposition.isPresent()) {
                return emergencyReposition;
            }
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_dodge_tile_unusable",
                    host.details(
                        "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                        "dodgeWorldX", bestTile.get().getX(),
                        "dodgeWorldY", bestTile.get().getY(),
                        "unavailableStreak", host.combatTargetUnavailableStreak(),
                        "noSafeTileStreak", host.brutusNoSafeTileStreak()
                    )
                )
            );
        }

        MotorHandle handle = host.scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(dodgeCanvas),
            MotorGestureType.MOVE_AND_CLICK,
            host.buildCombatDodgeMoveAndClickProfile(motion)
        );
        if (handle.status == MotorGestureStatus.IN_FLIGHT
            && "motor_program_busy".equals(host.safeString(handle.reason))
            && host.preemptActiveInteractionMotorForBrutusDodge()) {
            handle = host.scheduleMotorGesture(
                CanvasPoint.fromAwtPoint(dodgeCanvas),
                MotorGestureType.MOVE_AND_CLICK,
                host.buildCombatDodgeMoveAndClickProfile(motion)
            );
        }
        host.setBrutusLastTelegraphTick(host.currentExecutorTick());
        host.setBrutusLastTelegraphType(telegraph.name());
        if (handle.status == MotorGestureStatus.FAILED || handle.status == MotorGestureStatus.CANCELLED) {
            host.suppressBrutusDodgeTile(bestTile.get(), host.combatBrutusDodgeTileSuppressMs());
            host.noteBrutusNoSafeTile();
            host.incrementCombatTargetUnavailableStreak();
            return Optional.of(host.rejectDecision("combat_brutus_dodge_motor_" + host.safeString(handle.reason)));
        }
        if (handle.status == MotorGestureStatus.IN_FLIGHT
            && "motor_program_busy".equals(host.safeString(handle.reason))) {
            host.resetCombatTargetUnavailableStreak();
            host.clearBrutusNoSafeTileState();
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_dodge_deferred_motor_busy",
                    host.details(
                        "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                        "npcId", brutusNpc.getId(),
                        "npcIndex", brutusNpc.getIndex(),
                        "dodgeWorldX", bestTile.get().getX(),
                        "dodgeWorldY", bestTile.get().getY(),
                        "motorGestureId", handle.id,
                        "motorStatus", handle.status.name(),
                        "motorReason", handle.reason
                    )
                )
            );
        }

        host.clearCombatOutcomeWaitWindow();
        host.clearCombatTargetAttempt();
        host.setBrutusLastDodgeAtMs(now);
        host.setBrutusLastDodgeStartWorldPoint(localPos);
        host.setBrutusLastDodgeTargetWorldPoint(bestTile.get());
        host.resetCombatTargetUnavailableStreak();
        host.clearBrutusNoSafeTileState();

        if (handle.status == MotorGestureStatus.COMPLETE) {
            host.noteInteractionActivityNow();
            host.incrementClicksDispatched();
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_dodge_dispatched",
                    host.details(
                        "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                        "npcId", brutusNpc.getId(),
                        "npcIndex", brutusNpc.getIndex(),
                        "dodgeWorldX", bestTile.get().getX(),
                        "dodgeWorldY", bestTile.get().getY(),
                        "motorGestureId", handle.id
                    )
                )
            );
        }
        return Optional.of(
            host.acceptDecision(
                "combat_brutus_dodge_in_flight",
                host.details(
                    "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                    "npcId", brutusNpc.getId(),
                    "npcIndex", brutusNpc.getIndex(),
                    "dodgeWorldX", bestTile.get().getX(),
                    "dodgeWorldY", bestTile.get().getY(),
                    "motorGestureId", handle.id,
                    "motorStatus", handle.status.name(),
                    "motorReason", handle.reason
                )
            )
        );
    }

    private Optional<CommandExecutor.CommandDecision> maybeDispatchBrutusEmergencyReposition(
        WorldPoint localPos,
        WorldPoint brutusPos,
        ClickMotionSettings motion,
        BrutusTelegraph telegraph,
        long now,
        String triggerReason
    ) {
        if (localPos == null || now < host.combatRecenterCooldownUntilMs()) {
            return Optional.empty();
        }
        WorldView view = host.topLevelWorldView();
        if (view == null) {
            return Optional.empty();
        }

        List<WorldPoint> candidates = new ArrayList<>();
        int[][] offsets = new int[][] {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {2, 0}, {-2, 0}, {0, 2}, {0, -2},
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {-1, 2}, {1, -2}, {-1, -2}
        };
        for (int[] offset : offsets) {
            if (offset == null || offset.length < 2) {
                continue;
            }
            addCombatRecenterCandidate(
                candidates,
                new WorldPoint(localPos.getX() + offset[0], localPos.getY() + offset[1], localPos.getPlane())
            );
        }

        WorldPoint best = null;
        int bestScore = Integer.MIN_VALUE;
        for (WorldPoint candidate : candidates) {
            if (candidate == null || candidate.equals(localPos)) {
                continue;
            }
            if (host.hasCombatBoundary()
                && !host.isWorldPointWithinCombatArea(
                    candidate,
                    host.combatBoundaryCenterX(),
                    host.combatBoundaryCenterY(),
                    host.combatBoundaryRadiusTiles() + 2
                )) {
                continue;
            }
            if (host.isBrutusDodgeTileSuppressed(candidate, now)) {
                continue;
            }
            if (!host.isWorldPointWalkable(view, candidate)) {
                continue;
            }
            if (!host.isBrutusPathWalkable(view, localPos, candidate)) {
                continue;
            }
            Point canvas = host.resolveWorldTileClickPointForCombat(candidate);
            if (canvas == null || !host.isCombatCanvasPointUsable(canvas)) {
                continue;
            }
            int escapeExits = host.countBrutusEscapeExits(view, candidate);
            if (escapeExits < host.combatBrutusMinEscapeExitsStrict()) {
                continue;
            }
            if (host.isBrutusFenceRiskTile(view, candidate, escapeExits)) {
                continue;
            }
            int flags = host.collisionFlagsForWorldPoint(view, candidate);
            if (flags == Integer.MAX_VALUE) {
                continue;
            }
            int blockedEdges = host.countBlockedCardinalEdges(flags);
            int localDist = localPos.distanceTo2D(candidate);
            int brutusDist = brutusPos == null ? 0 : Math.max(0, brutusPos.distanceTo2D(candidate));
            int score = (escapeExits * 170) + (brutusDist * 28) - (blockedEdges * 95) - (localDist * 12);
            if (host.hasCombatBoundary()) {
                int boundaryDist = Math.max(
                    Math.abs(candidate.getX() - host.combatBoundaryCenterX()),
                    Math.abs(candidate.getY() - host.combatBoundaryCenterY())
                );
                score -= (boundaryDist * 8);
            }
            WorldPoint lastTarget = host.brutusLastDodgeTargetWorldPoint();
            if (lastTarget != null && lastTarget.equals(candidate)) {
                score -= 220;
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best == null) {
            return Optional.empty();
        }

        Point recenterCanvas = host.resolveWorldTileClickPointForCombat(best);
        if (recenterCanvas == null || !host.isCombatCanvasPointUsable(recenterCanvas)) {
            host.suppressBrutusDodgeTile(best, host.combatBrutusDodgeTileSuppressMs());
            return Optional.empty();
        }

        MotorHandle handle = host.scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(recenterCanvas),
            MotorGestureType.MOVE_AND_CLICK,
            host.buildCombatDodgeMoveAndClickProfile(motion)
        );
        if (handle.status == MotorGestureStatus.IN_FLIGHT
            && "motor_program_busy".equals(host.safeString(handle.reason))
            && host.preemptActiveInteractionMotorForBrutusDodge()) {
            handle = host.scheduleMotorGesture(
                CanvasPoint.fromAwtPoint(recenterCanvas),
                MotorGestureType.MOVE_AND_CLICK,
                host.buildCombatDodgeMoveAndClickProfile(motion)
            );
        }
        if (handle.status == MotorGestureStatus.FAILED || handle.status == MotorGestureStatus.CANCELLED) {
            host.suppressBrutusDodgeTile(best, host.combatBrutusDodgeTileSuppressMs());
            return Optional.empty();
        }
        if (handle.status == MotorGestureStatus.IN_FLIGHT
            && "motor_program_busy".equals(host.safeString(handle.reason))) {
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_dodge_emergency_recenter_deferred_motor_busy",
                    host.details(
                        "trigger", triggerReason,
                        "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                        "recenterWorldX", best.getX(),
                        "recenterWorldY", best.getY(),
                        "motorGestureId", handle.id,
                        "motorStatus", handle.status.name(),
                        "motorReason", handle.reason
                    )
                )
            );
        }

        host.clearCombatOutcomeWaitWindow();
        host.clearCombatTargetAttempt();
        host.resetCombatTargetUnavailableStreak();
        host.clearBrutusNoSafeTileState();
        host.setCombatRecenterCooldownUntilMs(now + host.randomBetween(
            host.combatRecenterMinCooldownMs(),
            host.combatRecenterMaxCooldownMs()
        ));
        host.setBrutusLastDodgeAtMs(now);
        host.setBrutusLastDodgeStartWorldPoint(localPos);
        host.setBrutusLastDodgeTargetWorldPoint(best);

        if (handle.status == MotorGestureStatus.COMPLETE) {
            host.noteInteractionActivityNow();
            host.incrementClicksDispatched();
            return Optional.of(
                host.acceptDecision(
                    "combat_brutus_dodge_emergency_recenter_dispatched",
                    host.details(
                        "trigger", triggerReason,
                        "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                        "recenterWorldX", best.getX(),
                        "recenterWorldY", best.getY(),
                        "motorGestureId", handle.id
                    )
                )
            );
        }
        return Optional.of(
            host.acceptDecision(
                "combat_brutus_dodge_emergency_recenter_in_flight",
                host.details(
                    "trigger", triggerReason,
                    "telegraph", telegraph.name().toLowerCase(Locale.ROOT),
                    "recenterWorldX", best.getX(),
                    "recenterWorldY", best.getY(),
                    "motorGestureId", handle.id,
                    "motorStatus", handle.status.name(),
                    "motorReason", handle.reason
                )
            )
        );
    }

    private static void addCombatRecenterCandidate(List<WorldPoint> out, WorldPoint candidate) {
        if (out == null || candidate == null) {
            return;
        }
        if (!out.contains(candidate)) {
            out.add(candidate);
        }
    }
}

