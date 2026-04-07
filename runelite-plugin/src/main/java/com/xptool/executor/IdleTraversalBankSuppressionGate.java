package com.xptool.executor;

import java.util.Locale;

final class IdleTraversalBankSuppressionGate {
    private static final long FISHING_WALK_BANK_HARD_SUPPRESS_STALE_MS = 14000L;
    private static final String SOURCE_FISHING = "fishing";
    private static final String COMMAND_WALK_TO_WORLDPOINT_SAFE = "WALK_TO_WORLDPOINT_SAFE";
    private static final String COMMAND_FISH_NEAREST_SPOT_SAFE = "FISH_NEAREST_SPOT_SAFE";
    private static final String COMMAND_STOP_ALL_RUNTIME = "STOP_ALL_RUNTIME";

    private long timedSuppressUntilMs = Long.MIN_VALUE;
    private long fishingHardSuppressUntilMs = Long.MIN_VALUE;

    static final class DirectState {
        final boolean bankOpen;
        final boolean bankMotorProgramActive;
        final boolean interactionMotorProgramActive;
        final boolean pendingCommandRows;
        final boolean interactionOnlySessionActive;
        final boolean shouldOwnInteractionSession;

        DirectState(
            boolean bankOpen,
            boolean bankMotorProgramActive,
            boolean interactionMotorProgramActive,
            boolean pendingCommandRows,
            boolean interactionOnlySessionActive,
            boolean shouldOwnInteractionSession
        ) {
            this.bankOpen = bankOpen;
            this.bankMotorProgramActive = bankMotorProgramActive;
            this.interactionMotorProgramActive = interactionMotorProgramActive;
            this.pendingCommandRows = pendingCommandRows;
            this.interactionOnlySessionActive = interactionOnlySessionActive;
            this.shouldOwnInteractionSession = shouldOwnInteractionSession;
        }
    }

    boolean isSuppressedNow() {
        return false;
    }

    boolean isSuppressedNow(DirectState state) {
        if (state == null) {
            return false;
        }
        // State-driven gate: block idle only while traversal/banking is actively happening now.
        if (state.bankOpen || state.bankMotorProgramActive) {
            return true;
        }
        // Interaction motor plus queued commands indicates active route processing.
        return state.interactionMotorProgramActive && state.pendingCommandRows;
    }

    boolean isFishingHardSuppressedNow() {
        return false;
    }

    long timedSuppressUntilMs() {
        return timedSuppressUntilMs;
    }

    long fishingHardSuppressUntilMs() {
        return fishingHardSuppressUntilMs;
    }

    void clear() {
        timedSuppressUntilMs = Long.MIN_VALUE;
        fishingHardSuppressUntilMs = Long.MIN_VALUE;
    }

    void noteCommandOutcome(
        CommandRow row,
        ExecutionOutcome outcome,
        long walkSuppressDurationMs,
        long bankSuppressDurationMs
    ) {
        // Kept for compatibility with existing wiring; direct-state gating no longer uses timed windows.
        if (row == null || outcome == null) {
            return;
        }
        long now = System.currentTimeMillis();
        pruneExpired(now);

        String commandType = safeString(row.commandType).trim().toUpperCase(Locale.ROOT);
        String source = safeString(row.source).trim().toLowerCase(Locale.ROOT);
        boolean fishingSource = SOURCE_FISHING.equals(source);
        boolean walkCommand = COMMAND_WALK_TO_WORLDPOINT_SAFE.equals(commandType);
        boolean bankCommand = isBankCommandType(commandType);
        boolean fishCommand = COMMAND_FISH_NEAREST_SPOT_SAFE.equals(commandType);
        boolean dispatchedOrDeferred =
            outcome.type == ExecutionOutcomeType.DISPATCHED || outcome.type == ExecutionOutcomeType.DEFERRED;

        if (dispatchedOrDeferred && walkCommand && walkSuppressDurationMs > 0L) {
            timedSuppressUntilMs = Math.max(timedSuppressUntilMs, now + Math.max(1L, walkSuppressDurationMs));
        } else if (dispatchedOrDeferred && bankCommand && bankSuppressDurationMs > 0L) {
            timedSuppressUntilMs = Math.max(timedSuppressUntilMs, now + Math.max(1L, bankSuppressDurationMs));
        }

        if (fishingSource && dispatchedOrDeferred && (walkCommand || bankCommand)) {
            fishingHardSuppressUntilMs = Math.max(
                fishingHardSuppressUntilMs,
                now + FISHING_WALK_BANK_HARD_SUPPRESS_STALE_MS
            );
        }

        if (fishingSource && fishCommand) {
            fishingHardSuppressUntilMs = Long.MIN_VALUE;
        }
        if (COMMAND_STOP_ALL_RUNTIME.equals(commandType)) {
            clear();
        }
    }

    static boolean isBankCommandType(String normalizedCommandType) {
        if (normalizedCommandType == null || normalizedCommandType.isBlank()) {
            return false;
        }
        switch (normalizedCommandType) {
            case "OPEN_BANK":
            case "BANK_OPEN_SAFE":
            case "ENTER_BANK_PIN":
            case "SEARCH_BANK_ITEM":
            case "DEPOSIT_ITEM":
            case "DEPOSIT_ALL_EXCEPT":
            case "BANK_DEPOSIT_ALL_EXCEPT_TOOL_SAFE":
            case "WITHDRAW_ITEM":
            case "BANK_WITHDRAW_LOGS_SAFE":
            case "CLOSE_BANK":
                return true;
            default:
                return false;
        }
    }

    private void pruneExpired(long now) {
        if (now >= timedSuppressUntilMs) {
            timedSuppressUntilMs = Long.MIN_VALUE;
        }
        if (now >= fishingHardSuppressUntilMs) {
            fishingHardSuppressUntilMs = Long.MIN_VALUE;
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
