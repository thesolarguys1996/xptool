package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MotorDispatchContextServiceTest {
    @Test
    void pushAndPopMotorOwnerContextNormalizesValues() {
        MotorDispatchContextService service = new MotorDispatchContextService(new TestHost(), "none");

        assertEquals("", service.activeMotorOwnerContext());

        String previous = service.pushMotorOwnerContext("  IDLE  ");
        assertEquals("", previous);
        assertEquals("idle", service.activeMotorOwnerContext());

        service.popMotorOwnerContext(" INTERACTION ");
        assertEquals("interaction", service.activeMotorOwnerContext());
    }

    @Test
    void pushAndPopClickTypeContextTracksCurrentClickType() {
        MotorDispatchContextService service = new MotorDispatchContextService(new TestHost(), "none");

        assertEquals("none", service.activeClickTypeContext());

        String previous = service.pushClickTypeContext("bank");
        assertEquals("none", previous);
        assertEquals("bank", service.activeClickTypeContext());

        service.popClickTypeContext(null);
        assertEquals("", service.activeClickTypeContext());
    }

    private static final class TestHost implements MotorDispatchContextService.Host {
        @Override
        public String normalizedMotorOwnerName(String owner) {
            return ExecutorValueParsers.safeString(owner).trim().toLowerCase();
        }

        @Override
        public String safeClickType(String clickType) {
            return ExecutorValueParsers.safeString(clickType);
        }
    }
}
