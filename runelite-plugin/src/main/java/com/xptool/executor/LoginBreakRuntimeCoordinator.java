package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.Locale;

final class LoginBreakRuntimeCoordinator {
    interface Host {
        boolean hasManualMetricsRuntimeSignalFor(String consumer, boolean emitWhenMissing);
        BreakRuntimeState breakRuntimeState();
        void breakRuntimeDisarm();
        void maybeEmitManualMetricsRuntimeGateEvent(String consumer, String reason, JsonObject details);
        JsonObject details(Object... kvPairs);
        boolean loginBreakRuntimeEnabled();
        boolean loginBreakRuntimeAutoArm();
        BreakProfile resolveManualMetricsBreakProfile();
        void breakRuntimeArm(BreakProfile profile);
        void breakRuntimeOnGameTick(int tick);
    }

    private final Host host;

    LoginBreakRuntimeCoordinator(Host host) {
        this.host = host;
    }

    void advanceOnGameTick(int tick) {
        if (!host.hasManualMetricsRuntimeSignalFor("break_runtime", host.loginBreakRuntimeEnabled())) {
            if (host.breakRuntimeState() != BreakRuntimeState.DISARMED) {
                host.breakRuntimeDisarm();
                host.maybeEmitManualMetricsRuntimeGateEvent(
                    "break_runtime",
                    "break_disarmed_manual_metrics_signal_missing",
                    host.details("tick", tick, "state", host.breakRuntimeState().name().toLowerCase(Locale.ROOT))
                );
            }
            return;
        }
        if (host.loginBreakRuntimeAutoArm() && host.breakRuntimeState() == BreakRuntimeState.DISARMED) {
            BreakProfile breakProfile = host.resolveManualMetricsBreakProfile();
            if (breakProfile != null) {
                host.breakRuntimeArm(breakProfile);
            } else {
                host.maybeEmitManualMetricsRuntimeGateEvent(
                    "break_runtime",
                    "break_profile_unresolved",
                    host.details("tick", tick)
                );
            }
        }
        host.breakRuntimeOnGameTick(tick);
    }
}

