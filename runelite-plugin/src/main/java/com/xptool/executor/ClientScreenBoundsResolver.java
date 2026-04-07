package com.xptool.executor;

import java.awt.Canvas;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.Optional;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;

final class ClientScreenBoundsResolver {
    private final Client client;

    ClientScreenBoundsResolver(Client client) {
        this.client = client;
    }

    Optional<Rectangle> resolveClientCanvasBoundsScreen() {
        Canvas canvas = client == null ? null : client.getCanvas();
        if (canvas == null) {
            return Optional.empty();
        }
        try {
            Point origin = canvas.getLocationOnScreen();
            int width = Math.max(1, canvas.getWidth());
            int height = Math.max(1, canvas.getHeight());
            return Optional.of(new Rectangle(origin.x, origin.y, width, height));
        } catch (IllegalComponentStateException ex) {
            return Optional.empty();
        }
    }

    Optional<Rectangle> resolveClientWindowBoundsScreen() {
        Canvas canvas = client == null ? null : client.getCanvas();
        if (canvas == null) {
            return Optional.empty();
        }
        Window window = SwingUtilities.getWindowAncestor(canvas);
        if (window == null) {
            return Optional.empty();
        }
        try {
            Point origin = window.getLocationOnScreen();
            int width = Math.max(1, window.getWidth());
            int height = Math.max(1, window.getHeight());
            return Optional.of(new Rectangle(origin.x, origin.y, width, height));
        } catch (IllegalComponentStateException ex) {
            return Optional.empty();
        }
    }

    Optional<Rectangle> resolveScreenBoundsForPoint(Point screenPoint) {
        if (screenPoint == null) {
            return Optional.empty();
        }
        try {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = env.getScreenDevices();
            Rectangle unionBounds = null;
            if (devices != null) {
                for (GraphicsDevice device : devices) {
                    if (device == null) {
                        continue;
                    }
                    GraphicsConfiguration cfg = device.getDefaultConfiguration();
                    if (cfg == null) {
                        continue;
                    }
                    Rectangle bounds = cfg.getBounds();
                    if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
                        continue;
                    }
                    if (bounds.contains(screenPoint)) {
                        return Optional.of(new Rectangle(bounds));
                    }
                    unionBounds = unionBounds == null ? new Rectangle(bounds) : unionBounds.union(bounds);
                }
            }
            return unionBounds == null ? Optional.empty() : Optional.of(unionBounds);
        } catch (HeadlessException ex) {
            return Optional.empty();
        }
    }
}
