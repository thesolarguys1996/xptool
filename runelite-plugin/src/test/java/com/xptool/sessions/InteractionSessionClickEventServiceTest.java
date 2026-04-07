package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;
import org.junit.jupiter.api.Test;

class InteractionSessionClickEventServiceTest {
    @Test
    void onInteractionClickEventDelegatesToHost() {
        TestHost host = new TestHost();
        InteractionSessionClickEventService service = new InteractionSessionClickEventService(host);
        InteractionClickEvent clickEvent = sampleEvent(101L);

        service.onInteractionClickEvent(clickEvent);

        assertEquals(1, host.calls);
        assertSame(clickEvent, host.lastEvent);
    }

    @Test
    void onInteractionClickEventForwardsNullEvent() {
        TestHost host = new TestHost();
        InteractionSessionClickEventService service = new InteractionSessionClickEventService(host);

        service.onInteractionClickEvent(null);

        assertEquals(1, host.calls);
        assertNull(host.lastEvent);
    }

    private static InteractionClickEvent sampleEvent(long clickSerial) {
        return new InteractionClickEvent(
            clickSerial,
            1,
            25_000L,
            "interaction",
            "fishing_world_interaction",
            new Point(30, 40),
            new Point(31, 39),
            null,
            500L
        );
    }

    private static final class TestHost implements InteractionSessionClickEventService.Host {
        private int calls;
        private InteractionClickEvent lastEvent;

        @Override
        public void onInteractionClickEvent(InteractionClickEvent clickEvent) {
            calls++;
            lastEvent = clickEvent;
        }
    }
}
