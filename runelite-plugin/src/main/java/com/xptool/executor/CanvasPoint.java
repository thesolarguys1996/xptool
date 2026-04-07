package com.xptool.executor;

import java.awt.Point;

final class CanvasPoint {
    final int x;
    final int y;

    CanvasPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    static CanvasPoint fromAwtPoint(Point p) {
        if (p == null) {
            return null;
        }
        return new CanvasPoint(p.x, p.y);
    }

    Point toAwtPoint() {
        return new Point(x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CanvasPoint)) {
            return false;
        }
        CanvasPoint other = (CanvasPoint) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + x;
        result = 31 * result + y;
        return result;
    }
}
