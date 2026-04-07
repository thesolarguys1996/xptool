package com.xptool.executor;

import java.awt.Canvas;
import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import javax.swing.SwingUtilities;

final class FocusMenuInteractionController {
    interface Host {
        Canvas clientCanvas();

        boolean isMenuOpen();

        Robot getOrCreateRobot();

        boolean canPerformMotorActionNow();

        Point jitterWithinBounds(Point base, Rectangle bounds, int radiusPx);

        void moveMouseCurve(Robot robot, Point to);

        void sleepCritical(long ms);

        void sleepNoCooldown(long ms);

        void sleepQuietly(long ms);

        void noteMotorAction();
    }

    static final class Config {
        final int focusAnchorJitterPx;

        Config(int focusAnchorJitterPx) {
            this.focusAnchorJitterPx = Math.max(0, focusAnchorJitterPx);
        }
    }

    private final Host host;
    private final Config config;

    FocusMenuInteractionController(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    boolean waitForMenuOpen(long timeoutMs) {
        long budgetMs = Math.max(1L, timeoutMs);
        long deadlineMs = System.currentTimeMillis() + budgetMs;
        while (System.currentTimeMillis() <= deadlineMs) {
            if (host.isMenuOpen()) {
                return true;
            }
            host.sleepNoCooldown(1L);
        }
        return host.isMenuOpen();
    }

    boolean focusClientWindowAndCanvas() {
        return focusClientWindowAndCanvas(true);
    }

    boolean focusClientWindowAndCanvas(boolean allowActivationClick) {
        return focusClientWindowAndCanvas(allowActivationClick, true);
    }

    boolean focusClientWindowAndCanvas(boolean allowActivationClick, boolean reserveGlobalCooldown) {
        Canvas canvas = host.clientCanvas();
        if (canvas == null) {
            return false;
        }
        Window window = SwingUtilities.getWindowAncestor(canvas);
        if (window == null) {
            return false;
        }
        try {
            window.toFront();
            window.requestFocus();
            canvas.requestFocus();
            canvas.requestFocusInWindow();
        } catch (Exception ignored) {
            return false;
        }
        if (reserveGlobalCooldown) {
            host.sleepCritical(8L);
        } else {
            host.sleepNoCooldown(8L);
        }
        if (isClientCanvasFocused()) {
            return true;
        }
        Robot robot = host.getOrCreateRobot();
        if (allowActivationClick && robot != null) {
            clickCanvasActivationAnchor(robot, reserveGlobalCooldown, reserveGlobalCooldown);
            if (reserveGlobalCooldown) {
                host.sleepCritical(8L);
            } else {
                host.sleepNoCooldown(8L);
            }
            if (isClientCanvasFocused()) {
                return true;
            }
        }
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        return window.isActive() || window == active;
    }

    boolean isClientCanvasFocused() {
        Canvas canvas = host.clientCanvas();
        if (canvas == null) {
            return false;
        }
        Window window = SwingUtilities.getWindowAncestor(canvas);
        if (window == null) {
            return false;
        }
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        return (window.isActive() || window == active) && canvas.isFocusOwner();
    }

    boolean clickCanvasActivationAnchor(Robot robot) {
        return clickCanvasActivationAnchor(robot, true, true);
    }

    boolean clickCanvasActivationAnchor(
        Robot robot,
        boolean reserveGlobalCooldown,
        boolean requireMotorGate
    ) {
        if (robot == null) {
            return false;
        }
        if (requireMotorGate && !host.canPerformMotorActionNow()) {
            return false;
        }
        Canvas canvas = host.clientCanvas();
        if (canvas == null) {
            return false;
        }
        try {
            Point origin = canvas.getLocationOnScreen();
            int width = Math.max(40, canvas.getWidth());
            int height = Math.max(40, canvas.getHeight());
            int x = origin.x + Math.min(width - 10, 24);
            int y = origin.y + Math.max(12, height - 24);
            Point anchor = host.jitterWithinBounds(
                new Point(x, y),
                new Rectangle(origin.x + 8, origin.y + 8, Math.max(1, width - 16), Math.max(1, height - 16)),
                config.focusAnchorJitterPx
            );
            host.moveMouseCurve(robot, anchor);
            if (reserveGlobalCooldown) {
                host.sleepQuietly(3L);
            } else {
                host.sleepNoCooldown(3L);
            }
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            if (reserveGlobalCooldown) {
                host.sleepQuietly(3L);
            } else {
                host.sleepNoCooldown(3L);
            }
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            if (reserveGlobalCooldown) {
                host.sleepQuietly(8L);
            } else {
                host.sleepNoCooldown(8L);
            }
            host.noteMotorAction();
            return true;
        } catch (IllegalComponentStateException ex) {
            return false;
        }
    }
}
