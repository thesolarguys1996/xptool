package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleKeyPolicyTest {
    @Test
    void defaultSessionInteractionKeyReturnsInteractionKey() {
        assertEquals(
            "interaction",
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()
        );
    }
}
