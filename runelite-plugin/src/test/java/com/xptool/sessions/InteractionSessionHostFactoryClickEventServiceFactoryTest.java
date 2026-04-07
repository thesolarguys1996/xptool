package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;
import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryClickEventServiceFactoryTest {
    @Test
    void createClickEventServiceRoutesInteractionClickEvent() {
        InteractionClickEvent[] lastEvent = {null};
        int[] calls = {0};
        InteractionSessionClickEventService service = InteractionSessionHostFactory.createClickEventService(clickEvent -> {
            calls[0]++;
            lastEvent[0] = clickEvent;
        });
        InteractionClickEvent clickEvent = sampleEvent(5001L);

        service.onInteractionClickEvent(clickEvent);

        assertEquals(1, calls[0]);
        assertSame(clickEvent, lastEvent[0]);
    }

    @Test
    void createClickEventServiceForwardsNullEvent() {
        InteractionClickEvent[] lastEvent = {sampleEvent(3L)};
        int[] calls = {0};
        InteractionSessionClickEventService service = InteractionSessionHostFactory.createClickEventService(clickEvent -> {
            calls[0]++;
            lastEvent[0] = clickEvent;
        });

        service.onInteractionClickEvent(null);

        assertEquals(1, calls[0]);
        assertNull(lastEvent[0]);
    }

    private static InteractionClickEvent sampleEvent(long clickSerial) {
        return new InteractionClickEvent(
            clickSerial,
            12,
            41_000L,
            "interaction",
            "fishing_world_interaction",
            new Point(18, 52),
            new Point(17, 51),
            null,
            131L
        );
    }
}
