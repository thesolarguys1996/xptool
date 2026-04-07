package com.xptool.executor;

final class BrutusMotorPreemptor {
    interface Host {
        MotorProgram activeMotorProgram();

        void clearActiveMotorProgram();

        String normalizedMotorOwnerName(String owner);

        String interactionMotorOwner();

        void cancelMotorProgram(MotorProgram program, String reason);

        void clearPendingMouseMove();
    }

    private final Host host;

    BrutusMotorPreemptor(Host host) {
        this.host = host;
    }

    boolean preemptActiveInteractionMotorForDodge() {
        MotorProgram active = host.activeMotorProgram();
        if (active == null || active.profile == null) {
            return false;
        }
        MotorHandle handle = active.toHandle();
        if (handle.isTerminal()) {
            host.clearActiveMotorProgram();
            return false;
        }
        String owner = host.normalizedMotorOwnerName(active.profile.owner);
        if (!host.interactionMotorOwner().equals(owner)) {
            return false;
        }
        host.cancelMotorProgram(active, "combat_brutus_dodge_preempt");
        host.clearActiveMotorProgram();
        host.clearPendingMouseMove();
        return true;
    }
}
