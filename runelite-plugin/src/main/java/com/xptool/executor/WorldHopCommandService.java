package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class WorldHopCommandService {
    private static final double WORLD_HOP_CLICK_REPEAT_EXCLUSION_PX = 3.0;
    private static final int WORLD_HOP_WIDGET_POINT_INSET_PX = 2;
    private static final int WORLD_HOP_WIDGET_POINT_ATTEMPTS = 24;
    private static final Pattern WORLD_ID_PATTERN = Pattern.compile("\\b(\\d{3})\\b");
    private static final String[] SHOP_CLOSE_KEYWORDS = {"close", "exit"};
    private static final int SHOP_CLOSE_MAX_WIDGET_DIM_PX = 140;
    private static final int SHOP_CLOSE_REGION_RIGHT_MARGIN_PX = 6;
    private static final int SHOP_CLOSE_REGION_TOP_MARGIN_PX = 4;
    private static final int SHOP_CLOSE_REGION_WIDTH_PX = 34;
    private static final int SHOP_CLOSE_REGION_HEIGHT_PX = 26;
    private static final int[] WORLD_SWITCHER_BUTTON_WIDGET_IDS = {
        InterfaceID.Logout.WORLD_SWITCHER,
        InterfaceID.Logout.WORLD_SWITCHER_GRAPHIC2,
        InterfaceID.Logout.WORLD_SWITCHER_GRAPHIC1,
        InterfaceID.Logout.WORLD_SWITCHER_GRAPHIC0,
        InterfaceID.Logout.WORLD_SWITCHER_TEXT3
    };
    private static final int[] LOGOUT_TAB_WIDGET_IDS = {
        InterfaceID.Toplevel.ICON10,
        InterfaceID.Toplevel.STONE10,
        InterfaceID.ToplevelOsrsStretch.ICON10,
        InterfaceID.ToplevelOsrsStretch.STONE10,
        InterfaceID.ToplevelPreEoc.ICON10,
        InterfaceID.ToplevelPreEoc.STONE10,
        InterfaceID.ToplevelOsm.ICON10,
        InterfaceID.ToplevelOsm.STONE10,
        InterfaceID.ToplevelOsm.LOGOUT_CONTAINER
    };
    private static final int[] SHOP_ROOT_WIDGET_IDS = {
        InterfaceID.Shopmain.FRAME,
        InterfaceID.Shopmain.UNIVERSE,
        InterfaceID.Shopmain.ITEMS
    };

    interface Host {
        CommandExecutor.CommandDecision accept(String reason, JsonObject details);

        CommandExecutor.CommandDecision reject(String reason);

        JsonObject details(Object... kvPairs);

        boolean isLoggedIn();

        int currentWorld();

        Widget widgetByPackedId(int packedWidgetId);

        Optional<Widget> findVisibleWidgetByKeywords(String... keywords);

        Optional<Point> centerOfWidget(Widget widget);

        boolean isUsableCanvasPoint(Point point);

        boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas);

        boolean waitForMotorActionReady(long timeoutMs);

        long menuActionReadyWaitMaxMs();

        boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion);
    }

    private final Host host;
    private Point lastSwitcherClickPoint = null;
    private Point lastWorldEntryClickPoint = null;
    private Point lastShopCloseClickPoint = null;

    WorldHopCommandService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision execute(JsonObject payload, MotionProfile motionProfile) {
        if (!host.isLoggedIn()) {
            return host.accept("world_hop_not_logged_in", null);
        }

        int targetWorld = asInt(payload == null ? null : payload.get("targetWorld"), -1);
        int currentWorld = host.currentWorld();
        if (targetWorld > 0 && currentWorld > 0 && targetWorld == currentWorld) {
            return host.accept(
                "world_hop_already_on_target",
                host.details("targetWorld", targetWorld, "currentWorld", currentWorld)
            );
        }

        ClickMotionSettings motion = motionProfile == null
            ? MotionProfile.GENERIC_INTERACT.resolveClickSettings(payload)
            : motionProfile.resolveClickSettings(payload);

        if (isShopOpen()) {
            if (closeShopInterface(motion)) {
                return host.accept(
                    "world_hop_close_shop_pending",
                    host.details("targetWorld", targetWorld, "currentWorld", currentWorld)
                );
            }
            return host.reject("world_hop_close_shop_unavailable");
        }

        if (isWorldSwitcherOpen()) {
            Widget worldWidget = resolveTargetWorldWidget(targetWorld, currentWorld);
            if (worldWidget == null) {
                return host.accept(
                    "world_hop_target_widget_unavailable",
                    host.details("targetWorld", targetWorld, "currentWorld", currentWorld)
                );
            }
            Point center = resolveWidgetCenter(worldWidget);
            Point targetPoint = resolveWidgetClickPoint(worldWidget, center, false);
            if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
                return host.reject("world_hop_target_point_unavailable");
            }
            if (!host.waitForMotorActionReady(host.menuActionReadyWaitMaxMs())) {
                return host.accept(
                    "world_hop_target_click_cooldown_deferred",
                    host.details("targetWorld", targetWorld, "currentWorld", currentWorld)
                );
            }
            if (!host.focusClientWindowAndCanvas(false, false)) {
                return host.reject("world_hop_focus_failed");
            }
            if (!host.clickCanvasPoint(targetPoint, motion)) {
                return host.reject("world_hop_target_click_failed");
            }
            lastWorldEntryClickPoint = new Point(targetPoint);
            int resolvedWorld = extractWorldFromWidget(worldWidget);
            return host.accept(
                "world_hop_target_click_dispatched",
                host.details(
                    "targetWorld", targetWorld,
                    "resolvedWorld", resolvedWorld,
                    "currentWorld", currentWorld,
                    "canvasX", targetPoint.x,
                    "canvasY", targetPoint.y
                )
            );
        }

        if (!isLogoutPanelOpen()) {
            if (clickFirstVisibleWidget(LOGOUT_TAB_WIDGET_IDS, motion)) {
                return host.accept(
                    "world_hop_open_logout_tab_pending",
                    host.details("targetWorld", targetWorld, "currentWorld", currentWorld)
                );
            }
            return host.reject("world_hop_open_logout_tab_unavailable");
        }

        if (clickFirstVisibleWidget(WORLD_SWITCHER_BUTTON_WIDGET_IDS, motion)) {
            return host.accept(
                "world_hop_open_switcher_pending",
                host.details("targetWorld", targetWorld, "currentWorld", currentWorld)
            );
        }

        Optional<Widget> keywordWidget = host.findVisibleWidgetByKeywords("world switcher", "switch world", "world");
        if (keywordWidget.isPresent()) {
            Point center = resolveWidgetCenter(keywordWidget.get());
            Point targetPoint = resolveWidgetClickPoint(keywordWidget.get(), center, true);
            if (targetPoint != null
                && host.isUsableCanvasPoint(targetPoint)
                && host.waitForMotorActionReady(host.menuActionReadyWaitMaxMs())
                && host.focusClientWindowAndCanvas(false, false)
                && host.clickCanvasPoint(targetPoint, motion)) {
                lastSwitcherClickPoint = new Point(targetPoint);
                return host.accept(
                    "world_hop_open_switcher_pending",
                    host.details("targetWorld", targetWorld, "currentWorld", currentWorld)
                );
            }
        }

        return host.reject("world_hop_switcher_unavailable");
    }

    private boolean isWorldSwitcherOpen() {
        Widget root = host.widgetByPackedId(InterfaceID.Worldswitcher.UNIVERSE);
        if (root != null && !root.isHidden()) {
            return true;
        }
        Widget optionsRoot = host.widgetByPackedId(InterfaceID.WorldswitcherOptions.UNIVERSE);
        return optionsRoot != null && !optionsRoot.isHidden();
    }

    private boolean isShopOpen() {
        Widget shopRoot = host.widgetByPackedId(InterfaceID.Shopmain.UNIVERSE);
        if (shopRoot != null && !shopRoot.isHidden()) {
            return true;
        }
        Widget items = host.widgetByPackedId(InterfaceID.Shopmain.ITEMS);
        return items != null && !items.isHidden();
    }

    private boolean isLogoutPanelOpen() {
        Widget logoutUniverse = host.widgetByPackedId(InterfaceID.Logout.UNIVERSE);
        if (logoutUniverse != null && !logoutUniverse.isHidden()) {
            return true;
        }
        Widget logoutButtons = host.widgetByPackedId(InterfaceID.Logout.LOGOUT_BUTTONS);
        return logoutButtons != null && !logoutButtons.isHidden();
    }

    private boolean closeShopInterface(ClickMotionSettings motion) {
        Widget closeWidget = resolveShopCloseWidget();
        Point center = resolveWidgetCenter(closeWidget);
        Point targetPoint = resolveWidgetClickPoint(closeWidget, center, lastShopCloseClickPoint);
        if (targetPoint == null) {
            targetPoint = resolveShopFrameClosePoint();
        }
        if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
            return false;
        }
        if (!host.waitForMotorActionReady(host.menuActionReadyWaitMaxMs())) {
            return false;
        }
        if (!host.focusClientWindowAndCanvas(false, false)) {
            return false;
        }
        if (!host.clickCanvasPoint(targetPoint, motion)) {
            return false;
        }
        lastShopCloseClickPoint = new Point(targetPoint);
        return true;
    }

    private Point resolveShopFrameClosePoint() {
        Rectangle frame = resolveShopFrameBounds();
        if (frame == null || frame.width <= 6 || frame.height <= 6) {
            return null;
        }
        int closeWidth = Math.min(Math.max(12, SHOP_CLOSE_REGION_WIDTH_PX), frame.width);
        int closeHeight = Math.min(Math.max(10, SHOP_CLOSE_REGION_HEIGHT_PX), frame.height);
        int x = (frame.x + frame.width) - closeWidth - SHOP_CLOSE_REGION_RIGHT_MARGIN_PX;
        int y = frame.y + SHOP_CLOSE_REGION_TOP_MARGIN_PX;
        Rectangle closeRegion = new Rectangle(x, y, closeWidth, closeHeight).intersection(frame);
        if (closeRegion.width <= 1 || closeRegion.height <= 1) {
            return null;
        }
        Point fallback = new Point(
            (int) Math.round(closeRegion.getCenterX()),
            (int) Math.round(closeRegion.getCenterY())
        );
        return RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            closeRegion,
            host::isUsableCanvasPoint,
            lastShopCloseClickPoint,
            WORLD_HOP_CLICK_REPEAT_EXCLUSION_PX,
            0,
            WORLD_HOP_WIDGET_POINT_ATTEMPTS,
            fallback
        );
    }

    private Widget resolveShopCloseWidget() {
        Rectangle shopFrameBounds = resolveShopFrameBounds();
        List<Widget> candidates = new ArrayList<>();
        Set<Integer> seenWidgetIds = new HashSet<>();
        for (int rootId : SHOP_ROOT_WIDGET_IDS) {
            collectShopCloseWidgets(host.widgetByPackedId(rootId), candidates, seenWidgetIds, shopFrameBounds);
        }
        Widget best = null;
        long bestScore = Long.MIN_VALUE;
        for (Widget candidate : candidates) {
            long score = shopCloseWidgetScore(candidate, shopFrameBounds);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private Rectangle resolveShopFrameBounds() {
        Widget frame = host.widgetByPackedId(InterfaceID.Shopmain.FRAME);
        if (frame != null && !frame.isHidden() && frame.getBounds() != null) {
            return frame.getBounds();
        }
        Widget universe = host.widgetByPackedId(InterfaceID.Shopmain.UNIVERSE);
        if (universe != null && !universe.isHidden() && universe.getBounds() != null) {
            return universe.getBounds();
        }
        return null;
    }

    private void collectShopCloseWidgets(
        Widget root,
        List<Widget> out,
        Set<Integer> seenWidgetIds,
        Rectangle shopFrameBounds
    ) {
        if (root == null || root.isHidden()) {
            return;
        }
        int id = root.getId();
        if (id != -1) {
            if (seenWidgetIds.contains(id)) {
                return;
            }
            seenWidgetIds.add(id);
        }
        if (isShopCloseWidgetCandidate(root, shopFrameBounds)) {
            out.add(root);
        }
        Widget[] dynamic = root.getDynamicChildren();
        if (dynamic != null) {
            for (Widget child : dynamic) {
                collectShopCloseWidgets(child, out, seenWidgetIds, shopFrameBounds);
            }
        }
        Widget[] children = root.getChildren();
        if (children != null) {
            for (Widget child : children) {
                collectShopCloseWidgets(child, out, seenWidgetIds, shopFrameBounds);
            }
        }
    }

    private static boolean isShopCloseWidgetCandidate(Widget widget, Rectangle shopFrameBounds) {
        if (!isWidgetVisibleAndClickable(widget)) {
            return false;
        }
        if (widget.getItemId() > 0) {
            return false;
        }
        String corpus = normalizeWidgetCorpus(widget);
        if (!corpus.isEmpty()) {
            for (String keyword : SHOP_CLOSE_KEYWORDS) {
                if (corpus.contains(keyword)) {
                    return true;
                }
            }
        }
        if (shopFrameBounds == null) {
            return false;
        }
        Rectangle bounds = widget.getBounds();
        if (bounds == null || bounds.width > SHOP_CLOSE_MAX_WIDGET_DIM_PX || bounds.height > SHOP_CLOSE_MAX_WIDGET_DIM_PX) {
            return false;
        }
        int centerX = bounds.x + (bounds.width / 2);
        int centerY = bounds.y + (bounds.height / 2);
        int frameTopRightX = shopFrameBounds.x + (int) Math.round(shopFrameBounds.width * 0.62);
        int frameTopRightY = shopFrameBounds.y + (int) Math.round(shopFrameBounds.height * 0.45);
        if (centerX < frameTopRightX || centerY > frameTopRightY) {
            return false;
        }
        int widgetRight = bounds.x + bounds.width;
        int frameRight = shopFrameBounds.x + shopFrameBounds.width;
        int rightDelta = Math.abs(frameRight - widgetRight);
        int topDelta = Math.abs(bounds.y - shopFrameBounds.y);
        return rightDelta <= 90 && topDelta <= 90;
    }

    private static long shopCloseWidgetScore(Widget widget, Rectangle shopFrameBounds) {
        if (!isWidgetVisibleAndClickable(widget)) {
            return Long.MIN_VALUE;
        }
        Rectangle bounds = widget.getBounds();
        if (bounds == null) {
            return Long.MIN_VALUE;
        }
        String corpus = normalizeWidgetCorpus(widget);
        long keywordBonus = 0L;
        for (String keyword : SHOP_CLOSE_KEYWORDS) {
            if (corpus.contains(keyword)) {
                keywordBonus = 1_000_000L;
                break;
            }
        }
        if (shopFrameBounds == null) {
            long areaPenalty = (long) bounds.width * (long) bounds.height;
            long rightEdge = bounds.x + bounds.width;
            long topBias = -bounds.y;
            return keywordBonus + (rightEdge * 100L) + (topBias * 10L) - areaPenalty;
        }
        int widgetRight = bounds.x + bounds.width;
        int frameRight = shopFrameBounds.x + shopFrameBounds.width;
        int widgetTop = bounds.y;
        int rightDelta = Math.abs(frameRight - widgetRight);
        int topDelta = Math.abs(shopFrameBounds.y - widgetTop);
        long cornerPenalty = ((long) rightDelta * 1400L) + ((long) topDelta * 1200L);
        long areaPenalty = (long) bounds.width * (long) bounds.height;
        return keywordBonus - cornerPenalty - areaPenalty;
    }

    private static String normalizeWidgetCorpus(Widget widget) {
        if (widget == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        appendText(out, widget.getText());
        appendText(out, widget.getName());
        String[] actions = widget.getActions();
        if (actions != null) {
            for (String action : actions) {
                appendText(out, action);
            }
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private static void appendText(StringBuilder out, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (out.length() > 0) {
            out.append(' ');
        }
        out.append(normalizeText(value));
    }

    private boolean clickFirstVisibleWidget(int[] widgetIds, ClickMotionSettings motion) {
        if (widgetIds == null || widgetIds.length == 0) {
            return false;
        }
        for (int widgetId : widgetIds) {
            Widget widget = host.widgetByPackedId(widgetId);
            Point center = resolveWidgetCenter(widget);
            Point targetPoint = resolveWidgetClickPoint(widget, center, true);
            if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
                continue;
            }
            if (!host.waitForMotorActionReady(host.menuActionReadyWaitMaxMs())) {
                return false;
            }
            if (!host.focusClientWindowAndCanvas(false, false)) {
                return false;
            }
            if (!host.clickCanvasPoint(targetPoint, motion)) {
                continue;
            }
            lastSwitcherClickPoint = new Point(targetPoint);
            return true;
        }
        return false;
    }

    private Widget resolveTargetWorldWidget(int targetWorld, int currentWorld) {
        List<Widget> candidates = new ArrayList<>();
        Set<Integer> seenWidgetIds = new HashSet<>();
        collectWorldWidgets(host.widgetByPackedId(InterfaceID.Worldswitcher.BUTTONS), candidates, seenWidgetIds);
        collectWorldWidgets(host.widgetByPackedId(InterfaceID.Worldswitcher.LIST), candidates, seenWidgetIds);
        collectWorldWidgets(host.widgetByPackedId(InterfaceID.Worldswitcher.SCROLLAREA), candidates, seenWidgetIds);
        collectWorldWidgets(host.widgetByPackedId(InterfaceID.WorldswitcherOptions.BUTTONS), candidates, seenWidgetIds);
        collectWorldWidgets(host.widgetByPackedId(InterfaceID.WorldswitcherOptions.LIST), candidates, seenWidgetIds);
        collectWorldWidgets(host.widgetByPackedId(InterfaceID.WorldswitcherOptions.SCROLLAREA), candidates, seenWidgetIds);

        Widget best = null;
        long bestScore = Long.MIN_VALUE;
        for (Widget widget : candidates) {
            int widgetWorld = extractWorldFromWidget(widget);
            if (widgetWorld <= 0) {
                continue;
            }
            if (targetWorld > 0 && widgetWorld != targetWorld) {
                continue;
            }
            if (targetWorld <= 0 && currentWorld > 0 && widgetWorld == currentWorld) {
                continue;
            }
            long score = widgetScore(widget);
            if (score > bestScore) {
                bestScore = score;
                best = widget;
            }
        }
        return best;
    }

    private void collectWorldWidgets(Widget root, List<Widget> out, Set<Integer> seenWidgetIds) {
        if (root == null || root.isHidden()) {
            return;
        }
        int id = root.getId();
        if (id != -1) {
            if (seenWidgetIds.contains(id)) {
                return;
            }
            seenWidgetIds.add(id);
        }
        if (extractWorldFromWidget(root) > 0 && isWidgetVisibleAndClickable(root)) {
            out.add(root);
        }
        Widget[] dynamic = root.getDynamicChildren();
        if (dynamic != null) {
            for (Widget child : dynamic) {
                collectWorldWidgets(child, out, seenWidgetIds);
            }
        }
        Widget[] children = root.getChildren();
        if (children != null) {
            for (Widget child : children) {
                collectWorldWidgets(child, out, seenWidgetIds);
            }
        }
    }

    private Point resolveWidgetCenter(Widget widget) {
        if (!isWidgetVisibleAndClickable(widget)) {
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

    private Point resolveWidgetClickPoint(Widget widget, Point center, boolean switcherButton) {
        Point lastPoint = switcherButton ? lastSwitcherClickPoint : lastWorldEntryClickPoint;
        return resolveWidgetClickPoint(widget, center, lastPoint);
    }

    private Point resolveWidgetClickPoint(Widget widget, Point center, Point lastPoint) {
        Rectangle bounds = widget == null ? null : widget.getBounds();
        if (bounds == null || bounds.width <= 1 || bounds.height <= 1) {
            return null;
        }
        return RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            bounds,
            host::isUsableCanvasPoint,
            lastPoint,
            WORLD_HOP_CLICK_REPEAT_EXCLUSION_PX,
            WORLD_HOP_WIDGET_POINT_INSET_PX,
            WORLD_HOP_WIDGET_POINT_ATTEMPTS,
            center
        );
    }

    private static long widgetScore(Widget widget) {
        if (!isWidgetVisibleAndClickable(widget)) {
            return Long.MIN_VALUE;
        }
        Rectangle bounds = widget.getBounds();
        long area = (long) bounds.width * (long) bounds.height;
        String text = normalizeText(widget.getText());
        long textBonus = text.isEmpty() ? 0L : 100L;
        return area + textBonus;
    }

    private static boolean isWidgetVisibleAndClickable(Widget widget) {
        if (widget == null || widget.isHidden()) {
            return false;
        }
        Rectangle bounds = widget.getBounds();
        return bounds != null && bounds.width > 1 && bounds.height > 1;
    }

    private static int extractWorldFromWidget(Widget widget) {
        if (widget == null) {
            return -1;
        }
        int fromText = extractWorldFromText(widget.getText());
        if (fromText > 0) {
            return fromText;
        }
        return extractWorldFromText(widget.getName());
    }

    private static int extractWorldFromText(String rawText) {
        String normalized = normalizeText(rawText);
        if (normalized.isEmpty()) {
            return -1;
        }
        Matcher matcher = WORLD_ID_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return -1;
        }
        try {
            int world = Integer.parseInt(matcher.group(1));
            return world > 0 ? world : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static String normalizeText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        return rawText.replaceAll("<[^>]*>", " ").replace('\u00A0', ' ').trim();
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
