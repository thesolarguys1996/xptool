package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;

public final class InteractionClickEvent {
    private final long clickSerial;
    private final int tick;
    private final long clickedAtMs;
    private final String owner;
    private final String clickType;
    private final Point clickCanvasPoint;
    private final Point anchorCanvasPoint;
    private final Rectangle anchorBoundsCanvas;
    private final long motorActionSerialAtClick;

    public InteractionClickEvent(
        long clickSerial,
        int tick,
        long clickedAtMs,
        String owner,
        String clickType,
        Point clickCanvasPoint,
        Point anchorCanvasPoint,
        Rectangle anchorBoundsCanvas,
        long motorActionSerialAtClick
    ) {
        this.clickSerial = clickSerial;
        this.tick = tick;
        this.clickedAtMs = clickedAtMs;
        this.owner = safeString(owner);
        this.clickType = safeString(clickType);
        this.clickCanvasPoint = clickCanvasPoint == null ? null : new Point(clickCanvasPoint);
        this.anchorCanvasPoint = anchorCanvasPoint == null ? null : new Point(anchorCanvasPoint);
        this.anchorBoundsCanvas = anchorBoundsCanvas == null ? null : new Rectangle(anchorBoundsCanvas);
        this.motorActionSerialAtClick = motorActionSerialAtClick;
    }

    public long getClickSerial() {
        return clickSerial;
    }

    public int getTick() {
        return tick;
    }

    public long getClickedAtMs() {
        return clickedAtMs;
    }

    public String getOwner() {
        return owner;
    }

    public String getClickType() {
        return clickType;
    }

    public Point getClickCanvasPoint() {
        return clickCanvasPoint == null ? null : new Point(clickCanvasPoint);
    }

    public Point getAnchorCanvasPoint() {
        return anchorCanvasPoint == null ? null : new Point(anchorCanvasPoint);
    }

    public Rectangle getAnchorBoundsCanvas() {
        return anchorBoundsCanvas == null ? null : new Rectangle(anchorBoundsCanvas);
    }

    public long getMotorActionSerialAtClick() {
        return motorActionSerialAtClick;
    }

    public boolean isSettleEligible() {
        return ExecutorMotorProfileCatalog.CLICK_TYPE_FISHING_WORLD.equals(clickType);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
