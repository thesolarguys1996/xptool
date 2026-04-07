package com.xptool.bridge;

import com.sun.tools.attach.VirtualMachine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class BridgeAgentAttacher {
    private BridgeAgentAttacher() {
        // CLI entrypoint holder.
    }

    public static void main(String[] args) {
        AttachRequest request;
        try {
            request = AttachRequest.parse(args);
        } catch (IllegalArgumentException ex) {
            System.err.println("xptool.bridge attach_invalid_args reason=" + ex.getMessage());
            System.exit(2);
            return;
        }

        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(request.pid());
            vm.loadAgent(request.agentJarPath(), request.agentArgs());
            System.out.println("xptool.bridge attach_success pid=" + request.pid());
        } catch (Exception ex) {
            System.err.println(
                "xptool.bridge attach_failed pid=" + request.pid() + " reason=" + ex.getClass().getSimpleName() + ":" + safe(ex)
            );
            System.exit(1);
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (Exception ignored) {
                    // Best effort detach.
                }
            }
        }
    }

    private static String safe(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return "unknown";
        }
        String message = throwable.getMessage().trim();
        return message.isEmpty() ? "unknown" : message;
    }

    private static final class AttachRequest {
        private final String pid;
        private final String agentJarPath;
        private final String agentArgs;

        private AttachRequest(String pid, String agentJarPath, String agentArgs) {
            this.pid = pid;
            this.agentJarPath = agentJarPath;
            this.agentArgs = agentArgs;
        }

        static AttachRequest parse(String[] args) {
            String pid = "";
            String agentJarPath = "";
            String agentArgs = "";

            for (int index = 0; index < args.length; index++) {
                String current = args[index];
                if ("--pid".equals(current)) {
                    pid = requireValue("--pid", args, ++index);
                } else if ("--agent-jar".equals(current)) {
                    agentJarPath = requireValue("--agent-jar", args, ++index);
                } else if ("--agent-args".equals(current)) {
                    agentArgs = requireValue("--agent-args", args, ++index);
                } else {
                    throw new IllegalArgumentException("unsupported_argument:" + current);
                }
            }

            if (pid.isBlank()) {
                throw new IllegalArgumentException("missing_pid");
            }
            if (agentJarPath.isBlank()) {
                throw new IllegalArgumentException("missing_agent_jar");
            }
            validatePid(pid);
            validateAgentJar(agentJarPath);
            return new AttachRequest(pid, agentJarPath, agentArgs == null ? "" : agentArgs.trim());
        }

        private static String requireValue(String flag, String[] args, int index) {
            if (index >= args.length) {
                throw new IllegalArgumentException("missing_value_for_" + flag.replace("-", ""));
            }
            String value = args[index];
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("blank_value_for_" + flag.replace("-", ""));
            }
            return value.trim();
        }

        private static void validatePid(String pid) {
            try {
                long parsed = Long.parseLong(pid);
                if (parsed <= 0L) {
                    throw new IllegalArgumentException("invalid_pid");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid_pid");
            }
        }

        private static void validateAgentJar(String agentJarPath) {
            Path jarPath = Paths.get(agentJarPath);
            if (!Files.exists(jarPath)) {
                throw new IllegalArgumentException("agent_jar_not_found");
            }
            if (!Files.isRegularFile(jarPath)) {
                throw new IllegalArgumentException("agent_jar_not_file");
            }
        }

        String pid() {
            return pid;
        }

        String agentJarPath() {
            return agentJarPath;
        }

        String agentArgs() {
            return agentArgs;
        }
    }
}
