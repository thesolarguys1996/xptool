package com.xptool.executor;

final class MotorProgramTerminalService {
    interface Host {
        void releaseIdleMotorOwnership();

        String normalizedMotorOwnerName(String owner);
    }

    private final MotorProgramLifecycleEngine lifecycleEngine;
    private final Host host;

    MotorProgramTerminalService(MotorProgramLifecycleEngine lifecycleEngine, Host host) {
        this.lifecycleEngine = lifecycleEngine;
        this.host = host;
    }

    boolean validateMotorProgramMenu(MotorProgram program) {
        return lifecycleEngine.validateMotorProgramMenu(program);
    }

    void completeMotorProgram(MotorProgram program, String reason) {
        lifecycleEngine.completeMotorProgram(program, reason);
        releaseIdleMotorOwnershipForTerminalProgram(program);
    }

    void cancelMotorProgram(MotorProgram program, String reason) {
        lifecycleEngine.cancelMotorProgram(program, reason);
        releaseIdleMotorOwnershipForTerminalProgram(program);
    }

    void failMotorProgram(MotorProgram program, String reason) {
        lifecycleEngine.failMotorProgram(program, reason);
        releaseIdleMotorOwnershipForTerminalProgram(program);
    }

    void releaseIdleMotorOwnershipAfterSuppression() {
        host.releaseIdleMotorOwnership();
    }

    void releaseIdleMotorOwnershipForRuntimeTeardown() {
        host.releaseIdleMotorOwnership();
    }

    private void releaseIdleMotorOwnershipForTerminalProgram(MotorProgram program) {
        if (program == null || program.profile == null) {
            return;
        }
        String owner = host.normalizedMotorOwnerName(program.profile.owner);
        if (ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE.equals(owner)) {
            host.releaseIdleMotorOwnership();
        }
    }
}
