package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdleCursorTargetPlannerTest {
    @Test
    void woodcutOffscreenPlannerSamplesAcrossCurrentMonitor() {
        TestHost host = new TestHost(IdleSkillContext.WOODCUTTING);
        IdleCursorTargetPlanner planner = new IdleCursorTargetPlanner(host);
        boolean[] sideSeen = new boolean[4];
        Set<String> uniqueTargets = new HashSet<>();

        for (int i = 0; i < 120; i++) {
            Optional<Point> targetOpt = planner.resolveIdleOffscreenTargetScreenPoint();
            assertTrue(targetOpt.isPresent());
            Point target = targetOpt.get();
            assertTrue(host.monitorBounds.contains(target));
            assertFalse(host.windowBounds.contains(target));
            markSides(sideSeen, target, host.windowBounds);
            uniqueTargets.add(target.x + ":" + target.y);
        }

        assertTrue(uniqueTargets.size() >= 40);
        assertTrue(sideSeen[0]);
        assertTrue(sideSeen[1]);
        assertTrue(sideSeen[2]);
        assertTrue(sideSeen[3]);
        assertTrue(host.reasons.contains("idle_offscreen_woodcut_monitor_wide_search_start"));
        assertTrue(host.reasons.contains("idle_offscreen_woodcut_monitor_wide_candidate_selected"));
    }

    @Test
    void fishingOffscreenPlannerDoesNotEmitWoodcutMonitorWideEvents() {
        TestHost host = new TestHost(IdleSkillContext.FISHING);
        IdleCursorTargetPlanner planner = new IdleCursorTargetPlanner(host);

        Optional<Point> targetOpt = planner.resolveIdleOffscreenTargetScreenPoint();
        assertTrue(targetOpt.isPresent());
        assertFalse(host.reasons.contains("idle_offscreen_woodcut_monitor_wide_search_start"));
        assertFalse(host.reasons.contains("idle_offscreen_woodcut_monitor_wide_candidate_selected"));
    }

    private static void markSides(boolean[] sideSeen, Point point, Rectangle windowBounds) {
        if (point == null || windowBounds == null || sideSeen == null || sideSeen.length < 4) {
            return;
        }
        int windowMinX = windowBounds.x;
        int windowMaxX = windowBounds.x + Math.max(0, windowBounds.width - 1);
        int windowMinY = windowBounds.y;
        int windowMaxY = windowBounds.y + Math.max(0, windowBounds.height - 1);
        if (point.x < windowMinX) {
            sideSeen[0] = true;
        }
        if (point.x > windowMaxX) {
            sideSeen[1] = true;
        }
        if (point.y < windowMinY) {
            sideSeen[2] = true;
        }
        if (point.y > windowMaxY) {
            sideSeen[3] = true;
        }
    }

    private static final class TestHost implements IdleCursorTargetPlanner.Host {
        private final IdleSkillContext context;
        private final Rectangle monitorBounds = new Rectangle(0, 0, 1920, 1080);
        private final Rectangle windowBounds = new Rectangle(560, 240, 800, 600);
        private final List<String> reasons = new ArrayList<>();

        private TestHost(IdleSkillContext context) {
            this.context = context == null ? IdleSkillContext.GLOBAL : context;
        }

        @Override
        public Optional<Rectangle> resolveInventoryInteractionRegionCanvas() {
            return Optional.empty();
        }

        @Override
        public Optional<Point> randomCanvasPointInRegion(Rectangle region, int insetPx) {
            return Optional.empty();
        }

        @Override
        public boolean isUsableCanvasPoint(Point point) {
            if (point == null) {
                return false;
            }
            return point.x >= 0 && point.y >= 0 && point.x < 800 && point.y < 600;
        }

        @Override
        public int canvasWidth() {
            return 800;
        }

        @Override
        public int canvasHeight() {
            return 600;
        }

        @Override
        public Point currentMouseCanvasPoint() {
            return new Point(400, 300);
        }

        @Override
        public Optional<Rectangle> resolveClientCanvasBoundsScreen() {
            return Optional.of(new Rectangle(windowBounds));
        }

        @Override
        public Optional<Rectangle> resolveClientWindowBoundsScreen() {
            return Optional.of(new Rectangle(windowBounds));
        }

        @Override
        public Optional<Rectangle> resolveScreenBoundsForPoint(Point point) {
            return Optional.of(new Rectangle(monitorBounds));
        }

        @Override
        public Point currentPointerLocationOr(Point fallback) {
            return fallback == null ? new Point(windowBounds.x + (windowBounds.width / 2), windowBounds.y + (windowBounds.height / 2)) : fallback;
        }

        @Override
        public IdleSkillContext resolveIdleSkillContext() {
            return context;
        }

        @Override
        public IdleCadenceTuning activeIdleCadenceTuning() {
            return IdleCadenceTuning.none();
        }

        @Override
        public void emitIdleEvent(String reason, JsonObject details) {
            reasons.add(reason == null ? "" : reason);
        }
    }
}
