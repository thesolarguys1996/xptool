package com.xptool.executor;

import java.util.Optional;
import java.util.function.Predicate;

final class CommandDispatchPrecheckPolicy {
    private CommandDispatchPrecheckPolicy() {
    }

    static Optional<String> rejectionReason(
        CommandRow row,
        Predicate<String> isDuplicateCommandId,
        CommandEnvelopeVerifier commandEnvelopeVerifier
    ) {
        CommandValidationPolicy.ValidationResult decision = CommandValidationPolicy.validateCommand(
            row.commandId,
            row.createdAtUnixMillis,
            row.commandType,
            isDuplicateCommandId
        );
        if (!decision.isAccepted()) {
            return Optional.of(decision.reason());
        }

        CommandEnvelopeVerifier.ValidationResult envelopeDecision = commandEnvelopeVerifier.validate(row);
        if (!envelopeDecision.isAccepted()) {
            return Optional.of(envelopeDecision.reason());
        }

        CommandValidationPolicy.ValidationResult pre = CommandValidationPolicy.validateRuntimePreconditions();
        if (!pre.isAccepted()) {
            return Optional.of(pre.reason());
        }

        return Optional.empty();
    }
}
