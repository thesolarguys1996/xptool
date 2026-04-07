package com.xptool.executor;

final class ExecutorGameplayRuntimeBundle {
    final ExecutorServiceBundle serviceBundle;
    final RandomEventDismissRuntime randomEventDismissRuntime;
    final MotorProgramLifecycleEngine motorProgramLifecycleEngine;

    ExecutorGameplayRuntimeBundle(
        ExecutorServiceBundle serviceBundle,
        RandomEventDismissRuntime randomEventDismissRuntime,
        MotorProgramLifecycleEngine motorProgramLifecycleEngine
    ) {
        this.serviceBundle = serviceBundle;
        this.randomEventDismissRuntime = randomEventDismissRuntime;
        this.motorProgramLifecycleEngine = motorProgramLifecycleEngine;
    }
}
