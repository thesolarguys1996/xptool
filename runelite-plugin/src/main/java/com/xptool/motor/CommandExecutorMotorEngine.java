package com.xptool.motor;

import com.xptool.executor.CommandExecutor;

public final class CommandExecutorMotorEngine implements BaseMotorEngine {
    private final CommandExecutor executor;

    public CommandExecutorMotorEngine(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean moveMouse(int canvasX, int canvasY) {
        return executor != null && executor.baseMotorMoveMouse(canvasX, canvasY);
    }

    @Override
    public boolean click() {
        return executor != null && executor.baseMotorClick();
    }

    @Override
    public boolean rightClick() {
        return executor != null && executor.baseMotorRightClick();
    }

    @Override
    public boolean pressKey(int keyCode) {
        return executor != null && executor.baseMotorPressKey(keyCode);
    }

    @Override
    public boolean drag(int fromCanvasX, int fromCanvasY, int toCanvasX, int toCanvasY) {
        return executor != null && executor.baseMotorDrag(fromCanvasX, fromCanvasY, toCanvasX, toCanvasY);
    }

    @Override
    public int canvasWidth() {
        return executor == null ? -1 : executor.baseMotorCanvasWidth();
    }

    @Override
    public int canvasHeight() {
        return executor == null ? -1 : executor.baseMotorCanvasHeight();
    }
}
