package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class ShopBuyCommandService {
    private static final double SHOP_CLICK_REPEAT_EXCLUSION_PX = 3.0;
    private static final int SHOP_WIDGET_POINT_INSET_PX = 2;
    private static final int SHOP_WIDGET_POINT_ATTEMPTS = 24;

    interface Host {
        CommandExecutor.CommandDecision accept(String reason, JsonObject details);

        CommandExecutor.CommandDecision reject(String reason);

        JsonObject details(Object... kvPairs);

        boolean isShopOpen();

        Widget widgetByPackedId(int packedWidgetId);

        Optional<Point> centerOfWidget(Widget widget);

        boolean isUsableCanvasPoint(Point point);

        boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas);

        boolean waitForMotorActionReady(long timeoutMs);

        long menuActionReadyWaitMaxMs();

        boolean tryPrimaryClick(Point targetPoint, ClickMotionSettings motion);
    }

    private final Host host;
    private Point lastShopItemClickPoint = null;

    ShopBuyCommandService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision executeBuyItem(JsonObject payload, MotionProfile motionProfile) {
        int itemId = asInt(payload == null ? null : payload.get("itemId"), -1);
        int quantity = Math.max(1, asInt(payload == null ? null : payload.get("quantity"), 5));
        if (itemId <= 0) {
            return host.reject("shop_buy_invalid_item_id");
        }
        if (!host.isShopOpen()) {
            return host.accept("shop_buy_shop_not_open", host.details("itemId", itemId, "quantity", quantity));
        }

        Widget itemWidget = resolveShopItemWidget(itemId);
        if (itemWidget == null) {
            return host.accept("shop_buy_item_not_visible", host.details("itemId", itemId, "quantity", quantity));
        }
        int stock = Math.max(0, itemWidget.getItemQuantity());
        if (stock <= 0) {
            return host.accept("shop_buy_item_out_of_stock", host.details("itemId", itemId, "quantity", quantity));
        }

        Point center = resolveWidgetCenter(itemWidget);
        if (center == null) {
            return host.reject("shop_buy_item_center_unavailable");
        }
        Point targetPoint = resolveWidgetClickPoint(itemWidget, center);
        if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
            return host.reject("shop_buy_item_click_point_unavailable");
        }
        if (!host.focusClientWindowAndCanvas(false, false)) {
            return host.reject("shop_buy_focus_failed");
        }
        if (!host.waitForMotorActionReady(host.menuActionReadyWaitMaxMs())) {
            return host.accept(
                "shop_buy_primary_click_cooldown_deferred",
                host.details("itemId", itemId, "quantity", quantity, "stock", stock)
            );
        }
        if (!host.tryPrimaryClick(targetPoint, resolvePrimaryBuyMotion(motionProfile))) {
            return host.accept(
                "shop_buy_primary_click_deferred",
                host.details("itemId", itemId, "quantity", quantity, "stock", stock)
            );
        }
        lastShopItemClickPoint = new Point(targetPoint);
        return host.accept(
            "shop_buy_item_dispatched",
            host.details(
                "itemId", itemId,
                "quantity", quantity,
                "stock", stock,
                "canvasX", targetPoint.x,
                "canvasY", targetPoint.y,
                "mode", "primary_click_buy"
            )
        );
    }

    private static ClickMotionSettings resolvePrimaryBuyMotion(MotionProfile motionProfile) {
        MotionProfile profile = motionProfile == null ? MotionProfile.GENERIC_INTERACT : motionProfile;
        return profile.directClickSettings;
    }

    private Widget resolveShopItemWidget(int itemId) {
        Widget items = host.widgetByPackedId(InterfaceID.Shopmain.ITEMS);
        if (items == null || items.isHidden()) {
            return null;
        }

        Widget[] dynamic = items.getDynamicChildren();
        if (dynamic != null && dynamic.length > 0) {
            for (Widget child : dynamic) {
                if (isMatchingShopItemWidget(child, itemId)) {
                    return child;
                }
            }
        }

        Widget[] children = items.getChildren();
        if (children != null && children.length > 0) {
            for (Widget child : children) {
                if (isMatchingShopItemWidget(child, itemId)) {
                    return child;
                }
            }
        }
        return null;
    }

    private static boolean isMatchingShopItemWidget(Widget widget, int itemId) {
        if (widget == null || widget.isHidden()) {
            return false;
        }
        return widget.getItemId() == itemId;
    }

    private Point resolveWidgetCenter(Widget widget) {
        if (widget == null) {
            return null;
        }
        Optional<Point> centerOpt = host.centerOfWidget(widget);
        if (centerOpt.isPresent()) {
            return centerOpt.get();
        }
        Rectangle bounds = widget.getBounds();
        if (bounds == null || bounds.width <= 1 || bounds.height <= 1) {
            return null;
        }
        return new Point((int) Math.round(bounds.getCenterX()), (int) Math.round(bounds.getCenterY()));
    }

    private Point resolveWidgetClickPoint(Widget widget, Point center) {
        Rectangle bounds = widget == null ? null : widget.getBounds();
        Point lastPoint = lastShopItemClickPoint;
        if (bounds == null || bounds.width <= 1 || bounds.height <= 1) {
            return null;
        }
        return RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            bounds,
            host::isUsableCanvasPoint,
            lastPoint,
            SHOP_CLICK_REPEAT_EXCLUSION_PX,
            SHOP_WIDGET_POINT_INSET_PX,
            SHOP_WIDGET_POINT_ATTEMPTS,
            center
        );
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

}
