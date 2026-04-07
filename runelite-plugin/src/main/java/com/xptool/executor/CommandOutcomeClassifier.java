package com.xptool.executor;

import com.google.gson.JsonObject;

final class CommandOutcomeClassifier {
    enum Kind {
        DISPATCHED,
        DEFERRED,
        FAILED,
        TERMINAL
    }

    static final class Result {
        final Kind kind;
        final String reason;
        final JsonObject details;

        private Result(Kind kind, String reason, JsonObject details) {
            this.kind = kind;
            this.reason = reason;
            this.details = details;
        }
    }

    private CommandOutcomeClassifier() {
    }

    static Result classify(CommandExecutor.CommandDecision decision) {
        if (decision == null) {
            return new Result(Kind.FAILED, "null_command_decision", null);
        }
        String reason = safeString(decision.getReason());
        JsonObject details = decision.getDetails();
        if (!decision.isAccepted()) {
            if (isTerminalReason(reason)) {
                return new Result(Kind.TERMINAL, reason, details);
            }
            return new Result(Kind.FAILED, reason.isEmpty() ? "command_rejected" : reason, details);
        }
        if (isDispatchedReason(reason)) {
            return new Result(Kind.DISPATCHED, reason, details);
        }
        return new Result(Kind.DEFERRED, reason.isEmpty() ? "executor_deferred_no_reason" : reason, details);
    }

    private static boolean isDispatchedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }
        return reason.endsWith("_dispatched")
            || reason.endsWith("_complete")
            || reason.endsWith("_entered")
            || "bank_already_open".equals(reason)
            || "bank_already_closed".equals(reason);
    }

    private static boolean isTerminalReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }
        return "missing_command_id".equals(reason)
            || "unknown_command_id_format".equals(reason)
            || "missing_created_at".equals(reason)
            || "duplicate_command_id".equals(reason)
            || "unsupported_command_type".equals(reason)
            || reason.startsWith("stale_command_id");
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
