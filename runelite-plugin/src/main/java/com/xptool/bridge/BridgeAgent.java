package com.xptool.bridge;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BridgeAgent {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeAgent.class);
    private static final AtomicReference<BridgeRuntime> RUNTIME = new AtomicReference<>();

    private BridgeAgent() {
        // Agent entrypoint holder.
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        startAgent("premain", agentArgs);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        startAgent("agentmain", agentArgs);
    }

    private static void startAgent(String entrypoint, String agentArgs) {
        BridgeAgentConfig config = BridgeAgentConfig.fromRuntime(agentArgs);
        if (!config.enabled()) {
            LOG.info("xptool.bridge agent_disabled entrypoint={}", entrypoint);
            return;
        }
        BridgeSystemPropertyBootstrap.apply(config);

        BridgeRuntime existing = RUNTIME.get();
        if (existing != null) {
            LOG.info("xptool.bridge agent_already_running entrypoint={}", entrypoint);
            return;
        }

        BridgeRuntime runtime = new BridgeRuntime(config);
        if (!RUNTIME.compareAndSet(null, runtime)) {
            LOG.info("xptool.bridge agent_race_detected entrypoint={}", entrypoint);
            return;
        }

        runtime.start();
        LOG.info(
            "xptool.bridge agent_started entrypoint={} bindAddress={} port={} authRequired={}",
            entrypoint,
            config.bindAddress(),
            config.ipcPort(),
            !config.authToken().isEmpty()
        );
    }

}
