package com.xptool.executor;

import com.google.gson.JsonObject;
import net.runelite.api.GameState;

final class CommandIngestLifecycleService {
    interface Host {
        CommandRow peekPendingCommand();
        void pollPendingCommand();
        ExecutionOutcome evaluateCommandRow(CommandRow row, int currentTick);
        ExecutionOutcome evaluateCommandRowShadow(CommandRow row, int currentTick);
        void maybeExtendIdleTraversalOrBankSuppression(CommandRow row, ExecutionOutcome outcome);
        boolean isShadowWouldDispatchOutcome(ExecutionOutcome outcome);
        int maxMechanicalDispatchesPerTick();
        JsonObject details(Object... kvPairs);
        void noteInteractionActivityNow();
        void emit(String status, CommandRow row, String reason, JsonObject details, String eventType);
        GameState gameState();
    }

    private final Host host;

    CommandIngestLifecycleService(Host host) {
        this.host = host;
    }

    int processShadowCommandRows(int tick) {
        int processed = 0;
        while (true) {
            CommandRow row = host.peekPendingCommand();
            if (row == null) {
                break;
            }
            ExecutionOutcome outcome = host.evaluateCommandRowShadow(row, tick);
            emitCommandOutcome(row, outcome);
            host.pollPendingCommand();
            processed++;
        }
        return processed;
    }

    int processGameTickCommandRows(int tick) {
        int processed = 0;
        int mechanicalDispatches = 0;
        while (true) {
            CommandRow row = host.peekPendingCommand();
            if (row == null) {
                break;
            }

            ExecutionOutcome outcome;
            if (mechanicalDispatches >= host.maxMechanicalDispatchesPerTick()) {
                outcome = ExecutionOutcome.deferred(
                    "executor_mechanical_frame_dispatch_limit",
                    host.details(
                        "limit", host.maxMechanicalDispatchesPerTick(),
                        "tick", tick
                    )
                );
            } else {
                outcome = host.evaluateCommandRow(row, tick);
            }

            host.maybeExtendIdleTraversalOrBankSuppression(row, outcome);
            emitCommandOutcome(row, outcome);
            host.pollPendingCommand();
            processed++;

            if (outcome.type == ExecutionOutcomeType.DISPATCHED && !host.isShadowWouldDispatchOutcome(outcome)) {
                mechanicalDispatches++;
            }
        }
        return processed;
    }

    void pumpPendingCommandOnClientTickWhenLoggedOut(int tick) {
        if (host.gameState() == GameState.LOGGED_IN) {
            return;
        }
        int budget = Math.max(1, host.maxMechanicalDispatchesPerTick() * 4);
        int processed = 0;
        while (processed < budget) {
            CommandRow row = host.peekPendingCommand();
            if (row == null) {
                break;
            }
            ExecutionOutcome outcome = host.evaluateCommandRow(row, tick);
            emitCommandOutcome(row, outcome);
            host.pollPendingCommand();
            processed++;
        }
    }

    void emitCommandOutcome(CommandRow row, ExecutionOutcome outcome) {
        if (row == null || outcome == null) {
            return;
        }
        if (outcome.type == ExecutionOutcomeType.DISPATCHED && !host.isShadowWouldDispatchOutcome(outcome)) {
            host.noteInteractionActivityNow();
        }
        String status;
        switch (outcome.type) {
            case DISPATCHED:
            case DEFERRED:
                status = "executed";
                break;
            case TERMINAL:
                status = "rejected";
                break;
            case FAILED:
            default:
                status = "failed";
                break;
        }
        host.emit(status, row, outcome.reason, outcome.details, outcome.type.name());
    }
}
