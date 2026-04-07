package com.xptool.sessions;

import com.xptool.executor.InteractionClickEvent;

final class InteractionSessionClickEventService {
    interface Host {
        void onInteractionClickEvent(InteractionClickEvent clickEvent);
    }

    private final Host host;

    InteractionSessionClickEventService(Host host) {
        this.host = host;
    }

    void onInteractionClickEvent(InteractionClickEvent clickEvent) {
        host.onInteractionClickEvent(clickEvent);
    }
}
