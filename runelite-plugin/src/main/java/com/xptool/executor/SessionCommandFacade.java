package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SessionCommandFacade {
    private final BankCommandService bankCommandService;
    private final WoodcuttingCommandService woodcuttingCommandService;
    private final MiningCommandService miningCommandService;
    private final FishingCommandService fishingCommandService;
    private final AgilityCommandService agilityCommandService;
    private final WalkCommandService walkCommandService;
    private final CombatCommandService combatCommandService;
    private final Function<JsonObject, CommandExecutor.CommandDecision> npcContextMenuTestHandler;
    private final Function<JsonObject, CommandExecutor.CommandDecision> sceneObjectActionHandler;
    private final Function<JsonObject, CommandExecutor.CommandDecision> groundItemActionHandler;
    private final Function<JsonObject, CommandExecutor.CommandDecision> shopBuyItemHandler;
    private final Function<JsonObject, CommandExecutor.CommandDecision> worldHopHandler;
    private final Function<JsonObject, CommandExecutor.CommandDecision> cameraNudgeSafeHandler;
    private final Function<JsonObject, CommandExecutor.CommandDecision> eatFoodSafeHandler;
    private final Supplier<CommandExecutor.CommandDecision> unsupportedHandler;

    public SessionCommandFacade(
        BankCommandService bankCommandService,
        WoodcuttingCommandService woodcuttingCommandService,
        MiningCommandService miningCommandService,
        FishingCommandService fishingCommandService,
        AgilityCommandService agilityCommandService,
        WalkCommandService walkCommandService,
        CombatCommandService combatCommandService,
        Function<JsonObject, CommandExecutor.CommandDecision> npcContextMenuTestHandler,
        Function<JsonObject, CommandExecutor.CommandDecision> sceneObjectActionHandler,
        Function<JsonObject, CommandExecutor.CommandDecision> groundItemActionHandler,
        Function<JsonObject, CommandExecutor.CommandDecision> shopBuyItemHandler,
        Function<JsonObject, CommandExecutor.CommandDecision> worldHopHandler,
        Function<JsonObject, CommandExecutor.CommandDecision> cameraNudgeSafeHandler,
        Function<JsonObject, CommandExecutor.CommandDecision> eatFoodSafeHandler,
        Supplier<CommandExecutor.CommandDecision> unsupportedHandler
    ) {
        this.bankCommandService = bankCommandService;
        this.woodcuttingCommandService = woodcuttingCommandService;
        this.miningCommandService = miningCommandService;
        this.fishingCommandService = fishingCommandService;
        this.agilityCommandService = agilityCommandService;
        this.walkCommandService = walkCommandService;
        this.combatCommandService = combatCommandService;
        this.npcContextMenuTestHandler = npcContextMenuTestHandler;
        this.sceneObjectActionHandler = sceneObjectActionHandler;
        this.groundItemActionHandler = groundItemActionHandler;
        this.shopBuyItemHandler = shopBuyItemHandler;
        this.worldHopHandler = worldHopHandler;
        this.cameraNudgeSafeHandler = cameraNudgeSafeHandler;
        this.eatFoodSafeHandler = eatFoodSafeHandler;
        this.unsupportedHandler = unsupportedHandler;
    }

    public CommandExecutor.CommandDecision executeOpenBank(JsonObject payload, MotionProfile motionProfile) {
        return bankCommandService.executeOpenBank(payload, motionProfile);
    }

    public CommandExecutor.CommandDecision executeEnterBankPin(JsonObject payload) {
        return bankCommandService.executeEnterBankPin(payload);
    }

    public CommandExecutor.CommandDecision executeSearchBankItem(JsonObject payload) {
        return bankCommandService.executeSearchBankItem(payload);
    }

    public CommandExecutor.CommandDecision executeDepositItem(JsonObject payload, MotionProfile motionProfile) {
        return bankCommandService.executeDepositItem(payload, motionProfile);
    }

    public CommandExecutor.CommandDecision executeDepositAllExcept(JsonObject payload, MotionProfile motionProfile) {
        return bankCommandService.executeDepositAllExcept(payload, motionProfile);
    }

    public JsonObject normalizeDepositAllExceptPayload(JsonObject payload) {
        return bankCommandService.normalizeDepositAllExceptPayload(payload);
    }

    public CommandExecutor.CommandDecision executeWithdrawItem(JsonObject payload, MotionProfile motionProfile) {
        return bankCommandService.executeWithdrawItem(payload, motionProfile);
    }

    public CommandExecutor.CommandDecision executeWithdrawItemSafeWithdrawAll(JsonObject payload, MotionProfile motionProfile) {
        return bankCommandService.executeWithdrawItemSafeWithdrawAll(payload, motionProfile);
    }

    public CommandExecutor.CommandDecision executeCloseBank(MotionProfile motionProfile) {
        return bankCommandService.executeCloseBank(motionProfile);
    }

    public CommandExecutor.CommandDecision executeWoodcutChopNearestTree(JsonObject payload, MotionProfile motionProfile) {
        RuntimeDecision decision = woodcuttingCommandService.executeChopNearestTree(payload, motionProfile);
        return CommandExecutor.CommandDecision.fromRuntimeDecision(decision);
    }

    public CommandExecutor.CommandDecision executeMineNearestRock(JsonObject payload, MotionProfile motionProfile) {
        RuntimeDecision decision = miningCommandService.executeMineNearestRock(payload, motionProfile);
        return CommandExecutor.CommandDecision.fromRuntimeDecision(decision);
    }

    public CommandExecutor.CommandDecision executeFishNearestSpot(JsonObject payload, MotionProfile motionProfile) {
        RuntimeDecision decision = fishingCommandService.executeFishNearestSpot(payload, motionProfile);
        return CommandExecutor.CommandDecision.fromRuntimeDecision(decision);
    }

    public CommandExecutor.CommandDecision executeAgilityObstacleAction(JsonObject payload, MotionProfile motionProfile) {
        return agilityCommandService.executeAgilityObstacleAction(payload, motionProfile);
    }

    public CommandExecutor.CommandDecision executeWalkToWorldPoint(JsonObject payload, MotionProfile motionProfile) {
        return walkCommandService.executeWalkToWorldPoint(payload, motionProfile);
    }

    public CommandExecutor.CommandDecision executeCombatAttackNearestNpc(JsonObject payload, MotionProfile motionProfile) {
        return combatCommandService.executeCombatAttackNearestNpc(payload, motionProfile);
    }

    public CommandExecutor.CommandDecision executeNpcContextMenuTest(JsonObject payload) {
        return npcContextMenuTestHandler.apply(payload);
    }

    public CommandExecutor.CommandDecision executeSceneObjectActionSafe(JsonObject payload) {
        return sceneObjectActionHandler.apply(payload);
    }

    public CommandExecutor.CommandDecision executeGroundItemActionSafe(JsonObject payload) {
        return groundItemActionHandler.apply(payload);
    }

    public CommandExecutor.CommandDecision executeShopBuyItemSafe(JsonObject payload) {
        return shopBuyItemHandler.apply(payload);
    }

    public CommandExecutor.CommandDecision executeWorldHopSafe(JsonObject payload) {
        return worldHopHandler.apply(payload);
    }

    public CommandExecutor.CommandDecision executeCameraNudgeSafe(JsonObject payload) {
        return cameraNudgeSafeHandler.apply(payload);
    }

    public CommandExecutor.CommandDecision executeEatFoodSafe(JsonObject payload) {
        return eatFoodSafeHandler.apply(payload);
    }

    public CommandExecutor.CommandDecision rejectUnsupportedCommandType() {
        return unsupportedHandler.get();
    }
}
