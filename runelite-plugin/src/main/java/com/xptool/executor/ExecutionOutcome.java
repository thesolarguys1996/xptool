package com.xptool.executor;

import com.google.gson.JsonObject;

final class ExecutionOutcome {
    final ExecutionOutcomeType type;
    final String reason;
    final JsonObject details;

    private ExecutionOutcome(ExecutionOutcomeType type, String reason, JsonObject details) {
        this.type = type;
        this.reason = safeString(reason);
        this.details = details;
    }

    static ExecutionOutcome dispatched(String reason, JsonObject details) {
        return new ExecutionOutcome(ExecutionOutcomeType.DISPATCHED, reason, details);
    }

    static ExecutionOutcome deferred(String reason, JsonObject details) {
        return new ExecutionOutcome(ExecutionOutcomeType.DEFERRED, reason, details);
    }

    static ExecutionOutcome failed(String reason, JsonObject details) {
        return new ExecutionOutcome(ExecutionOutcomeType.FAILED, reason, details);
    }

    static ExecutionOutcome terminal(String reason, JsonObject details) {
        return new ExecutionOutcome(ExecutionOutcomeType.TERMINAL, reason, details);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
