package com.xptool.executor;

import java.util.concurrent.LinkedBlockingDeque;

final class CommandQueueIngestService {
    private final LinkedBlockingDeque<CommandRow> pendingCommands;
    private final CommandQueueIdleArmingService queueIdleArmingService;

    CommandQueueIngestService(
        LinkedBlockingDeque<CommandRow> pendingCommands,
        CommandQueueIdleArmingService queueIdleArmingService
    ) {
        this.pendingCommands = pendingCommands;
        this.queueIdleArmingService = queueIdleArmingService;
    }

    void onParsedCommandRow(CommandRowParser.ParsedCommandRow parsedRow) {
        if (parsedRow == null) {
            return;
        }
        CommandRow row = toCommandRow(parsedRow);
        queueIdleArmingService.onQueuedCommand(row);
        while (!pendingCommands.offerLast(row)) {
            pendingCommands.pollFirst();
        }
    }

    private static CommandRow toCommandRow(CommandRowParser.ParsedCommandRow row) {
        return new CommandRow(
            row.tick,
            row.source,
            row.commandId,
            row.createdAtUnixMillis,
            row.commandType,
            row.commandPayload,
            row.reason
        );
    }
}
