package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PendingMoveTelemetryServiceTest {
    @Test
    void notePendingMoveAgeTracksMaxTicksAlive() {
        PendingMoveTelemetryService service = new PendingMoveTelemetryService(
            ExecutorValueParsers::details,
            (reason, details) -> {},
            fallback -> new Point(0, 0)
        );
        PendingMouseMove pending = pending(new Point(0, 0), new Point(10, 0));

        service.notePendingMoveAge(pending, 10);
        service.notePendingMoveAge(pending, 11);

        assertEquals(2, service.pendingMoveTicksAliveMax());
    }

    @Test
    void notePendingMoveRemainingDistanceTracksAveragesAndBounds() {
        PendingMoveTelemetryService service = new PendingMoveTelemetryService(
            ExecutorValueParsers::details,
            (reason, details) -> {},
            fallback -> new Point(0, 0)
        );

        service.notePendingMoveRemainingDistance(pending(new Point(0, 0), new Point(3, 4)));
        service.notePendingMoveRemainingDistance(pending(new Point(0, 0), new Point(6, 8)));

        assertEquals(7.5, service.averageRemainingDistancePx());
        assertEquals(5.0, service.minRemainingDistancePx());
        assertEquals(10.0, service.maxRemainingDistancePx());
    }

    @Test
    void notePendingMoveBlockedSuppressesDuplicateEventsPerTickReasonAndTarget() {
        List<JsonObject> emitted = new ArrayList<>();
        PendingMoveTelemetryService service = new PendingMoveTelemetryService(
            ExecutorValueParsers::details,
            (reason, details) -> emitted.add(details.deepCopy()),
            fallback -> fallback
        );
        PendingMouseMove pending = pending(new Point(0, 0), new Point(10, 20));

        service.notePendingMoveBlocked(pending, "next_allowed_tick", 50);
        service.notePendingMoveBlocked(pending, "next_allowed_tick", 50);
        service.notePendingMoveBlocked(pending, "mouse_mutation_budget_unavailable", 50);

        assertEquals(2, emitted.size());
        assertEquals("next_allowed_tick", emitted.get(0).get("reasonCode").getAsString());
        assertEquals("mouse_mutation_budget_unavailable", emitted.get(1).get("reasonCode").getAsString());
    }

    private static PendingMouseMove pending(Point from, Point to) {
        return new PendingMouseMove(
            null,
            from,
            to,
            null,
            "idle",
            12,
            0
        );
    }
}
