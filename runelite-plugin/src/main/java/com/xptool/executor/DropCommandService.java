package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.function.Supplier;

public final class DropCommandService {
    private final DropRuntime dropRuntime;
    private final Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier;

    public DropCommandService(
        DropRuntime dropRuntime,
        Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier
    ) {
        this.dropRuntime = dropRuntime;
        this.unsupportedDecisionSupplier = unsupportedDecisionSupplier;
    }

    public CommandExecutor.CommandDecision executeStartDropSession(JsonObject payload) {
        return dropRuntime.executeStartDropSession(payload);
    }

    public CommandExecutor.CommandDecision executeStopDropSession(JsonObject payload) {
        return dropRuntime.executeStopDropSession(payload);
    }

    public CommandExecutor.CommandDecision executeDropItem(JsonObject payload) {
        return dropRuntime.executeDropItem(payload);
    }

    public void onGameTick(int tick) {
        dropRuntime.advanceTick(tick);
    }

    public CommandExecutor.CommandDecision rejectUnsupportedCommandType() {
        if (unsupportedDecisionSupplier == null) {
            return null;
        }
        return unsupportedDecisionSupplier.get();
    }
}
