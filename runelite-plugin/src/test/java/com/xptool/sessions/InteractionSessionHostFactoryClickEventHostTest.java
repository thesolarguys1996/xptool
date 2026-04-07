package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;
import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryClickEventHostTest {
    @Test
    void createClickEventHostDelegatesInteractionClickEvent() {
        InteractionClickEvent[] lastEvent = {null};
        int[] calls = {0};
        InteractionSessionClickEventService.Host host = InteractionSessionHostFactory.createClickEventHost(clickEvent -> {
            calls[0]++;
            lastEvent[0] = clickEvent;
        });
        InteractionClickEvent clickEvent = sampleEvent(3001L);

        host.onInteractionClickEvent(clickEvent);

        assertEquals(1, calls[0]);
        assertSame(clickEvent, lastEvent[0]);
    }

    @Test
    void createClickEventHostForwardsNullEvent() {
        InteractionClickEvent[] lastEvent = {sampleEvent(1L)};
        int[] calls = {0};
        InteractionSessionClickEventService.Host host = InteractionSessionHostFactory.createClickEventHost(clickEvent -> {
            calls[0]++;
            lastEvent[0] = clickEvent;
        });

        host.onInteractionClickEvent(null);

        assertEquals(1, calls[0]);
        assertNull(lastEvent[0]);
    }

    @Test
    void createClickEventHostFromDelegatesForwardsInteractionClickEvent() {
        InteractionClickEvent[] lastEvent = {null};
        int[] calls = {0};
        InteractionSessionClickEventService.Host host = InteractionSessionHostFactory.createClickEventHostFromDelegates(
            clickEvent -> {
                calls[0]++;
                lastEvent[0] = clickEvent;
            }
        );
        InteractionClickEvent clickEvent = sampleEvent(3002L);

        host.onInteractionClickEvent(clickEvent);

        assertEquals(1, calls[0]);
        assertSame(clickEvent, lastEvent[0]);
    }

    private static InteractionClickEvent sampleEvent(long clickSerial) {
        return new InteractionClickEvent(
            clickSerial,
            10,
            33_000L,
            "interaction",
            "fishing_world_interaction",
            new Point(20, 50),
            new Point(21, 49),
            null,
            123L
        );
    }
}
