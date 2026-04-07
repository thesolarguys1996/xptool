package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DropSweepSessionServiceTest {
    @Test
    void beginSessionSetsActiveStateAndResetsProgressTracking() {
        DropSweepSessionService service = new DropSweepSessionService();

        boolean changed = service.beginSession(1511, Set.of(1511, 1513), 10);
        assertTrue(changed);
        assertTrue(service.hasActiveSession());
        assertEquals(1511, service.itemId());
        assertEquals(Set.of(1511, 1513), service.activeItemIds());
        assertEquals(0, service.nextSlot());
        assertTrue(service.awaitingFirstCursorSync());
    }

    @Test
    void endSessionClearsStateAndRecordsEndTimestamp() {
        DropSweepSessionService service = new DropSweepSessionService();
        service.beginSession(1511, Set.of(1511), 8);

        boolean wasActive = service.endSession(12345L);
        assertTrue(wasActive);
        assertFalse(service.hasActiveSession());
        assertEquals(12345L, service.lastSessionEndedAtMs());
        assertEquals(Set.of(), service.activeItemIds());
    }

    @Test
    void updateProgressStateRespectsNoProgressLimit() {
        DropSweepSessionService service = new DropSweepSessionService();
        service.beginSession(1511, Set.of(1511), 5);
        service.setLastDispatchTick(20);
        service.setProgressCheckPending(true);

        assertTrue(service.updateProgressState(5, 21, 2));

        service.setProgressCheckPending(true);
        assertFalse(service.updateProgressState(5, 22, 1));
    }

    @Test
    void noteDispatchFailureTracksFailureLimit() {
        DropSweepSessionService service = new DropSweepSessionService();
        service.beginSession(1511, Set.of(1511), 2);

        assertTrue(service.noteDispatchFailure(3));
        assertTrue(service.noteDispatchFailure(3));
        assertFalse(service.noteDispatchFailure(3));
        service.clearDispatchFailureStreak();
        assertTrue(service.noteDispatchFailure(3));
    }
}
