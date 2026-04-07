package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ManualMetricsGateTelemetryServiceTest {
    @Test
    void emitsGateEventWhenSignalMissingAndRequested() {
        AtomicInteger tick = new AtomicInteger(42);
        AtomicBoolean hasSignal = new AtomicBoolean(false);
        List<JsonObject> emitted = new ArrayList<>();

        ManualMetricsGateTelemetryService service = new ManualMetricsGateTelemetryService(
            tick::get,
            hasSignal::get,
            () -> "db_parity",
            ExecutorValueParsers::details,
            (status, reason, details, eventType) -> emitted.add(details.deepCopy()),
            24
        );

        assertFalse(service.hasSignalForConsumer("random_event_runtime", true));
        assertEquals(1, emitted.size());
        assertEquals("random_event_runtime", emitted.get(0).get("consumer").getAsString());
        assertEquals("manual_metrics_signal_missing", emitted.get(0).get("reasonCode").getAsString());
        assertFalse(emitted.get(0).get("hasManualMetricsSignal").getAsBoolean());
    }

    @Test
    void suppressesRepeatedGateEventInsideIntervalWindow() {
        AtomicInteger tick = new AtomicInteger(100);
        AtomicBoolean hasSignal = new AtomicBoolean(false);
        List<JsonObject> emitted = new ArrayList<>();

        ManualMetricsGateTelemetryService service = new ManualMetricsGateTelemetryService(
            tick::get,
            hasSignal::get,
            () -> "db_parity",
            ExecutorValueParsers::details,
            (status, reason, details, eventType) -> emitted.add(details.deepCopy()),
            24
        );

        service.emitGateEvent("consumerA", "reasonA", ExecutorValueParsers.details("tick", tick.get()));
        service.emitGateEvent("consumerA", "reasonA", ExecutorValueParsers.details("tick", tick.get() + 1));

        assertEquals(1, emitted.size());

        tick.set(124);
        service.emitGateEvent("consumerA", "reasonA", ExecutorValueParsers.details("tick", tick.get()));
        assertEquals(2, emitted.size());
    }

    @Test
    void returnsTrueWithoutEmitWhenSignalExists() {
        AtomicInteger tick = new AtomicInteger(7);
        AtomicBoolean hasSignal = new AtomicBoolean(true);
        List<JsonObject> emitted = new ArrayList<>();

        ManualMetricsGateTelemetryService service = new ManualMetricsGateTelemetryService(
            tick::get,
            hasSignal::get,
            () -> "db_parity",
            ExecutorValueParsers::details,
            (status, reason, details, eventType) -> emitted.add(details.deepCopy()),
            24
        );

        assertTrue(service.hasSignalForConsumer("consumerA", true));
        assertEquals(0, emitted.size());
    }
}
