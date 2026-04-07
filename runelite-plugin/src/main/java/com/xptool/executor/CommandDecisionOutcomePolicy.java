package com.xptool.executor;

final class CommandDecisionOutcomePolicy {
    private CommandDecisionOutcomePolicy() {
    }

    static ExecutionOutcome outcomeFromDecision(CommandExecutor.CommandDecision decision) {
        CommandOutcomeClassifier.Result result = CommandOutcomeClassifier.classify(decision);
        if (result == null) {
            return ExecutionOutcome.failed("null_command_decision", null);
        }
        switch (result.kind) {
            case DISPATCHED:
                return ExecutionOutcome.dispatched(result.reason, result.details);
            case TERMINAL:
                return ExecutionOutcome.terminal(result.reason, result.details);
            case FAILED:
                return ExecutionOutcome.failed(result.reason, result.details);
            case DEFERRED:
            default:
                return ExecutionOutcome.deferred(result.reason, result.details);
        }
    }
}
