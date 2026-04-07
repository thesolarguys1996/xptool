package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Robot;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.WorldView;
import net.runelite.api.widgets.Widget;

final class ExecutorLoginInteractionHostFactory {
    @FunctionalInterface
    interface FocusClientWindowAndCanvas {
        boolean execute(boolean focusWindow, boolean focusCanvas);
    }

    private ExecutorLoginInteractionHostFactory() {
    }

    static LoginSubmitTargetPlanner.Host createLoginSubmitPlannerHost(
        IntSupplier canvasWidth,
        IntSupplier canvasHeight,
        Predicate<Point> isUsableCanvasPoint
    ) {
        return new LoginSubmitTargetPlanner.Host() {
            @Override
            public int canvasWidth() {
                return canvasWidth.getAsInt();
            }

            @Override
            public int canvasHeight() {
                return canvasHeight.getAsInt();
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }
        };
    }

    static MenuEntryTargetMatcher.Host createMenuEntryTargetMatcherHost(
        Supplier<MenuEntry[]> currentMenuEntries,
        IntFunction<WorldView> resolveWorldViewById
    ) {
        return new MenuEntryTargetMatcher.Host() {
            @Override
            public MenuEntry[] currentMenuEntries() {
                return currentMenuEntries.get();
            }

            @Override
            public WorldView resolveWorldViewById(int worldViewId) {
                return resolveWorldViewById.apply(worldViewId);
            }
        };
    }

    static LoginInteractionController.Host createLoginInteractionControllerHost(
        Function<String[], Optional<Widget>> findVisibleWidgetByKeywords,
        Function<Widget, Optional<Point>> centerOfWidget,
        Predicate<Point> isUsableCanvasPoint,
        FocusClientWindowAndCanvas focusClientWindowAndCanvas,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        BiConsumer<String, JsonObject> emitLoginEvent,
        Function<Object[], JsonObject> details,
        Supplier<GameState> currentGameState,
        IntSupplier canvasWidth,
        IntSupplier canvasHeight,
        Supplier<Robot> getOrCreateRobot,
        LongConsumer sleepQuietly,
        Runnable noteMotorAction
    ) {
        return new LoginInteractionController.Host() {
            @Override
            public Optional<Widget> findVisibleWidgetByKeywords(String... keywords) {
                return findVisibleWidgetByKeywords.apply(keywords);
            }

            @Override
            public Optional<Point> centerOfWidget(Widget widget) {
                return centerOfWidget.apply(widget);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas) {
                return focusClientWindowAndCanvas.execute(focusWindow, focusCanvas);
            }

            @Override
            public boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
                return clickCanvasPoint.apply(canvasPoint, motion);
            }

            @Override
            public void emitLoginEvent(String reason, JsonObject detailsJson) {
                emitLoginEvent.accept(reason, detailsJson);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public GameState currentGameState() {
                return currentGameState.get();
            }

            @Override
            public int canvasWidth() {
                return canvasWidth.getAsInt();
            }

            @Override
            public int canvasHeight() {
                return canvasHeight.getAsInt();
            }

            @Override
            public Robot getOrCreateRobot() {
                return getOrCreateRobot.get();
            }

            @Override
            public void sleepQuietly(long ms) {
                sleepQuietly.accept(ms);
            }

            @Override
            public void noteMotorAction() {
                noteMotorAction.run();
            }
        };
    }

    static LogoutInteractionController.Host createLogoutInteractionControllerHost(
        IntFunction<Widget> widgetByPackedId,
        Function<String[], Optional<Widget>> findVisibleWidgetByKeywords,
        Function<Widget, Optional<Point>> centerOfWidget,
        Predicate<Point> isUsableCanvasPoint,
        FocusClientWindowAndCanvas focusClientWindowAndCanvas,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        BiConsumer<String, JsonObject> emitLogoutEvent,
        Function<Object[], JsonObject> details,
        Supplier<GameState> currentGameState
    ) {
        return new LogoutInteractionController.Host() {
            @Override
            public Widget widgetByPackedId(int packedWidgetId) {
                return widgetByPackedId.apply(packedWidgetId);
            }

            @Override
            public Optional<Widget> findVisibleWidgetByKeywords(String... keywords) {
                return findVisibleWidgetByKeywords.apply(keywords);
            }

            @Override
            public Optional<Point> centerOfWidget(Widget widget) {
                return centerOfWidget.apply(widget);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas) {
                return focusClientWindowAndCanvas.execute(focusWindow, focusCanvas);
            }

            @Override
            public boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
                return clickCanvasPoint.apply(canvasPoint, motion);
            }

            @Override
            public void emitLogoutEvent(String reason, JsonObject detailsJson) {
                emitLogoutEvent.accept(reason, detailsJson);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public GameState currentGameState() {
                return currentGameState.get();
            }
        };
    }
}
