package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;
import org.junit.jupiter.api.Test;

class InteractionSessionClickEventFactoryTest {
    @Test
    void createClickEventHostRoutesInteractionClickEvent() {
        TestHost host = new TestHost();
        InteractionSessionClickEventService.Host clickEventHost =
            InteractionSessionClickEventFactory.createClickEventHost(host::onInteractionClickEvent);
        InteractionClickEvent clickEvent = sampleEvent(7001L);

        clickEventHost.onInteractionClickEvent(clickEvent);
        clickEventHost.onInteractionClickEvent(null);

        assertEquals(2, host.calls);
        assertNull(host.lastEvent);
        assertSame(clickEvent, host.firstEvent);
    }

    @Test
    void createClickEventServiceFromHostRoutesInteractionClickEventLifecycle() {
        TestHost host = new TestHost();
        InteractionSessionClickEventService service = InteractionSessionClickEventFactory.createClickEventServiceFromHost(host);
        InteractionClickEvent clickEvent = sampleEvent(8001L);

        service.onInteractionClickEvent(clickEvent);
        service.onInteractionClickEvent(null);

        assertEquals(2, host.calls);
        assertNull(host.lastEvent);
        assertSame(clickEvent, host.firstEvent);
    }

    @Test
    void createClickEventServiceRoutesInteractionClickEventLifecycle() {
        TestHost host = new TestHost();
        InteractionSessionClickEventService service =
            InteractionSessionClickEventFactory.createClickEventService(host::onInteractionClickEvent);
        InteractionClickEvent clickEvent = sampleEvent(9001L);

        service.onInteractionClickEvent(clickEvent);
        service.onInteractionClickEvent(null);

        assertEquals(2, host.calls);
        assertNull(host.lastEvent);
        assertSame(clickEvent, host.firstEvent);
    }

    private static InteractionClickEvent sampleEvent(long clickSerial) {
        return new InteractionClickEvent(
            clickSerial,
            17,
            64_000L,
            "interaction",
            "woodcut_world_interaction",
            new Point(62, 55),
            new Point(60, 53),
            null,
            301L
        );
    }

    private static final class TestHost implements InteractionSessionClickEventService.Host {
        private int calls = 0;
        private InteractionClickEvent firstEvent = null;
        private InteractionClickEvent lastEvent = null;

        @Override
        public void onInteractionClickEvent(InteractionClickEvent clickEvent) {
            calls++;
            if (calls == 1) {
                firstEvent = clickEvent;
            }
            lastEvent = clickEvent;
        }
    }
}
