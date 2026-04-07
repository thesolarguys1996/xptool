package com.xptool.executor;

import java.awt.Point;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdleActionPatternDetectorTest {
    @Test
    void emitsRepeatAvoidedEventWhenRetriesWereBlocked() {
        IdleActionPatternDetector detector = new IdleActionPatternDetector();

        List<IdleActionPatternDetector.DetectionEvent> events = detector.onMoveExecuted(
            120,
            "idle_drift_move",
            new Point(250, 320),
            2,
            false
        );

        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(e -> IdleActionPatternDetector.REASON_REPEAT_AVOIDED.equals(e.reason)));
    }

    @Test
    void emitsConsecutiveReasonPatternEventOnThirdRepeat() {
        IdleActionPatternDetector detector = new IdleActionPatternDetector();

        detector.onMoveExecuted(10, "idle_hover_move", new Point(100, 100), 0, false);
        detector.onMoveExecuted(20, "idle_hover_move", new Point(120, 110), 0, false);
        List<IdleActionPatternDetector.DetectionEvent> events =
            detector.onMoveExecuted(30, "idle_hover_move", new Point(140, 120), 0, false);

        assertTrue(events.stream().anyMatch(e -> IdleActionPatternDetector.REASON_PATTERN_DETECTED.equals(e.reason)));
        IdleActionPatternDetector.DetectionEvent event = events.stream()
            .filter(e -> IdleActionPatternDetector.REASON_PATTERN_DETECTED.equals(e.reason))
            .findFirst()
            .orElseThrow();
        assertEquals("consecutive_reason", event.details.get("patternType").getAsString());
    }

    @Test
    void emitsAlternatingPairPatternOnAbabSequence() {
        IdleActionPatternDetector detector = new IdleActionPatternDetector();

        detector.onMoveExecuted(10, "idle_hover_move", new Point(100, 100), 0, false);
        detector.onMoveExecuted(20, "idle_drift_move", new Point(120, 110), 0, false);
        detector.onMoveExecuted(30, "idle_hover_move", new Point(140, 120), 0, false);
        List<IdleActionPatternDetector.DetectionEvent> events =
            detector.onMoveExecuted(40, "idle_drift_move", new Point(160, 130), 0, false);

        assertTrue(events.stream().anyMatch(e -> IdleActionPatternDetector.REASON_PATTERN_DETECTED.equals(e.reason)));
        IdleActionPatternDetector.DetectionEvent event = events.stream()
            .filter(e -> IdleActionPatternDetector.REASON_PATTERN_DETECTED.equals(e.reason))
            .findFirst()
            .orElseThrow();
        assertEquals("alternating_pair", event.details.get("patternType").getAsString());
    }
}
