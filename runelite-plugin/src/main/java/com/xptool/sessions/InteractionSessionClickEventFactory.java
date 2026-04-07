package com.xptool.sessions;

import com.xptool.executor.InteractionClickEvent;
import java.util.function.Consumer;

final class InteractionSessionClickEventFactory {
    private InteractionSessionClickEventFactory() {
        // Static factory utility.
    }

    static InteractionSessionClickEventService createClickEventService(
        Consumer<InteractionClickEvent> onInteractionClickEvent
    ) {
        return createClickEventServiceFromHost(
            createClickEventHost(onInteractionClickEvent)
        );
    }

    static InteractionSessionClickEventService.Host createClickEventHost(
        Consumer<InteractionClickEvent> onInteractionClickEvent
    ) {
        return createClickEventHostFromDelegates(onInteractionClickEvent);
    }

    static InteractionSessionClickEventService.Host createClickEventHostFromDelegates(
        Consumer<InteractionClickEvent> onInteractionClickEvent
    ) {
        return new InteractionSessionClickEventService.Host() {
            @Override
            public void onInteractionClickEvent(InteractionClickEvent clickEvent) {
                onInteractionClickEvent.accept(clickEvent);
            }
        };
    }

    static InteractionSessionClickEventService createClickEventServiceFromHost(
        InteractionSessionClickEventService.Host host
    ) {
        return new InteractionSessionClickEventService(host);
    }
}
