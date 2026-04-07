package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandIdDeduplicationServiceTest {
    @Test
    void returnsFalseForFirstSeenCommandIdAndTrueForDuplicates() {
        CommandIdDeduplicationService service = new CommandIdDeduplicationService(8);

        assertFalse(service.isDuplicateCommandId("cmd-1"));
        assertTrue(service.isDuplicateCommandId("cmd-1"));
    }

    @Test
    void clearSeenCommandIdsResetsDuplicateTracking() {
        CommandIdDeduplicationService service = new CommandIdDeduplicationService(8);

        assertFalse(service.isDuplicateCommandId("cmd-1"));
        assertTrue(service.isDuplicateCommandId("cmd-1"));
        service.clearSeenCommandIds();
        assertFalse(service.isDuplicateCommandId("cmd-1"));
    }

    @Test
    void evictsOldestCommandIdsWhenCapacityIsExceeded() {
        CommandIdDeduplicationService service = new CommandIdDeduplicationService(2);

        assertFalse(service.isDuplicateCommandId("cmd-1"));
        assertFalse(service.isDuplicateCommandId("cmd-2"));
        assertFalse(service.isDuplicateCommandId("cmd-3"));

        assertFalse(service.isDuplicateCommandId("cmd-1"));
        assertTrue(service.isDuplicateCommandId("cmd-3"));
    }
}
