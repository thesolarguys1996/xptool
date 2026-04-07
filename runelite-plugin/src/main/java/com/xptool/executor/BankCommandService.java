package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Canvas;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class BankCommandService {
    interface Host {
        CommandExecutor.CommandDecision accept(String reason, JsonObject details);

        CommandExecutor.CommandDecision reject(String reason);

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        boolean isBankOpen();

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        Optional<TileObject> resolveOpenBankTarget(JsonObject payload);

        Point resolveBankObjectClickPoint(TileObject targetObject);

        boolean isUsableCanvasPoint(Point point);

        void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point);

        MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile);

        MotorProfile buildBankMoveAndClickProfile(ClickMotionSettings motion, TileObject targetObject);

        void incrementClicksDispatched();

        Optional<Integer> findVisibleBankItemSlot(int itemId);

        Optional<Integer> findBankItemSlot(int itemId);

        Widget resolveBankItemSlotWidget(int slot);

        Client client();

        Optional<Point> slotCenter(Widget container, int slot);

        Optional<CommandExecutor.CommandDecision> prepareBankWidgetHover(
            boolean inventorySlot,
            int slot,
            Point targetPoint,
            MotionProfile motionProfile,
            String reasonPrefix
        );

        int chooseWidgetOpByKeywordPriority(Widget widget, String... preferredKeywords);

        boolean tryConsumeWorkBudget();

        boolean humanizedBankWidgetActionsEnabled();

        long bankMotorReadyWaitMaxMs();

        boolean waitForMotorActionReady(long maxWaitMs);

        boolean tryHumanizedBankWidgetAction(Point targetPoint, String... optionKeywords);

        boolean typeWithdrawQuantity(String quantityRaw);

        String summarizeWidgetActions(Widget widget);

        Optional<Integer> findInventorySlot(int itemId);

        Optional<Point> resolveInventorySlotPoint(int slot);

        Widget resolveInventorySlotWidget(int slot);

        Set<Integer> parseExcludeItemIds(JsonElement element);

        Optional<Integer> findFirstInventoryItemNotIn(Set<Integer> excludeItemIds);

        void copyMotionFields(JsonObject source, JsonObject destination);

        Robot getOrCreateRobot();

        void sleepQuietly(long ms);

        long randomBetween(long minInclusive, long maxInclusive);

        long bankSearchKeyMinDelayMs();

        long bankSearchKeyMaxDelayMs();

        void typeBankSearchChar(Robot robot, char ch);

        boolean isBankPinPromptVisible();

        Optional<Point> centerOfWidget(Widget widget);

        boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion);

        ClickMotionSettings genericInteractClickSettings();
    }

    private final Host host;

    BankCommandService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision executeOpenBank(JsonObject payload, MotionProfile motionProfile) {
        ClickMotionSettings motion = host.resolveClickMotion(payload, motionProfile);
        if (host.isBankOpen()) {
            return host.accept("bank_already_open", null);
        }
        int targetWorldX = asInt(payload == null ? null : payload.get("targetWorldX"), -1);
        int targetWorldY = asInt(payload == null ? null : payload.get("targetWorldY"), -1);
        boolean hasPreferredTarget = targetWorldX > 0 && targetWorldY > 0;
        Optional<TileObject> targetObject = host.resolveOpenBankTarget(payload);
        if (targetObject.isEmpty()) {
            return host.reject("bank_target_point_unavailable");
        }
        Point targetCanvas = host.resolveBankObjectClickPoint(targetObject.get());
        if (targetCanvas == null || !host.isUsableCanvasPoint(targetCanvas)) {
            return host.reject("bank_click_point_unavailable");
        }
        host.rememberInteractionAnchorForTileObject(targetObject.get(), targetCanvas);

        MotorHandle handle = host.scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(targetCanvas),
            MotorGestureType.MOVE_AND_CLICK,
            host.buildBankMoveAndClickProfile(motion, targetObject.get())
        );
        if (handle.status == MotorGestureStatus.COMPLETE) {
            host.incrementClicksDispatched();
            return host.accept(
                "open_bank_booth_visual_click_dispatched",
                host.details(
                    "target", "bank_booth_or_chest",
                    "openMode", "left_click",
                    "objectId", targetObject.get().getId(),
                    "preferredWorldTarget", hasPreferredTarget,
                    "targetWorldX", targetWorldX,
                    "targetWorldY", targetWorldY,
                    "motorGestureId", handle.id
                )
            );
        }
        if (handle.status == MotorGestureStatus.FAILED || handle.status == MotorGestureStatus.CANCELLED) {
            return host.reject("bank_motor_gesture_" + host.safeString(handle.reason));
        }
        return host.accept(
            "bank_motor_gesture_in_flight",
            host.details(
                "target", "bank_booth_or_chest",
                "objectId", targetObject.get().getId(),
                "motorGestureId", handle.id,
                "motorStatus", handle.status.name(),
                "motorReason", handle.reason
            )
        );
    }

    CommandExecutor.CommandDecision executeWithdrawItem(JsonObject payload, MotionProfile motionProfile) {
        Optional<CommandExecutor.CommandDecision> bankOpenDecision =
            ensureBankOpenForItemLoop(payload, motionProfile, "withdraw");
        if (bankOpenDecision.isPresent()) {
            return bankOpenDecision.get();
        }

        int itemId = asInt(payload == null ? null : payload.get("itemId"), -1);
        String quantity = asString(payload == null ? null : payload.get("quantity"));
        if (itemId <= 0) {
            return host.reject("invalid_item_id");
        }
        Optional<Integer> visibleSlotOpt = host.findVisibleBankItemSlot(itemId);
        if (visibleSlotOpt.isEmpty()) {
            Optional<Integer> containerSlotOpt = host.findBankItemSlot(itemId);
            if (containerSlotOpt.isPresent()) {
                return host.reject("item_present_but_not_visible_in_bank");
            }
            return host.reject("item_not_visible_in_bank");
        }
        int slot = visibleSlotOpt.get();

        Widget bankSlotWidget = host.resolveBankItemSlotWidget(slot);
        if (bankSlotWidget == null) {
            return host.reject("bank_slot_widget_unavailable");
        }
        Optional<Point> targetPoint = host.slotCenter(host.client().getWidget(InterfaceID.Bankmain.ITEMS), slot);
        if (targetPoint.isEmpty()) {
            return host.reject("bank_slot_point_unavailable");
        }
        Optional<CommandExecutor.CommandDecision> hoverDecision = host.prepareBankWidgetHover(
            false,
            slot,
            targetPoint.get(),
            motionProfile,
            "bank_withdraw"
        );
        if (hoverDecision.isPresent()) {
            return hoverDecision.get();
        }
        String q = quantity == null ? "" : quantity.trim().toUpperCase(Locale.ROOT);
        int opId;
        boolean usedWithdrawX = false;
        if ("ALL".equals(q)) {
            opId = host.chooseWidgetOpByKeywordPriority(bankSlotWidget, "withdraw-all", "withdraw all");
        } else if ("1".equals(q) || q.isEmpty()) {
            opId = host.chooseWidgetOpByKeywordPriority(bankSlotWidget, "withdraw-1", "withdraw 1", "withdraw");
        } else {
            opId = host.chooseWidgetOpByKeywordPriority(bankSlotWidget, "withdraw-" + q, "withdraw " + q);
            if (opId <= 0) {
                opId = host.chooseWidgetOpByKeywordPriority(bankSlotWidget, "withdraw-x", "withdraw x");
                usedWithdrawX = opId > 0;
            }
        }
        if (opId <= 0) {
            return host.reject("withdraw_widget_op_unavailable");
        }
        if (!host.tryConsumeWorkBudget()) {
            return host.accept(
                "withdraw_dispatch_budget_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", quantity)
            );
        }
        if (host.humanizedBankWidgetActionsEnabled() && !host.waitForMotorActionReady(host.bankMotorReadyWaitMaxMs())) {
            return host.accept(
                "withdraw_context_menu_cooldown_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", quantity)
            );
        }
        boolean usedHumanizedContextMenu = host.tryHumanizedBankWidgetAction(
            targetPoint.get(),
            withdrawMenuOptionKeywords(q, usedWithdrawX)
        );
        if (!usedHumanizedContextMenu) {
            return host.accept(
                "withdraw_context_menu_action_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", quantity)
            );
        }
        if (usedWithdrawX && !host.typeWithdrawQuantity(q)) {
            return host.reject("withdraw_x_quantity_type_failed");
        }
        return host.accept(
            "withdraw_dispatched",
            host.details(
                "itemId", itemId,
                "slot", slot,
                "quantity", quantity,
                "mode", usedHumanizedContextMenu ? "context_menu" : "visual_widget_op",
                "widgetActions", host.summarizeWidgetActions(bankSlotWidget)
            )
        );
    }

    CommandExecutor.CommandDecision executeWithdrawItemSafeWithdrawAll(JsonObject payload, MotionProfile motionProfile) {
        Optional<CommandExecutor.CommandDecision> bankOpenDecision =
            ensureBankOpenForItemLoop(payload, motionProfile, "withdraw_all");
        if (bankOpenDecision.isPresent()) {
            return bankOpenDecision.get();
        }
        int itemId = asInt(payload == null ? null : payload.get("itemId"), -1);
        if (itemId <= 0) {
            return host.reject("invalid_item_id");
        }

        Optional<Integer> visibleSlotOpt = host.findVisibleBankItemSlot(itemId);
        if (visibleSlotOpt.isEmpty()) {
            Optional<Integer> containerSlotOpt = host.findBankItemSlot(itemId);
            if (containerSlotOpt.isPresent()) {
                return host.reject("item_present_but_not_visible_in_bank");
            }
            return host.reject("item_not_visible_in_bank");
        }
        int slot = visibleSlotOpt.get();
        Widget bankSlotWidget = host.resolveBankItemSlotWidget(slot);
        if (bankSlotWidget == null) {
            return host.reject("bank_slot_widget_unavailable");
        }
        Optional<Point> targetPoint = host.slotCenter(host.client().getWidget(InterfaceID.Bankmain.ITEMS), slot);
        if (targetPoint.isEmpty()) {
            return host.reject("bank_slot_point_unavailable");
        }
        Optional<CommandExecutor.CommandDecision> hoverDecision = host.prepareBankWidgetHover(
            false,
            slot,
            targetPoint.get(),
            motionProfile,
            "bank_withdraw_all"
        );
        if (hoverDecision.isPresent()) {
            return hoverDecision.get();
        }
        int opId = host.chooseWidgetOpByKeywordPriority(bankSlotWidget, "withdraw-all", "withdraw all");
        if (opId <= 0) {
            return host.reject("withdraw_all_widget_op_unavailable");
        }
        if (!host.tryConsumeWorkBudget()) {
            return host.accept(
                "withdraw_all_dispatch_budget_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", "ALL")
            );
        }
        if (host.humanizedBankWidgetActionsEnabled() && !host.waitForMotorActionReady(host.bankMotorReadyWaitMaxMs())) {
            return host.accept(
                "withdraw_all_context_menu_cooldown_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", "ALL")
            );
        }
        boolean usedHumanizedContextMenu = host.tryHumanizedBankWidgetAction(
            targetPoint.get(),
            "withdraw-all",
            "withdraw all"
        );
        if (!usedHumanizedContextMenu) {
            return host.accept(
                "withdraw_all_context_menu_action_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", "ALL")
            );
        }
        return host.accept(
            "withdraw_all_dispatched",
            host.details(
                "itemId", itemId,
                "slot", slot,
                "quantity", "ALL",
                "mode", usedHumanizedContextMenu ? "context_menu" : "visual_widget_op"
            )
        );
    }

    CommandExecutor.CommandDecision executeSearchBankItem(JsonObject payload) {
        if (!host.isBankOpen()) {
            return host.reject("bank_not_open");
        }
        String query = asString(payload == null ? null : payload.get("query")).trim();
        if (query.isEmpty()) {
            return host.reject("missing_search_query");
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return host.reject("robot_unavailable");
        }

        Client client = host.client();
        Canvas canvas = client == null ? null : client.getCanvas();
        if (canvas != null) {
            Window window = SwingUtilities.getWindowAncestor(canvas);
            if (window != null) {
                window.toFront();
                window.requestFocus();
                host.sleepQuietly(10L);
            }
            canvas.requestFocus();
            canvas.requestFocusInWindow();
            host.sleepQuietly(10L);
        }

        robot.keyPress(KeyEvent.VK_CONTROL);
        host.sleepQuietly(8L);
        robot.keyPress(KeyEvent.VK_F);
        host.sleepQuietly(8L);
        robot.keyRelease(KeyEvent.VK_F);
        host.sleepQuietly(8L);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        host.sleepQuietly(24L);

        robot.keyPress(KeyEvent.VK_CONTROL);
        host.sleepQuietly(8L);
        robot.keyPress(KeyEvent.VK_A);
        host.sleepQuietly(8L);
        robot.keyRelease(KeyEvent.VK_A);
        host.sleepQuietly(8L);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        host.sleepQuietly(16L);
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        host.sleepQuietly(8L);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);
        host.sleepQuietly(16L);

        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            host.typeBankSearchChar(robot, ch);
            host.sleepQuietly(host.randomBetween(host.bankSearchKeyMinDelayMs(), host.bankSearchKeyMaxDelayMs()));
            if (ch == ' ' && i + 1 < query.length()) {
                host.sleepQuietly(host.randomBetween(120L, 240L));
            }
        }

        return host.accept("bank_search_dispatched", host.details("query", query));
    }

    CommandExecutor.CommandDecision executeDepositItem(JsonObject payload, MotionProfile motionProfile) {
        Optional<CommandExecutor.CommandDecision> bankOpenDecision =
            ensureBankOpenForItemLoop(payload, motionProfile, "deposit");
        if (bankOpenDecision.isPresent()) {
            return bankOpenDecision.get();
        }
        int itemId = asInt(payload == null ? null : payload.get("itemId"), -1);
        String quantity = asString(payload == null ? null : payload.get("quantity"));
        if (itemId <= 0) {
            return host.reject("invalid_item_id");
        }
        Optional<Integer> slotOpt = host.findInventorySlot(itemId);
        if (slotOpt.isEmpty()) {
            return host.accept("deposit_item_not_present", host.details("itemId", itemId));
        }
        int slot = slotOpt.get();

        Optional<Point> slotPoint = host.resolveInventorySlotPoint(slot);
        if (slotPoint.isEmpty()) {
            return host.reject("inventory_slot_point_unavailable");
        }
        Optional<CommandExecutor.CommandDecision> hoverDecision = host.prepareBankWidgetHover(
            true,
            slot,
            slotPoint.get(),
            motionProfile,
            "inventory_deposit"
        );
        if (hoverDecision.isPresent()) {
            return hoverDecision.get();
        }
        Widget invSlotWidget = host.resolveInventorySlotWidget(slot);
        if (invSlotWidget == null) {
            return host.reject("inventory_slot_widget_unavailable");
        }
        String q = quantity == null ? "" : quantity.trim().toUpperCase(Locale.ROOT);
        int opId;
        if ("ALL".equals(q)) {
            opId = host.chooseWidgetOpByKeywordPriority(invSlotWidget, "deposit-all", "deposit all");
        } else if ("1".equals(q) || q.isEmpty()) {
            opId = host.chooseWidgetOpByKeywordPriority(invSlotWidget, "deposit-1", "deposit 1", "deposit");
        } else {
            opId = host.chooseWidgetOpByKeywordPriority(invSlotWidget, "deposit-" + q, "deposit " + q);
            if (opId <= 0) {
                opId = host.chooseWidgetOpByKeywordPriority(invSlotWidget, "deposit-x", "deposit x");
            }
        }
        if (opId <= 0) {
            return host.reject("deposit_widget_op_unavailable");
        }
        if (!host.tryConsumeWorkBudget()) {
            return host.accept(
                "deposit_dispatch_budget_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", quantity)
            );
        }
        if (host.humanizedBankWidgetActionsEnabled() && !host.waitForMotorActionReady(host.bankMotorReadyWaitMaxMs())) {
            return host.accept(
                "deposit_context_menu_cooldown_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", quantity)
            );
        }
        boolean usedHumanizedContextMenu = host.tryHumanizedBankWidgetAction(
            slotPoint.get(),
            depositMenuOptionKeywords(q)
        );
        if (!usedHumanizedContextMenu) {
            return host.accept(
                "deposit_context_menu_action_deferred",
                host.details("itemId", itemId, "slot", slot, "quantity", quantity)
            );
        }
        return host.accept(
            "deposit_item_dispatched",
            host.details(
                "itemId", itemId,
                "slot", slot,
                "quantity", quantity,
                "mode", usedHumanizedContextMenu ? "context_menu" : "visual_widget_op",
                "widgetActions", host.summarizeWidgetActions(invSlotWidget)
            )
        );
    }

    CommandExecutor.CommandDecision executeDepositAllExcept(JsonObject payload, MotionProfile motionProfile) {
        if (!host.isBankOpen()) {
            return host.reject("bank_not_open");
        }
        Set<Integer> exclude = host.parseExcludeItemIds(payload == null ? null : payload.get("excludeItemIds"));
        Optional<Integer> firstDepositable = host.findFirstInventoryItemNotIn(exclude);
        if (firstDepositable.isEmpty()) {
            return host.accept(
                "deposit_all_except_complete",
                host.details("excludeItemIds", exclude.toString())
            );
        }

        JsonObject delegatePayload = new JsonObject();
        delegatePayload.addProperty("itemId", firstDepositable.get());
        delegatePayload.addProperty("quantity", "ALL");
        host.copyMotionFields(payload, delegatePayload);
        CommandExecutor.CommandDecision delegated = executeDepositItem(delegatePayload, motionProfile);
        if (!delegated.isAccepted()) {
            return delegated;
        }
        return host.accept(
            "deposit_all_except_dispatched",
            host.details(
                "excludeItemIds", exclude.toString(),
                "depositedItemId", firstDepositable.get()
            )
        );
    }

    JsonObject normalizeDepositAllExceptPayload(JsonObject payload) {
        JsonObject out = payload == null ? new JsonObject() : payload.deepCopy();
        if (!out.has("excludeItemIds") && out.has("excludeItemId")) {
            out.add("excludeItemIds", out.get("excludeItemId"));
        }
        return out;
    }

    CommandExecutor.CommandDecision executeEnterBankPin(JsonObject payload) {
        String pinRaw = asString(payload == null ? null : payload.get("pin"));
        String pin = pinRaw.replaceAll("\\D", "");
        if (pin.length() != 4) {
            return host.reject("invalid_bank_pin");
        }
        if (!host.isBankPinPromptVisible()) {
            return host.accept("bank_pin_prompt_not_visible", null);
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return host.reject("robot_unavailable");
        }

        Client client = host.client();
        Canvas canvas = client == null ? null : client.getCanvas();
        if (canvas != null) {
            Window window = SwingUtilities.getWindowAncestor(canvas);
            if (window != null) {
                window.toFront();
                window.requestFocus();
                host.sleepQuietly(12L);
            }
            canvas.requestFocus();
            canvas.requestFocusInWindow();
            host.sleepQuietly(12L);
        }

        for (int i = 0; i < pin.length(); i++) {
            int keyCode = KeyEvent.VK_0 + (pin.charAt(i) - '0');
            robot.keyPress(keyCode);
            host.sleepQuietly(10L);
            robot.keyRelease(keyCode);
            host.sleepQuietly(26L);
        }

        return host.accept("bank_pin_entered", host.details("digitsEntered", pin.length()));
    }

    CommandExecutor.CommandDecision executeCloseBank(MotionProfile motionProfile) {
        if (!host.isBankOpen()) {
            return host.accept("bank_already_closed", null);
        }
        Robot robot = host.getOrCreateRobot();
        if (robot != null) {
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            return host.accept("close_bank_dispatched", host.details("method", "escape"));
        }
        Client client = host.client();
        Widget bankContainer = client == null ? null : client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if (bankContainer == null) {
            return host.reject("bank_widget_unavailable");
        }
        Optional<Point> center = host.centerOfWidget(bankContainer);
        if (center.isEmpty() || !host.clickCanvasPoint(center.get(), host.genericInteractClickSettings())) {
            return host.reject("close_bank_click_failed");
        }
        return host.accept("close_bank_dispatched", host.details("method", "widget_click"));
    }

    private Optional<CommandExecutor.CommandDecision> ensureBankOpenForItemLoop(
        JsonObject payload,
        MotionProfile motionProfile,
        String reasonPrefix
    ) {
        if (host.isBankOpen()) {
            return Optional.empty();
        }
        JsonObject openPayload = payload == null ? new JsonObject() : payload.deepCopy();
        MotionProfile profile = motionProfile == null ? MotionProfile.BANK : motionProfile;
        CommandExecutor.CommandDecision openDecision = executeOpenBank(openPayload, profile);
        if (!openDecision.isAccepted()) {
            return Optional.of(host.reject("bank_auto_open_failed:" + host.safeString(openDecision.getReason())));
        }
        JsonObject info = host.details("openReason", openDecision.getReason());
        if (openDecision.getDetails() != null) {
            info.add("openDetails", openDecision.getDetails().deepCopy());
        }
        return Optional.of(host.accept(reasonPrefix + "_open_bank_deferred", info));
    }

    private static String[] withdrawMenuOptionKeywords(String quantityUpper, boolean usedWithdrawX) {
        String q = safeUpper(quantityUpper);
        if ("ALL".equals(q)) {
            return new String[]{"withdraw-all", "withdraw all"};
        }
        if ("1".equals(q) || q.isEmpty()) {
            return new String[]{"withdraw-1", "withdraw 1", "withdraw"};
        }
        if (usedWithdrawX) {
            return new String[]{"withdraw-x", "withdraw x"};
        }
        return new String[]{"withdraw-" + q, "withdraw " + q, "withdraw"};
    }

    private static String[] depositMenuOptionKeywords(String quantityUpper) {
        String q = safeUpper(quantityUpper);
        if ("ALL".equals(q)) {
            return new String[]{"deposit-all", "deposit all"};
        }
        if ("1".equals(q) || q.isEmpty()) {
            return new String[]{"deposit-1", "deposit 1", "deposit"};
        }
        return new String[]{"deposit-" + q, "deposit " + q, "deposit-x", "deposit x"};
    }

    private static String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
