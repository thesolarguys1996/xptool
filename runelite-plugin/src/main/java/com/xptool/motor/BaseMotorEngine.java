package com.xptool.motor;

public interface BaseMotorEngine {
    boolean moveMouse(int canvasX, int canvasY);

    boolean click();

    boolean rightClick();

    boolean pressKey(int keyCode);

    boolean drag(int fromCanvasX, int fromCanvasY, int toCanvasX, int toCanvasY);

    int canvasWidth();

    int canvasHeight();
}
