package com.xptool.executor;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

final class CommandRowEvaluationPolicy {
    @FunctionalInterface
    interface OutcomeOperation {
        ExecutionOutcome run() throws Exception;
    }

    private CommandRowEvaluationPolicy() {
    }

    static Optional<ExecutionOutcome> precheckRejectionOutcome(
        CommandRow row,
        Predicate<String> isDuplicateCommandId,
        CommandEnvelopeVerifier commandEnvelopeVerifier,
        Function<String, ExecutionOutcome> rejectionOutcomeMapper
    ) {
        Optional<String> precheckRejection = CommandDispatchPrecheckPolicy.rejectionReason(
            row,
            isDuplicateCommandId,
            commandEnvelopeVerifier
        );
        return precheckRejection.map(rejectionOutcomeMapper);
    }

    static ExecutionOutcome evaluateSafely(OutcomeOperation operation) {
        try {
            return operation.run();
        } catch (Exception ex) {
            return ExecutionOutcome.failed("command_row_processing_exception:" + ex.getMessage(), null);
        }
    }
}
