package com.xptool.executor;

import java.util.UUID;
import java.util.function.Predicate;

final class CommandValidationPolicy {
    private CommandValidationPolicy() {
    }

    static ValidationResult validateCommand(
        String commandId,
        long createdAtUnixMillis,
        String commandType,
        Predicate<String> duplicateCommandIdCheck
    ) {
        if (safeString(commandId).isEmpty()) {
            return ValidationResult.reject("missing_command_id");
        }
        if (!isUuidLike(commandId)) {
            return ValidationResult.reject("unknown_command_id_format");
        }
        if (createdAtUnixMillis <= 0L) {
            return ValidationResult.reject("missing_created_at");
        }
        if (duplicateCommandIdCheck.test(commandId)) {
            return ValidationResult.reject("stale_command_id_duplicate");
        }
        if (!CommandSupportPolicy.isSupported(commandType)) {
            return ValidationResult.reject("unsupported_command_type");
        }
        return ValidationResult.accept("accepted_for_execution");
    }

    static ValidationResult validateRuntimePreconditions() {
        return ValidationResult.accept("preconditions_skipped");
    }

    private static boolean isUuidLike(String value) {
        try {
            UUID.fromString(safeString(value));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    static final class ValidationResult {
        private final boolean accepted;
        private final String reason;

        private ValidationResult(boolean accepted, String reason) {
            this.accepted = accepted;
            this.reason = reason == null ? "" : reason;
        }

        static ValidationResult accept(String reason) {
            return new ValidationResult(true, reason);
        }

        static ValidationResult reject(String reason) {
            return new ValidationResult(false, reason);
        }

        boolean isAccepted() {
            return accepted;
        }

        String reason() {
            return reason;
        }
    }
}
