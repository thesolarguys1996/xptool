package com.xptool.sessions.idle;

public final class IdleBehaviorProfile {
    private final int hoverChancePercent;
    private final int driftChancePercent;
    private final int offscreenParkChancePercent;
    private final int cameraChancePercent;
    private final int noopChancePercent;
    private final int parkAfterBurstMinActions;
    private final int parkAfterBurstChancePercent;

    public IdleBehaviorProfile(
        int hoverChancePercent,
        int driftChancePercent,
        int offscreenParkChancePercent,
        int cameraChancePercent,
        int noopChancePercent,
        int parkAfterBurstMinActions,
        int parkAfterBurstChancePercent
    ) {
        this.hoverChancePercent = clampPercent(hoverChancePercent);
        this.driftChancePercent = clampPercent(driftChancePercent);
        this.offscreenParkChancePercent = clampPercent(offscreenParkChancePercent);
        this.cameraChancePercent = clampPercent(cameraChancePercent);
        this.noopChancePercent = clampPercent(noopChancePercent);
        this.parkAfterBurstMinActions = Math.max(0, parkAfterBurstMinActions);
        this.parkAfterBurstChancePercent = clampPercent(parkAfterBurstChancePercent);
    }

    public int hoverChancePercent() {
        return hoverChancePercent;
    }

    public int driftChancePercent() {
        return driftChancePercent;
    }

    public int offscreenParkChancePercent() {
        return offscreenParkChancePercent;
    }

    public int cameraChancePercent() {
        return cameraChancePercent;
    }

    public int noopChancePercent() {
        return noopChancePercent;
    }

    public int parkAfterBurstMinActions() {
        return parkAfterBurstMinActions;
    }

    public int parkAfterBurstChancePercent() {
        return parkAfterBurstChancePercent;
    }

    public int totalActionPercent() {
        return hoverChancePercent
            + driftChancePercent
            + offscreenParkChancePercent
            + cameraChancePercent
            + noopChancePercent;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
