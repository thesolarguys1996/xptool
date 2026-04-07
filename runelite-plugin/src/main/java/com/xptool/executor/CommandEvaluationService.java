package com.xptool.executor;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class CommandEvaluationService {
    private final BridgeCommandDispatchModePolicy dispatchModePolicy;
    private final Predicate<String> isDuplicateCommandId;
    private final CommandEnvelopeVerifier commandEnvelopeVerifier;
    private final Function<String, ExecutionOutcome> rejectionOutcomeMapper;
    private final Function<CommandRow, CommandExecutor.CommandDecision> commandDispatcher;
    private final Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier;
    private final Function<CommandRow, String> plannerTagResolver;

    CommandEvaluationService(
        BridgeCommandDispatchModePolicy dispatchModePolicy,
        Predicate<String> isDuplicateCommandId,
        CommandEnvelopeVerifier commandEnvelopeVerifier,
        Function<String, ExecutionOutcome> rejectionOutcomeMapper,
        Function<CommandRow, CommandExecutor.CommandDecision> commandDispatcher,
        Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier,
        Function<CommandRow, String> plannerTagResolver
    ) {
        this.dispatchModePolicy = dispatchModePolicy;
        this.isDuplicateCommandId = isDuplicateCommandId;
        this.commandEnvelopeVerifier = commandEnvelopeVerifier;
        this.rejectionOutcomeMapper = rejectionOutcomeMapper;
        this.commandDispatcher = commandDispatcher;
        this.unsupportedDecisionSupplier = unsupportedDecisionSupplier;
        this.plannerTagResolver = plannerTagResolver;
    }

    ExecutionOutcome evaluate(CommandRow row, int currentTick) {
        return CommandRowDispatchRoutingPolicy.evaluate(
            row,
            currentTick,
            dispatchModePolicy,
            this::evaluateLive,
            this::evaluateShadow
        );
    }

    ExecutionOutcome evaluateShadow(CommandRow row, int currentTick) {
        return CommandShadowEvaluationPolicy.evaluateShadow(
            row,
            currentTick,
            isDuplicateCommandId,
            commandEnvelopeVerifier,
            rejectionOutcomeMapper,
            dispatchModePolicy,
            unsupportedDecisionSupplier,
            plannerTagResolver
        );
    }

    private ExecutionOutcome evaluateLive(CommandRow row) {
        return CommandLiveDispatchPolicy.evaluateLive(
            row,
            isDuplicateCommandId,
            commandEnvelopeVerifier,
            rejectionOutcomeMapper,
            commandDispatcher
        );
    }
}
