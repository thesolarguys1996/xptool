package com.xptool.executor;

import java.util.function.Function;

final class CommandRowDispatchRoutingPolicy {
    @FunctionalInterface
    interface ShadowEvaluator {
        ExecutionOutcome evaluate(CommandRow row, int currentTick);
    }

    private CommandRowDispatchRoutingPolicy() {
    }

    static ExecutionOutcome evaluate(
        CommandRow row,
        int currentTick,
        BridgeCommandDispatchModePolicy dispatchModePolicy,
        Function<CommandRow, ExecutionOutcome> liveEvaluator,
        ShadowEvaluator shadowEvaluator
    ) {
        String commandType = row == null ? "" : row.commandType;
        if (dispatchModePolicy.shouldEvaluateLiveCommand(commandType)) {
            return liveEvaluator.apply(row);
        }
        return shadowEvaluator.evaluate(row, currentTick);
    }
}
