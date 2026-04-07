package com.xptool.executor;

final class ActivityIdleCadenceWindow {
    private final int actedMinIntervalTicks;
    private final int actedMaxIntervalTicks;
    private final int retryMinIntervalTicks;
    private final int retryMaxIntervalTicks;

    private ActivityIdleCadenceWindow(
        int actedMinIntervalTicks,
        int actedMaxIntervalTicks,
        int retryMinIntervalTicks,
        int retryMaxIntervalTicks
    ) {
        this.actedMinIntervalTicks = Math.max(1, actedMinIntervalTicks);
        this.actedMaxIntervalTicks = Math.max(this.actedMinIntervalTicks, actedMaxIntervalTicks);
        this.retryMinIntervalTicks = Math.max(1, retryMinIntervalTicks);
        this.retryMaxIntervalTicks = Math.max(this.retryMinIntervalTicks, retryMaxIntervalTicks);
    }

    static ActivityIdleCadenceWindow of(
        int actedMinIntervalTicks,
        int actedMaxIntervalTicks,
        int retryMinIntervalTicks,
        int retryMaxIntervalTicks
    ) {
        return new ActivityIdleCadenceWindow(
            actedMinIntervalTicks,
            actedMaxIntervalTicks,
            retryMinIntervalTicks,
            retryMaxIntervalTicks
        );
    }

    int actedMinIntervalTicks() {
        return actedMinIntervalTicks;
    }

    int actedMaxIntervalTicks() {
        return actedMaxIntervalTicks;
    }

    int retryMinIntervalTicks() {
        return retryMinIntervalTicks;
    }

    int retryMaxIntervalTicks() {
        return retryMaxIntervalTicks;
    }
}
