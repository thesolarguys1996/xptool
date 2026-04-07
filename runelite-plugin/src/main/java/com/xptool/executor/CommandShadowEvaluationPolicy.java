package com.xptool.executor;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class CommandShadowEvaluationPolicy {
    private CommandShadowEvaluationPolicy() {
    }

    static ExecutionOutcome evaluateShadow(
        CommandRow row,
        int currentTick,
        Predicate<String> isDuplicateCommandId,
        CommandEnvelopeVerifier commandEnvelopeVerifier,
        Function<String, ExecutionOutcome> rejectionOutcomeMapper,
        BridgeCommandDispatchModePolicy dispatchModePolicy,
        Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier,
        Function<CommandRow, String> plannerTagResolver
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

            String normalizedCommandType = dispatchModePolicy.normalizeCommandType(row == null ? "" : row.commandType);
            if (!CommandSupportPolicy.isSupported(normalizedCommandType)) {
                return CommandShadowDispatchPolicy.unsupportedCommandTypeOutcome(unsupportedDecisionSupplier.get());
            }

            String plannerTag = plannerTagResolver.apply(row);
            return CommandShadowDispatchPolicy.shadowWouldDispatchOutcome(currentTick, normalizedCommandType, plannerTag);
        });
    }
}
