package net.runelite.api.coords;

public final class WorldPoint {
    private final int x;
    private final int y;
    private final int plane;

    public WorldPoint(int x, int y, int plane) {
        this.x = x;
        this.y = y;
        this.plane = plane;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPlane() {
        return plane;
    }

    public int distanceTo(WorldPoint other) {
        if (other == null || plane != other.plane) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }
}
