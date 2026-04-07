package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;
import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryClickEventServiceHostDecompositionTest {
    @Test
    void createClickEventServiceFromHostRoutesInteractionClickEventLifecycle() {
        TestHost host = new TestHost();
        InteractionSessionClickEventService service = InteractionSessionHostFactory.createClickEventServiceFromHost(host);
        InteractionClickEvent clickEvent = sampleEvent(7001L);

        service.onInteractionClickEvent(clickEvent);
        service.onInteractionClickEvent(null);

        assertEquals(2, host.calls);
        assertNull(host.lastEvent);
        assertSame(clickEvent, host.firstEvent);
    }

    private static InteractionClickEvent sampleEvent(long clickSerial) {
        return new InteractionClickEvent(
            clickSerial,
            13,
            52_000L,
            "interaction",
            "fishing_world_interaction",
            new Point(42, 75),
            new Point(43, 74),
            null,
            201L
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
