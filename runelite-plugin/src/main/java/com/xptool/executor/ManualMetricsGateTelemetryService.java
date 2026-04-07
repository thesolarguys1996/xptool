package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.function.Supplier;

final class ManualMetricsGateTelemetryService {
    @FunctionalInterface
    interface EventEmitter {
        void emit(String status, String reason, JsonObject details, String eventType);
    }

    @FunctionalInterface
    interface DetailsBuilder {
        JsonObject build(Object... kvPairs);
    }

    private final Supplier<Integer> currentTickSupplier;
    private final Supplier<Boolean> hasSignalSupplier;
    private final Supplier<String> activeDropCadenceProfileSupplier;
    private final DetailsBuilder detailsBuilder;
    private final EventEmitter eventEmitter;
    private final int minIntervalTicks;

    private int lastGateTelemetryTick = Integer.MIN_VALUE;
    private String lastGateTelemetryKey = "";

    ManualMetricsGateTelemetryService(
        Supplier<Integer> currentTickSupplier,
        Supplier<Boolean> hasSignalSupplier,
        Supplier<String> activeDropCadenceProfileSupplier,
        DetailsBuilder detailsBuilder,
        EventEmitter eventEmitter,
        int minIntervalTicks
    ) {
        this.currentTickSupplier = currentTickSupplier;
        this.hasSignalSupplier = hasSignalSupplier;
        this.activeDropCadenceProfileSupplier = activeDropCadenceProfileSupplier;
        this.detailsBuilder = detailsBuilder;
        this.eventEmitter = eventEmitter;
        this.minIntervalTicks = Math.max(1, minIntervalTicks);
    }

    boolean hasSignalForConsumer(String consumer, boolean emitWhenMissing) {
        boolean hasSignal = hasSignalSupplier.get();
        if (!hasSignal && emitWhenMissing) {
            emitGateEvent(
                consumer,
                "manual_metrics_signal_missing",
                detailsBuilder.build("tick", currentTickSupplier.get())
            );
        }
        return hasSignal;
    }

    void emitGateEvent(String consumer, String reason, JsonObject details) {
        String safeConsumer = ExecutorValueParsers.safeString(consumer).trim().toLowerCase(Locale.ROOT);
        String safeReason = ExecutorValueParsers.safeString(reason).trim().toLowerCase(Locale.ROOT);
        String key = safeConsumer + "|" + safeReason;
        int tick = currentTickSupplier.get();
        int elapsedTicks = elapsedTicksSince(tick, lastGateTelemetryTick);
        if (key.equals(lastGateTelemetryKey)
            && elapsedTicks >= 0
            && elapsedTicks < minIntervalTicks) {
            return;
        }

        lastGateTelemetryKey = key;
        lastGateTelemetryTick = tick;
        JsonObject payload = details == null ? new JsonObject() : details.deepCopy();
        payload.addProperty("consumer", safeConsumer);
        payload.addProperty("reasonCode", safeReason);
        payload.addProperty("hasManualMetricsSignal", hasSignalSupplier.get());
        payload.addProperty("activeDropCadenceProfile", activeDropCadenceProfileSupplier.get());
        eventEmitter.emit("executed", "manual_metrics_runtime_gate", payload, "RUNTIME_GATE");
    }

    private static int elapsedTicksSince(int nowTick, int lastTick) {
        if (lastTick == Integer.MIN_VALUE || nowTick == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return nowTick - lastTick;
    }
}
