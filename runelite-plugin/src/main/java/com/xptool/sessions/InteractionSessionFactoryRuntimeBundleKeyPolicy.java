package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleKeyPolicy {
    private static final String DEFAULT_SESSION_INTERACTION_KEY = "interaction";

    private InteractionSessionFactoryRuntimeBundleKeyPolicy() {
        // Static policy utility.
    }

    static String defaultSessionInteractionKey() {
        return DEFAULT_SESSION_INTERACTION_KEY;
    }
}
