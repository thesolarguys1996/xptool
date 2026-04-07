package com.xptool.executor;

final class MotorDispatchContextService {
    interface Host {
        String normalizedMotorOwnerName(String owner);

        String safeClickType(String clickType);
    }

    private final Host host;
    private String activeMotorOwnerContext = "";
    private String activeClickTypeContext;

    MotorDispatchContextService(Host host, String initialClickType) {
        this.host = host;
        this.activeClickTypeContext = host.safeClickType(initialClickType);
    }

    String activeMotorOwnerContext() {
        return activeMotorOwnerContext;
    }

    String activeClickTypeContext() {
        return activeClickTypeContext;
    }

    String pushMotorOwnerContext(String owner) {
        String previous = activeMotorOwnerContext;
        activeMotorOwnerContext = host.normalizedMotorOwnerName(owner);
        return previous;
    }

    void popMotorOwnerContext(String previous) {
        activeMotorOwnerContext = host.normalizedMotorOwnerName(previous);
    }

    String pushClickTypeContext(String clickType) {
        String previous = activeClickTypeContext;
        activeClickTypeContext = host.safeClickType(clickType);
        return previous;
    }

    void popClickTypeContext(String previous) {
        activeClickTypeContext = host.safeClickType(previous);
    }
}
