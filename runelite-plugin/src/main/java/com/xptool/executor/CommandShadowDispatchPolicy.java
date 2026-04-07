package com.xptool.executor;

import com.google.gson.JsonObject;

final class CommandShadowDispatchPolicy {
    private CommandShadowDispatchPolicy() {
    }

    static ExecutionOutcome unsupportedCommandTypeOutcome(CommandExecutor.CommandDecision decision) {
        return CommandDecisionOutcomePolicy.outcomeFromDecision(decision);
    }

    static ExecutionOutcome shadowWouldDispatchOutcome(int currentTick, String normalizedCommandType, String plannerTag) {
        JsonObject details = new JsonObject();
        details.addProperty("shadowOnly", true);
        details.addProperty("tick", currentTick);
        details.addProperty("commandType", normalizedCommandType == null ? "" : normalizedCommandType);
        if (plannerTag != null && !plannerTag.isEmpty()) {
            details.addProperty("plannerTag", plannerTag);
        }
        return ExecutionOutcome.dispatched("shadow_would_dispatch", details);
    }
}
