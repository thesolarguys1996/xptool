package com.xptool.executor;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

final class CommandLiveDispatchPolicy {
    private CommandLiveDispatchPolicy() {
    }

    static ExecutionOutcome evaluateLive(
        CommandRow row,
        Predicate<String> isDuplicateCommandId,
        CommandEnvelopeVerifier commandEnvelopeVerifier,
        Function<String, ExecutionOutcome> rejectionOutcomeMapper,
        Function<CommandRow, CommandExecutor.CommandDecision> commandDispatcher
    ) {
        return CommandRowEvaluationPolicy.evaluateSafely(() -> {
            Optional<ExecutionOutcome> precheckOutcome = CommandRowEvaluationPolicy.precheckRejectionOutcome(
                row,
                isDuplicateCommandId,
                commandEnvelopeVerifier,
                rejectionOutcomeMapper
            );
            if (precheckOutcome.isPresent()) {
                return precheckOutcome.get();
            }

            CommandExecutor.CommandDecision commandDecision = commandDispatcher.apply(row);
            return CommandDecisionOutcomePolicy.outcomeFromDecision(commandDecision);
        });
    }
}
