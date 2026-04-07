package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.Optional;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;

final class LoginInteractionController {
    private static final double LOGIN_CLICK_REPEAT_EXCLUSION_PX = 3.0;
    private static final int LOGIN_WIDGET_POINT_INSET_PX = 2;
    private static final int LOGIN_WIDGET_POINT_ATTEMPTS = 24;
    private static final int LOGIN_SUBMIT_POINT_INSET_PX = 3;
    private static final int LOGIN_SUBMIT_POINT_ATTEMPTS = 28;

    interface Host {
        Optional<Widget> findVisibleWidgetByKeywords(String... keywords);

        Optional<Point> centerOfWidget(Widget widget);

        boolean isUsableCanvasPoint(Point point);

        boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas);

        boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion);

        void emitLoginEvent(String reason, JsonObject details);

        JsonObject details(Object... kvPairs);

        GameState currentGameState();

        int canvasWidth();

        int canvasHeight();

        Robot getOrCreateRobot();

        void sleepQuietly(long ms);

        void noteMotorAction();
    }

    private final Host host;
    private final LoginSubmitStagePlanner submitStagePlanner;
    private static final String[] PRIMARY_SUBMIT_WIDGET_KEYWORDS = {
        "play now",
        "log in",
        "existing user",
        "new user"
    };
    private static final String[] SECONDARY_SUBMIT_WIDGET_KEYWORDS = {
        "click here to play",
        "click here to continue"
    };
    private static final Rectangle PRIMARY_SUBMIT_BASE_REGION = new Rectangle(270, 198, 225, 72);
    private static final Rectangle SECONDARY_SUBMIT_BASE_REGION = new Rectangle(285, 304, 195, 66);
    private static final int LOGIN_CANVAS_BASE_WIDTH = 765;
    private static final int LOGIN_CANVAS_BASE_HEIGHT = 503;
    private Point lastWidgetClickPoint = null;
    private Point lastPrimarySubmitClickPoint = null;
    private Point lastSecondarySubmitClickPoint = null;

    LoginInteractionController(
        Host host,
        LoginSubmitStagePlanner submitStagePlanner
    ) {
        this.host = host;
        this.submitStagePlanner = submitStagePlanner;
    }

    boolean focusLoginFieldByKeywords(String... keywords) {
        return clickVisibleLoginWidgetWithContext("focus_field", keywords);
    }

    boolean isPrimaryLoginSubmitPromptVisible() {
        return host.findVisibleWidgetByKeywords(PRIMARY_SUBMIT_WIDGET_KEYWORDS).isPresent();
    }

    boolean isSecondaryLoginSubmitPromptVisible() {
        return host.findVisibleWidgetByKeywords(SECONDARY_SUBMIT_WIDGET_KEYWORDS).isPresent();
    }

    boolean submitLoginAttempt() {
        return clickLoginSubmitRegionTarget();
    }

    boolean clickVisibleLoginWidget(String... keywords) {
        return clickVisibleLoginWidgetWithContext("generic", keywords);
    }

    boolean pressLoginKeyChord(int keyCode, boolean holdShift, int holdMs) {
        if (keyCode <= 0) {
            return false;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        if (!host.focusClientWindowAndCanvas(false, false)) {
            return false;
        }
        int keyHoldMs = Math.max(8, holdMs);
        try {
            if (holdShift) {
                robot.keyPress(KeyEvent.VK_SHIFT);
                host.sleepQuietly(6L);
            }
            robot.keyPress(keyCode);
            host.sleepQuietly(keyHoldMs);
            robot.keyRelease(keyCode);
            if (holdShift) {
                host.sleepQuietly(4L);
                robot.keyRelease(KeyEvent.VK_SHIFT);
            }
            host.noteMotorAction();
            return true;
        } catch (Exception ignored) {
            if (holdShift) {
                try {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                } catch (Exception ignoredShiftRelease) {
                    // Best effort release.
                }
            }
            return false;
        }
    }

    private boolean clickVisibleLoginWidgetWithContext(String context, String... keywords) {
        String resolvedContext = safeString(context).isBlank() ? "generic" : safeString(context).trim();
        if (keywords == null || keywords.length == 0) {
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", resolvedContext,
                    "failure", "missing_keywords"
                )
            );
            return false;
        }
        Optional<Widget> widgetOpt = host.findVisibleWidgetByKeywords(keywords);
        if (widgetOpt.isEmpty()) {
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", resolvedContext,
                    "failure", "no_widget_match",
                    "keywords", LoginWidgetHeuristics.joinKeywords(keywords),
                    "gameState", safeString(nameOf(host.currentGameState()))
                )
            );
            return false;
        }
        Widget widget = widgetOpt.get();
        Optional<Point> centerOpt = host.centerOfWidget(widget);
        if (centerOpt.isEmpty()) {
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", resolvedContext,
                    "failure", "widget_no_center",
                    "keywords", LoginWidgetHeuristics.joinKeywords(keywords),
                    "widgetBounds", String.valueOf(widget.getBounds())
                )
            );
            return false;
        }
        Point center = centerOpt.get();
        Point targetPoint = resolveWidgetClickPoint(widget, center);
        if (targetPoint == null || !host.isUsableCanvasPoint(targetPoint)) {
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", resolvedContext,
                    "failure", "point_outside_canvas",
                    "keywords", LoginWidgetHeuristics.joinKeywords(keywords),
                    "canvasX", center.x,
                    "canvasY", center.y,
                    "canvasWidth", host.canvasWidth(),
                    "canvasHeight", host.canvasHeight(),
                    "widgetBounds", String.valueOf(widget.getBounds())
                )
            );
            return false;
        }
        if (!host.focusClientWindowAndCanvas(false, false)) {
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", resolvedContext,
                    "failure", "focus_client_failed",
                    "keywords", LoginWidgetHeuristics.joinKeywords(keywords)
                )
            );
            return false;
        }
        ClickMotionSettings motion = MotionProfile.GENERIC_INTERACT.resolveClickSettings(null);
        boolean clicked = host.clickCanvasPoint(targetPoint, motion);
        if (!clicked) {
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", resolvedContext,
                    "failure", "click_dispatch_failed",
                    "keywords", LoginWidgetHeuristics.joinKeywords(keywords),
                    "canvasX", targetPoint.x,
                    "canvasY", targetPoint.y
                )
            );
            return false;
        }
        lastWidgetClickPoint = new Point(targetPoint);
        emitLoginClickDebug(
            "login_widget_click_dispatched",
            host.details(
                "context", resolvedContext,
                "keywords", LoginWidgetHeuristics.joinKeywords(keywords),
                "canvasX", targetPoint.x,
                "canvasY", targetPoint.y,
                "widgetBounds", String.valueOf(widget.getBounds())
            )
        );
        return true;
    }

    private boolean clickLoginSubmitRegionTarget() {
        GameState gameState = host.currentGameState();
        if (gameState != GameState.LOGIN_SCREEN
            && gameState != GameState.LOGGING_IN
            && gameState != GameState.LOGGED_IN) {
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", "submit_region",
                    "failure", "unsupported_game_state",
                    "gameState", safeString(nameOf(gameState))
                )
            );
            return false;
        }
        long nowMs = System.currentTimeMillis();
        boolean primaryPromptVisible = isPrimaryLoginSubmitPromptVisible();
        boolean secondaryPromptVisible = isSecondaryLoginSubmitPromptVisible();
        LoginSubmitStagePlanner.Stage stage = submitStagePlanner.chooseStage(
            primaryPromptVisible,
            secondaryPromptVisible,
            nowMs
        );
        ClickMotionSettings motion = MotionProfile.GENERIC_INTERACT.resolveClickSettings(null);
        Point candidate = resolveAnchoredSubmitPoint(stage);
        if (candidate == null) {
            String context = stage == LoginSubmitStagePlanner.Stage.SECONDARY
                ? "submit_button_secondary"
                : "submit_button_primary";
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", context,
                    "failure", "submit_region_point_unusable",
                    "stage", stage.name().toLowerCase(Locale.ROOT),
                    "primaryPromptVisible", primaryPromptVisible,
                    "secondaryPromptVisible", secondaryPromptVisible
                )
            );
            return false;
        }
        if (!host.focusClientWindowAndCanvas(false, false)) {
            String context = stage == LoginSubmitStagePlanner.Stage.SECONDARY
                ? "submit_button_secondary"
                : "submit_button_primary";
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", context,
                    "failure", "focus_client_failed",
                    "stage", stage.name().toLowerCase(Locale.ROOT),
                    "primaryPromptVisible", primaryPromptVisible,
                    "secondaryPromptVisible", secondaryPromptVisible
                )
            );
            return false;
        }
        boolean clicked = host.clickCanvasPoint(candidate, motion);
        if (!clicked) {
            String context = stage == LoginSubmitStagePlanner.Stage.SECONDARY
                ? "submit_button_secondary"
                : "submit_button_primary";
            emitLoginClickDebug(
                "login_widget_click_failed",
                host.details(
                    "context", context,
                    "failure", "click_dispatch_failed",
                    "canvasX", candidate.x,
                    "canvasY", candidate.y,
                    "stage", stage.name().toLowerCase(Locale.ROOT),
                    "primaryPromptVisible", primaryPromptVisible,
                    "secondaryPromptVisible", secondaryPromptVisible
                )
            );
            return false;
        }
        noteSubmitClickPoint(stage, candidate);
        submitStagePlanner.noteDispatched(stage, nowMs);
        Rectangle buttonRegion = scaledLoginRegion(
            stage == LoginSubmitStagePlanner.Stage.SECONDARY
                ? SECONDARY_SUBMIT_BASE_REGION
                : PRIMARY_SUBMIT_BASE_REGION
        );
        String context = stage == LoginSubmitStagePlanner.Stage.SECONDARY
            ? "submit_button_secondary"
            : "submit_button_primary";
        emitLoginClickDebug(
            "login_widget_click_dispatched",
            host.details(
                "context", context,
                "canvasX", candidate.x,
                "canvasY", candidate.y,
                "stage", stage.name().toLowerCase(Locale.ROOT),
                "primaryPromptVisible", primaryPromptVisible,
                "secondaryPromptVisible", secondaryPromptVisible,
                "buttonRegion", String.valueOf(buttonRegion),
                "targetingMode", "sampled_anchor"
            )
        );
        return true;
    }

    private Point resolveAnchoredSubmitPoint(LoginSubmitStagePlanner.Stage stage) {
        Rectangle region = scaledLoginRegion(
            stage == LoginSubmitStagePlanner.Stage.SECONDARY
                ? SECONDARY_SUBMIT_BASE_REGION
                : PRIMARY_SUBMIT_BASE_REGION
        );
        if (region == null || region.width <= 1 || region.height <= 1) {
            return null;
        }
        Point center = new Point(
            (int) Math.round(region.getCenterX()),
            (int) Math.round(region.getCenterY())
        );
        Point recent = stage == LoginSubmitStagePlanner.Stage.SECONDARY
            ? lastSecondarySubmitClickPoint
            : lastPrimarySubmitClickPoint;
        return RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            region,
            host::isUsableCanvasPoint,
            recent,
            LOGIN_CLICK_REPEAT_EXCLUSION_PX,
            LOGIN_SUBMIT_POINT_INSET_PX,
            LOGIN_SUBMIT_POINT_ATTEMPTS,
            center
        );
    }

    private void noteSubmitClickPoint(LoginSubmitStagePlanner.Stage stage, Point point) {
        if (point == null) {
            return;
        }
        if (stage == LoginSubmitStagePlanner.Stage.SECONDARY) {
            lastSecondarySubmitClickPoint = new Point(point);
            return;
        }
        lastPrimarySubmitClickPoint = new Point(point);
    }

    private Point resolveWidgetClickPoint(Widget widget, Point center) {
        Rectangle bounds = widget == null ? null : widget.getBounds();
        if (bounds == null || bounds.width <= 1 || bounds.height <= 1) {
            return null;
        }
        return RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            bounds,
            host::isUsableCanvasPoint,
            lastWidgetClickPoint,
            LOGIN_CLICK_REPEAT_EXCLUSION_PX,
            LOGIN_WIDGET_POINT_INSET_PX,
            LOGIN_WIDGET_POINT_ATTEMPTS,
            center
        );
    }

    private Rectangle scaledLoginRegion(Rectangle baseRegion) {
        if (baseRegion == null) {
            return new Rectangle();
        }
        int canvasWidth = Math.max(1, host.canvasWidth());
        int canvasHeight = Math.max(1, host.canvasHeight());
        double xScale = canvasWidth / (double) LOGIN_CANVAS_BASE_WIDTH;
        double yScale = canvasHeight / (double) LOGIN_CANVAS_BASE_HEIGHT;
        int x = (int) Math.round(baseRegion.x * xScale);
        int y = (int) Math.round(baseRegion.y * yScale);
        int width = Math.max(1, (int) Math.round(baseRegion.width * xScale));
        int height = Math.max(1, (int) Math.round(baseRegion.height * yScale));
        return new Rectangle(x, y, width, height);
    }

    private void emitLoginClickDebug(String reason, JsonObject details) {
        host.emitLoginEvent(reason, details);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String nameOf(GameState gameState) {
        return gameState == null ? "" : safeString(gameState.name());
    }
}
