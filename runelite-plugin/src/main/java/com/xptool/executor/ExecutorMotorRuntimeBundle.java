package com.xptool.executor;

final class ExecutorMotorRuntimeBundle {
    final MotorCanvasMoveEngine motorCanvasMoveEngine;
    final MotorProgramMoveEngine motorProgramMoveEngine;
    final MotorRuntime motorRuntime;

    ExecutorMotorRuntimeBundle(
        MotorCanvasMoveEngine motorCanvasMoveEngine,
        MotorProgramMoveEngine motorProgramMoveEngine,
        MotorRuntime motorRuntime
    ) {
        this.motorCanvasMoveEngine = motorCanvasMoveEngine;
        this.motorProgramMoveEngine = motorProgramMoveEngine;
        this.motorRuntime = motorRuntime;
    }
}
