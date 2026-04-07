package com.xptool.executor;

final class ExecutionEventLogPolicy {
    private ExecutionEventLogPolicy() {
    }

    static boolean shouldLogExecutionStatus(String status, String reason, boolean verboseExecutionLogs) {
        if ("failed".equals(status)) {
            return true;
        }
        if ("executed".equals(status)) {
            return shouldLogExecutedReason(reason);
        }
        if (verboseExecutionLogs) {
            return true;
        }
        return "executor_config_updated".equals(reason);
    }

    static boolean shouldLogExecutedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return true;
        }
        if ("drop_item_menu_action_dispatched".equals(reason) || "drop_item_dispatched".equals(reason)) {
            return false;
        }
        if ("selected_tree_target_unavailable".equals(reason)) {
            return false;
        }
        if ("selected_rock_target_unavailable".equals(reason)) {
            return false;
        }
        if (reason.startsWith("woodcut_drop_")) {
            return false;
        }
        return true;
    }
}
