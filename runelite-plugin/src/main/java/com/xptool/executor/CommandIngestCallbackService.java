package com.xptool.executor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.function.Consumer;

final class CommandIngestCallbackService {
    @FunctionalInterface
    interface DetailsBuilder {
        JsonObject build(Object... kvPairs);
    }

    @FunctionalInterface
    interface IngestDebugEmitter {
        void emit(String reason, JsonObject details);
    }

    private final DetailsBuilder detailsBuilder;
    private final Consumer<JsonObject> configAcceptedEmitter;
    private final IngestDebugEmitter ingestDebugEmitter;
    private final Consumer<String> failureEmitter;
    private final Runnable clearSeenCommandIds;
    private final Consumer<CommandRowParser.ParsedCommandRow> parsedCommandConsumer;
    private final CommandEnvelopeVerifier commandEnvelopeVerifier;
    private final BridgeCommandDispatchModePolicy bridgeDispatchModePolicy;
    private final boolean loginBreakRuntimeEnabled;
    private final boolean loginBreakRuntimeAutoArm;
    private final boolean commandIngestDebugEnabled;
    private final boolean shadowOnly;

    CommandIngestCallbackService(
        DetailsBuilder detailsBuilder,
        Consumer<JsonObject> configAcceptedEmitter,
        IngestDebugEmitter ingestDebugEmitter,
        Consumer<String> failureEmitter,
        Runnable clearSeenCommandIds,
        Consumer<CommandRowParser.ParsedCommandRow> parsedCommandConsumer,
        CommandEnvelopeVerifier commandEnvelopeVerifier,
        BridgeCommandDispatchModePolicy bridgeDispatchModePolicy,
        boolean loginBreakRuntimeEnabled,
        boolean loginBreakRuntimeAutoArm,
        boolean commandIngestDebugEnabled,
        boolean shadowOnly
    ) {
        this.detailsBuilder = detailsBuilder;
        this.configAcceptedEmitter = configAcceptedEmitter;
        this.ingestDebugEmitter = ingestDebugEmitter;
        this.failureEmitter = failureEmitter;
        this.clearSeenCommandIds = clearSeenCommandIds;
        this.parsedCommandConsumer = parsedCommandConsumer;
        this.commandEnvelopeVerifier = commandEnvelopeVerifier;
        this.bridgeDispatchModePolicy = bridgeDispatchModePolicy;
        this.loginBreakRuntimeEnabled = loginBreakRuntimeEnabled;
        this.loginBreakRuntimeAutoArm = loginBreakRuntimeAutoArm;
        this.commandIngestDebugEnabled = commandIngestDebugEnabled;
        this.shadowOnly = shadowOnly;
    }

    void onConfigPathUpdated(String configuredPath, boolean exists) {
        configAcceptedEmitter.accept(detailsBuilder.build(
            "commandFilePath", configuredPath,
            "commandFilePathConfigured", !configuredPath.isEmpty(),
            "commandFileExists", !configuredPath.isEmpty() && exists,
            "loginBreakRuntimeEnabled", loginBreakRuntimeEnabled,
            "loginBreakRuntimeAutoArm", loginBreakRuntimeAutoArm,
            "commandIngestDebugEnabled", commandIngestDebugEnabled,
            "commandEnvelopeValidationEnabled", commandEnvelopeVerifier.isEnabled(),
            "commandEnvelopeRequired", commandEnvelopeVerifier.isRequired(),
            "commandEnvelopeSignatureVerifyEnabled", commandEnvelopeVerifier.isSignatureVerificationEnabled(),
            "commandEnvelopeSigningKeyConfigured", commandEnvelopeVerifier.hasSigningKey(),
            "commandEnvelopeReplayWindowMs", commandEnvelopeVerifier.replayWindowMs(),
            "commandExecutorShadowOnly", shadowOnly,
            "bridgeRuntimeEnabled", bridgeDispatchModePolicy.isBridgeRuntimeEnabled(),
            "bridgeLiveDispatchEnabled", bridgeDispatchModePolicy.isLiveDispatchEnabled(),
            "bridgeLiveDispatchCommandAllowlist", bridgeDispatchModePolicy.liveDispatchAllowlistCsv()
        ));
    }

    void onCommandFileAttached() {
        clearSeenCommandIds.run();
        ingestDebugEmitter.emit("command_ingest_file_attached", null);
    }

    void onCommandFileTruncated() {
        clearSeenCommandIds.run();
        ingestDebugEmitter.emit("command_ingest_file_truncated", null);
    }

    Optional<CommandRowParser.ParsedCommandRow> parseCommandLine(String line, Gson gson) {
        String safeLine = safeString(line);
        JsonObject lineDetails = detailsBuilder.build(
            "lineLength", safeLine.length(),
            "lineHash", Integer.toHexString(safeLine.hashCode())
        );
        ingestDebugEmitter.emit("command_ingest_line_seen", lineDetails);
        Optional<CommandRowParser.ParsedCommandRow> parsed = CommandRowParser.parseCommandLine(safeLine, gson);
        if (parsed.isPresent()) {
            CommandRowParser.ParsedCommandRow row = parsed.get();
            ingestDebugEmitter.emit(
                "command_ingest_line_parsed",
                detailsBuilder.build(
                    "source", safeString(row.source),
                    "commandType", safeString(row.commandType),
                    "commandId", safeString(row.commandId),
                    "tick", row.tick
                )
            );
        } else {
            ingestDebugEmitter.emit("command_ingest_line_parse_skipped", lineDetails);
        }
        return parsed;
    }

    void onParsedCommandRow(CommandRowParser.ParsedCommandRow parsed) {
        ingestDebugEmitter.emit(
            "command_ingest_row_queued",
            detailsBuilder.build(
                "source", safeString(parsed.source),
                "commandType", safeString(parsed.commandType),
                "commandId", safeString(parsed.commandId),
                "tick", parsed.tick
            )
        );
        parsedCommandConsumer.accept(parsed);
    }

    void onFailure(String reason) {
        failureEmitter.accept(reason);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
