package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Locale;
import java.util.Optional;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class LogoutInteractionController {
    private static final double LOGOUT_CLICK_REPEAT_EXCLUSION_PX = 3.0;
    private static final int LOGOUT_WIDGET_POINT_INSET_PX = 2;
    private static final int LOGOUT_WIDGET_POINT_ATTEMPTS = 24;

    enum AttemptStatus {
        ALREADY_LOGGED_OUT,
        ACTION_DISPATCHED,
        FAILED
    }

    interface Host {
        Widget widgetByPackedId(int packedWidgetId);

        Optional<Widget> findVisibleWidgetByKeywords(String... keywords);

        Optional<Point> centerOfWidget(Widget widget);

        boolean isUsableCanvasPoint(Point point);

        boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas);

        boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion);

        void emitLogoutEvent(String reason, JsonObject details);

        JsonObject details(Object... kvPairs);

        GameState currentGameState();
    }

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
    private static final int[] LOGOUT_BUTTON_WIDGET_IDS = {
        InterfaceID.Logout.LOGOUT,
        InterfaceID.Logout.LOGOUT_GRAPHIC2,
        InterfaceID.Logout.LOGOUT_GRAPHIC1,
        InterfaceID.Logout.LOGOUT_GRAPHIC0,
        InterfaceID.Logout.LOGOUT_TEXT3,
        InterfaceID.Logout.LOGOUT_BUTTONS
    };
    private static final String[] LOGOUT_FOLLOW_UP_WIDGET_KEYWORDS = {
        "click here to logout",
        "click here to log out",
        "log out"
    };

    private final Host host;
    private Point lastLogoutClickPoint = null;

    LogoutInteractionController(Host host) {
        this.host = host;
    }

    AttemptStatus requestLogoutAttempt() {
        GameState gameState = host.currentGameState();
        if (gameState != GameState.LOGGED_IN && gameState != GameState.LOGGING_IN) {
            emit(
                "logout_noop_not_logged_in",
                host.details("gameState", safeString(nameOf(gameState)))
            );
            return AttemptStatus.ALREADY_LOGGED_OUT;
        }

        // Step 1: if the follow-up logout confirmation is visible, click it directly.
        if (dispatchKeywordWidgetClick(LOGOUT_FOLLOW_UP_WIDGET_KEYWORDS, "logout_follow_up")) {
            emit(
                "logout_follow_up_click_dispatched",
                host.details("keywords", joinKeywords(LOGOUT_FOLLOW_UP_WIDGET_KEYWORDS))
            );
            return AttemptStatus.ACTION_DISPATCHED;
        }

        // Step 2: if the logout panel button is visible, click it.
        int logoutButtonWidgetId = dispatchFirstWidgetClick(LOGOUT_BUTTON_WIDGET_IDS, "logout_button");
        if (logoutButtonWidgetId > 0) {
            emit(
                "logout_button_click_dispatched",
                host.details("logoutButtonWidgetId", logoutButtonWidgetId)
            );
            return AttemptStatus.ACTION_DISPATCHED;
        }

        // Step 3: ensure logout tab is opened so the button becomes visible next tick.
        int logoutTabWidgetId = dispatchFirstWidgetClick(LOGOUT_TAB_WIDGET_IDS, "logout_tab");
        if (logoutTabWidgetId > 0) {
            emit(
                "logout_tab_click_dispatched",
                host.details("logoutTabWidgetId", logoutTabWidgetId)
            );
            return AttemptStatus.ACTION_DISPATCHED;
        }

        emit(
            "logout_click_failed",
            host.details(
                "failure", "logout_widgets_unavailable",
                "gameState", safeString(nameOf(gameState)),
                "logoutFollowUpKeywords", joinKeywords(LOGOUT_FOLLOW_UP_WIDGET_KEYWORDS),
                "logoutTabWidgetIds", joinWidgetIds(LOGOUT_TAB_WIDGET_IDS),
                "logoutButtonWidgetIds", joinWidgetIds(LOGOUT_BUTTON_WIDGET_IDS)
            )
        );
        return AttemptStatus.FAILED;
    }

    private int dispatchFirstWidgetClick(int[] packedWidgetIds, String context) {
        if (packedWidgetIds == null || packedWidgetIds.length == 0) {
            return -1;
        }
        for (int packedWidgetId : packedWidgetIds) {
            if (dispatchWidgetClick(packedWidgetId, context)) {
                return packedWidgetId;
            }
        }
        return -1;
    }

    private boolean dispatchWidgetClick(int packedWidgetId, String context) {
        Widget widget = host.widgetByPackedId(packedWidgetId);
        return dispatchWidgetClick(widget, packedWidgetId, context);
    }

    private boolean dispatchKeywordWidgetClick(String[] keywords, String context) {
        if (keywords == null || keywords.length == 0) {
            return false;
        }
        Optional<Widget> widgetOpt = host.findVisibleWidgetByKeywords(keywords);
        if (widgetOpt.isEmpty()) {
            return false;
        }
        Widget widget = widgetOpt.get();
        int widgetId = widget == null ? -1 : widget.getId();
        return dispatchWidgetClick(widget, widgetId, context);
    }

    private boolean dispatchWidgetClick(Widget widget, int widgetId, String context) {
        Optional<Point> centerOpt = host.centerOfWidget(widget);
        if (centerOpt.isEmpty()) {
            return false;
        }
        Point center = centerOpt.get();
        Point targetPoint = resolveWidgetClickPoint(widget, center);
        if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
            emit(
                "logout_click_failed",
                host.details(
                    "context", safeString(context),
                    "failure", "point_outside_canvas",
                    "widgetId", widgetId,
                    "canvasX", center.x,
                    "canvasY", center.y
                )
            );
            return false;
        }
        if (!host.focusClientWindowAndCanvas(false, false)) {
            emit(
                "logout_click_failed",
                host.details(
                    "context", safeString(context),
                    "failure", "focus_client_failed",
                    "widgetId", widgetId
                )
            );
            return false;
        }
        ClickMotionSettings motion = MotionProfile.GENERIC_INTERACT.resolveClickSettings(null);
        if (!host.clickCanvasPoint(targetPoint, motion)) {
            emit(
                "logout_click_failed",
                host.details(
                    "context", safeString(context),
                    "failure", "click_dispatch_failed",
                    "widgetId", widgetId,
                    "canvasX", targetPoint.x,
                    "canvasY", targetPoint.y
                )
            );
            return false;
        }
        lastLogoutClickPoint = new Point(targetPoint);
        return true;
    }

    private Point resolveWidgetClickPoint(Widget widget, Point center) {
        Rectangle bounds = widget == null ? null : widget.getBounds();
        if (bounds == null || bounds.width <= 1 || bounds.height <= 1) {
            return null;
        }
        return RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            bounds,
            host::isUsableCanvasPoint,
            lastLogoutClickPoint,
            LOGOUT_CLICK_REPEAT_EXCLUSION_PX,
            LOGOUT_WIDGET_POINT_INSET_PX,
            LOGOUT_WIDGET_POINT_ATTEMPTS,
            center
        );
    }

    private void emit(String reason, JsonObject details) {
        host.emitLogoutEvent(reason, details);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String nameOf(GameState gameState) {
        return gameState == null ? "" : gameState.name().toLowerCase(Locale.ROOT);
    }

    private static String joinWidgetIds(int[] packedWidgetIds) {
        if (packedWidgetIds == null || packedWidgetIds.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packedWidgetIds.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(packedWidgetIds[i]);
        }
        return sb.toString();
    }

    private static String joinKeywords(String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keywords.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(safeString(keywords[i]));
        }
        return sb.toString();
    }
}
