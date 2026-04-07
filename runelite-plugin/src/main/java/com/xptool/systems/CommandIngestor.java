package com.xptool.systems;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CommandIngestor {
    public interface Host {
        String resolveCommandFilePath();

        int maxLinesPerPoll();

        void onConfigPathUpdated(String configuredPath, boolean exists);

        void onCommandFileAttached();

        void onCommandFileTruncated();

        void onCommandLine(String line);

        void onFailure(String reason);
    }

    private final Host host;
    private final int pollIntervalMs;
    private final String threadName;

    private volatile boolean running = false;
    private Thread thread = null;
    private String lastConfiguredCommandPath = "";
    private String lastCommandPath = "";
    private long readOffsetBytes = 0L;

    public CommandIngestor(Host host, int pollIntervalMs, String threadName) {
        this.host = host;
        this.pollIntervalMs = Math.max(1, pollIntervalMs);
        this.threadName = safeString(threadName).isEmpty() ? "xptool-command-ingest" : threadName;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this::ingestLoop, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        Thread t = thread;
        if (t != null) {
            t.interrupt();
            thread = null;
        }
    }

    private void ingestLoop() {
        while (running) {
            try {
                String configuredPath = host == null ? "" : safeString(host.resolveCommandFilePath());
                handleConfigTransitions(configuredPath);
                int maxLines = host == null ? 0 : Math.max(1, host.maxLinesPerPoll());
                List<String> lines = readNewLines(configuredPath, maxLines);
                for (String line : lines) {
                    if (host != null) {
                        host.onCommandLine(line);
                    }
                }
            } catch (IOException ex) {
                if (host != null) {
                    host.onFailure("command_file_read_error:" + safeString(ex.getMessage()));
                }
            } catch (Exception ex) {
                if (host != null) {
                    host.onFailure("command_ingest_error:" + safeString(ex.getMessage()));
                }
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void handleConfigTransitions(String configuredPath) {
        boolean pathChanged = !safeString(configuredPath).equals(lastConfiguredCommandPath);
        if (!pathChanged) {
            return;
        }
        lastConfiguredCommandPath = safeString(configuredPath);
        if (host != null) {
            File commandFile = configuredPath.isEmpty() ? null : new File(configuredPath);
            host.onConfigPathUpdated(configuredPath, commandFile != null && commandFile.exists());
        }
    }

    private List<String> readNewLines(String commandPath, int maxLines) throws IOException {
        File file = new File(commandPath);
        if (!file.exists()) {
            return List.of();
        }

        if (!commandPath.equals(lastCommandPath)) {
            lastCommandPath = commandPath;
            readOffsetBytes = file.length();
            if (host != null) {
                host.onCommandFileAttached();
            }
            return List.of();
        }

        List<String> out = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long len = raf.length();
            if (len < readOffsetBytes) {
                readOffsetBytes = 0L;
                if (host != null) {
                    host.onCommandFileTruncated();
                }
            }
            raf.seek(readOffsetBytes);
            String line;
            while ((line = raf.readLine()) != null) {
                byte[] latin1 = line.getBytes(StandardCharsets.ISO_8859_1);
                String utf8Line = new String(latin1, StandardCharsets.UTF_8).trim();
                if (!utf8Line.isEmpty()) {
                    out.add(utf8Line);
                }
                if (out.size() >= maxLines) {
                    break;
                }
            }
            readOffsetBytes = raf.getFilePointer();
        }
        return out;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
