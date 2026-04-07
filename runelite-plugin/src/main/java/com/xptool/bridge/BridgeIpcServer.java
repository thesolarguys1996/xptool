package com.xptool.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BridgeIpcServer {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeIpcServer.class);
    private static final Gson GSON = new Gson();
    private static final String AUTH_HEADER = "X-XPTool-Bridge-Token";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    private final BridgeAgentConfig config;
    private final BridgeRuntime runtime;
    private final BridgeHeartbeatService heartbeatService;
    private final BridgeDispatchConfigService dispatchConfigService;
    private HttpServer server;
    private ExecutorService executor;

    BridgeIpcServer(
        BridgeAgentConfig config,
        BridgeRuntime runtime,
        BridgeHeartbeatService heartbeatService,
        BridgeDispatchSettings dispatchSettings
    ) {
        this.config = config;
        this.runtime = runtime;
        this.heartbeatService = heartbeatService;
        this.dispatchConfigService = new BridgeDispatchConfigService(dispatchSettings);
    }

    synchronized void start() {
        if (server != null) {
            return;
        }
        if (!config.isLoopbackBindAddress()) {
            LOG.error(
                "xptool.bridge ipc_start_blocked_non_loopback bindAddress={} reason=loopback_required",
                config.bindAddress()
            );
            return;
        }
        if (!config.hasAuthToken()) {
            LOG.error("xptool.bridge ipc_start_blocked_missing_auth_token reason=auth_token_required");
            return;
        }
        try {
            InetAddress bindAddress = InetAddress.getByName(config.bindAddress());
            InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, config.ipcPort());
            server = HttpServer.create(socketAddress, 0);
            server.createContext("/v1/ping", this::handlePing);
            server.createContext("/v1/state/heartbeat", this::handleHeartbeat);
            server.createContext("/v1/config/dispatch", this::handleDispatchConfig);
            executor = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "xptool-bridge-ipc");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.start();
            LOG.info(
                "xptool.bridge ipc_started bindAddress={} port={} authRequired={}",
                config.bindAddress(),
                config.ipcPort(),
                !config.authToken().isEmpty()
            );
        } catch (IOException ex) {
            LOG.error("xptool.bridge ipc_start_failed", ex);
        }
    }

    private void handlePing(HttpExchange exchange) throws IOException {
        try {
            if (!validateRequest(exchange, "GET")) {
                return;
            }
            JsonObject payload = new JsonObject();
            payload.addProperty("ok", true);
            payload.addProperty("bridgeReady", runtime.isBridgeReady());
            payload.addProperty("runtimeStartedAtUnixMs", runtime.runtimeStartedAtUnixMs());
            JsonObject dispatchSnapshot = dispatchConfigService.snapshot();
            payload.addProperty("bridgeRuntimeEnabled", dispatchSnapshot.get("bridgeRuntimeEnabled").getAsBoolean());
            payload.addProperty("liveDispatchEnabled", dispatchSnapshot.get("liveDispatchEnabled").getAsBoolean());
            sendJson(exchange, 200, payload);
        } catch (Exception ex) {
            sendFailure(exchange, 500, "internal_error");
        }
    }

    private void handleHeartbeat(HttpExchange exchange) throws IOException {
        try {
            if (!validateRequest(exchange, "GET")) {
                return;
            }
            sendJson(exchange, 200, heartbeatService.heartbeat());
        } catch (Exception ex) {
            sendFailure(exchange, 500, "internal_error");
        }
    }

    private void handleDispatchConfig(HttpExchange exchange) throws IOException {
        try {
            if (!validateRequest(exchange, "GET", "POST")) {
                return;
            }
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, dispatchConfigService.snapshot());
                return;
            }
            JsonObject request = readJsonObject(exchange);
            JsonObject response = dispatchConfigService.apply(request);
            sendJson(exchange, 200, response);
        } catch (IllegalArgumentException ex) {
            sendFailure(exchange, 400, safeReason(ex.getMessage(), "invalid_request"));
        } catch (RuntimeException ex) {
            sendFailure(exchange, 400, "invalid_request_payload");
        } catch (Exception ex) {
            sendFailure(exchange, 500, "internal_error");
        }
    }

    private boolean validateRequest(HttpExchange exchange, String... allowedMethods) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        boolean methodAllowed = false;
        for (String allowedMethod : allowedMethods) {
            if (allowedMethod != null && allowedMethod.equalsIgnoreCase(requestMethod)) {
                methodAllowed = true;
                break;
            }
        }
        if (!methodAllowed) {
            sendFailure(exchange, 405, "method_not_allowed");
            return false;
        }
        if (!isAuthorized(exchange)) {
            sendFailure(exchange, 401, "unauthorized");
            return false;
        }
        return true;
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String configuredToken = config.authToken();
        if (configuredToken == null || configuredToken.isBlank()) {
            return true;
        }
        String provided = exchange.getRequestHeaders().getFirst(AUTH_HEADER);
        if (provided == null || provided.isBlank()) {
            return false;
        }
        byte[] expectedBytes = configuredToken.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = provided.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private void sendFailure(HttpExchange exchange, int statusCode, String reason) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", false);
        payload.addProperty("reason", reason);
        sendJson(exchange, statusCode, payload);
    }

    private static JsonObject readJsonObject(HttpExchange exchange) throws IOException {
        String body = readBodyUtf8(exchange);
        if (body.isBlank()) {
            return new JsonObject();
        }
        JsonElement parsed = new JsonParser().parse(body);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("invalid_request_json");
        }
        return parsed.getAsJsonObject();
    }

    private static String readBodyUtf8(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            byte[] bytes = input.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String safeReason(String reason, String fallback) {
        if (reason == null) {
            return fallback;
        }
        String normalized = reason.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static void sendJson(HttpExchange exchange, int statusCode, JsonObject payload) throws IOException {
        byte[] responseBytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_JSON);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
        } finally {
            exchange.close();
        }
    }
}
