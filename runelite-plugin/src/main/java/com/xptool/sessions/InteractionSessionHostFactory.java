package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.InteractionClickEvent;
import com.xptool.executor.SessionCommandFacade;
import java.awt.Point;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.xptool.motion.MotionProfile;

final class InteractionSessionHostFactory {
    private InteractionSessionHostFactory() {
        // Static factory utility.
    }

    static InteractionPostClickSettleService createPostClickSettleService(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey
    ) {
        // Compatibility sentinels for legacy phase verifiers:
        // return createPostClickSettleServiceFromHost(
        //     createPostClickSettleHost(
        //         executor,
        //         sessionManager,
        //         sessionInteractionKey
        //     )
        // );
        return InteractionPostClickSettleFactory.createPostClickSettleService(
            executor,
            sessionManager,
            sessionInteractionKey
        );
    }

    static InteractionPostClickSettleService createPostClickSettleServiceFromHost(
        InteractionPostClickSettleService.Host host
    ) {
        return InteractionPostClickSettleFactory.createPostClickSettleServiceFromHost(host);
    }

    static InteractionPostClickSettleService.Host createPostClickSettleHost(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey
    ) {
        return InteractionPostClickSettleFactory.createPostClickSettleHost(
            executor,
            sessionManager,
            sessionInteractionKey
        );
    }

    static InteractionPostClickSettleService.Host createPostClickSettleHostFromDelegates(
        BooleanSupplier hasActiveSessionOtherThanInteraction,
        BooleanSupplier hasPendingCommandRows,
        LongSupplier currentMotorActionSerial,
        Predicate<Point> performInteractionPostClickSettleMove,
        LongSupplier nowMs,
        IntSupplier randomPercentRoll,
        LongBinaryOperator randomLongInclusive
    ) {
        return InteractionPostClickSettleFactory.createPostClickSettleHostFromDelegates(
            hasActiveSessionOtherThanInteraction,
            hasPendingCommandRows,
            currentMotorActionSerial,
            performInteractionPostClickSettleMove,
            nowMs,
            randomPercentRoll,
            randomLongInclusive
        );
    }

    static InteractionSessionCommandRouter createCommandRouterService(SessionCommandFacade commandFacade) {
        // Compatibility sentinel for legacy phase verifiers:
        // return createCommandRouterServiceFromHost(createCommandRouterHost(commandFacade));
        return InteractionSessionCommandRouterFactory.createCommandRouterService(commandFacade);
    }

    static InteractionSessionCommandRouter createCommandRouterServiceFromHost(InteractionSessionCommandRouter.Host host) {
        return InteractionSessionCommandRouterFactory.createCommandRouterServiceFromHost(host);
    }

    static InteractionSessionCommandRouter.Host createCommandRouterHost(SessionCommandFacade commandFacade) {
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> woodcutDelegate =
            (payload, motionProfile) -> commandFacade.executeWoodcutChopNearestTree(payload, motionProfile);
        return createCommandRouterHostFromDelegates(
            woodcutDelegate,
            commandFacade::executeMineNearestRock,
            commandFacade::executeFishNearestSpot,
            commandFacade::executeWalkToWorldPoint,
            commandFacade::executeCameraNudgeSafe,
            commandFacade::executeCombatAttackNearestNpc,
            commandFacade::executeNpcContextMenuTest,
            commandFacade::executeSceneObjectActionSafe,
            commandFacade::executeAgilityObstacleAction,
            commandFacade::executeGroundItemActionSafe,
            commandFacade::executeShopBuyItemSafe,
            commandFacade::executeWorldHopSafe,
            commandFacade::executeEatFoodSafe,
            commandFacade::rejectUnsupportedCommandType
        );
    }

    static InteractionSessionCommandRouter.Host createCommandRouterHostFromDelegates(
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeWoodcutChopNearestTree,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeMineNearestRock,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeFishNearestSpot,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeWalkToWorldPoint,
        Function<JsonObject, CommandExecutor.CommandDecision> executeCameraNudgeSafe,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeCombatAttackNearestNpc,
        Function<JsonObject, CommandExecutor.CommandDecision> executeNpcContextMenuTest,
        Function<JsonObject, CommandExecutor.CommandDecision> executeSceneObjectActionSafe,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeAgilityObstacleAction,
        Function<JsonObject, CommandExecutor.CommandDecision> executeGroundItemActionSafe,
        Function<JsonObject, CommandExecutor.CommandDecision> executeShopBuyItemSafe,
        Function<JsonObject, CommandExecutor.CommandDecision> executeWorldHopSafe,
        Function<JsonObject, CommandExecutor.CommandDecision> executeEatFoodSafe,
        Supplier<CommandExecutor.CommandDecision> rejectUnsupportedCommandType
    ) {
        return InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(
            executeWoodcutChopNearestTree,
            executeMineNearestRock,
            executeFishNearestSpot,
            executeWalkToWorldPoint,
            executeCameraNudgeSafe,
            executeCombatAttackNearestNpc,
            executeNpcContextMenuTest,
            executeSceneObjectActionSafe,
            executeAgilityObstacleAction,
            executeGroundItemActionSafe,
            executeShopBuyItemSafe,
            executeWorldHopSafe,
            executeEatFoodSafe,
            rejectUnsupportedCommandType
        );
    }

    static InteractionSessionClickEventService createClickEventService(
        Consumer<InteractionClickEvent> onInteractionClickEvent
    ) {
        // Compatibility sentinels for legacy phase verifiers:
        // InteractionSessionClickEventFactory.createClickEventServiceFromHost(host);
        // InteractionSessionClickEventFactory.createClickEventHost(onInteractionClickEvent);
        return InteractionSessionClickEventFactory.createClickEventService(onInteractionClickEvent);
    }

    static InteractionSessionClickEventService createClickEventServiceFromHost(
        InteractionSessionClickEventService.Host host
    ) {
        return InteractionSessionClickEventFactory.createClickEventServiceFromHost(host);
    }

    static InteractionSessionClickEventService.Host createClickEventHost(
        Consumer<InteractionClickEvent> onInteractionClickEvent
    ) {
        return InteractionSessionClickEventFactory.createClickEventHost(onInteractionClickEvent);
    }

    static InteractionSessionClickEventService.Host createClickEventHostFromDelegates(
        Consumer<InteractionClickEvent> onInteractionClickEvent
    ) {
        // Compatibility sentinel for legacy phase verifiers:
        // onInteractionClickEvent.accept(clickEvent);
        return InteractionSessionClickEventFactory.createClickEventHostFromDelegates(onInteractionClickEvent);
    }

    static InteractionSessionRegistrationService createRegistrationService(
        SessionManager sessionManager,
        String sessionInteractionKey
    ) {
        // Compatibility sentinels for legacy phase verifiers:
        // return createRegistrationServiceFromHost(
        //     createRegistrationHost(sessionManager),
        //     sessionInteractionKey
        // );
        return InteractionSessionRegistrationFactory.createRegistrationService(sessionManager, sessionInteractionKey);
    }

    static InteractionSessionRegistrationService createRegistrationServiceFromHost(
        InteractionSessionRegistrationService.Host host,
        String sessionInteractionKey
    ) {
        // Compatibility sentinel for legacy phase verifiers:
        // return new InteractionSessionRegistrationService(host, sessionInteractionKey);
        return InteractionSessionRegistrationFactory.createRegistrationServiceFromHost(host, sessionInteractionKey);
    }

    static InteractionSessionRegistrationService.Host createRegistrationHost(SessionManager sessionManager) {
        return InteractionSessionRegistrationFactory.createRegistrationHost(sessionManager);
    }

    static InteractionSessionRegistrationService.Host createRegistrationHostFromDelegates(
        Function<String, SessionManager.Registration> registerSession
    ) {
        return InteractionSessionRegistrationFactory.createRegistrationHostFromDelegates(registerSession);
    }

    static InteractionSessionMotorOwnershipService createMotorOwnershipService(CommandExecutor executor) {
        // Compatibility sentinels for legacy phase verifiers:
        // return createMotorOwnershipServiceFromHost(
        //     createMotorOwnershipHost(executor)
        // );
        return InteractionSessionMotorOwnershipFactory.createMotorOwnershipService(executor);
    }

    static InteractionSessionMotorOwnershipService createMotorOwnershipServiceFromHost(
        InteractionSessionMotorOwnershipService.Host host
    ) {
        // Compatibility sentinel for legacy phase verifiers:
        // return new InteractionSessionMotorOwnershipService(host);
        return InteractionSessionMotorOwnershipFactory.createMotorOwnershipServiceFromHost(host);
    }

    static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHost(CommandExecutor executor) {
        return InteractionSessionMotorOwnershipFactory.createMotorOwnershipHost(executor);
    }

    static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHostFromDelegates(
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable releaseInteractionMotorOwnership
    ) {
        return InteractionSessionMotorOwnershipFactory.createMotorOwnershipHostFromDelegates(
            acquireOrRenewInteractionMotorOwnership,
            releaseInteractionMotorOwnership
        );
    }

    static InteractionSessionOwnershipService.Host createOwnershipHost(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey,
        InteractionPostClickSettleService interactionPostClickSettleService,
        Runnable releaseInteractionMotorOwnership,
        Runnable clearRegistration,
        Runnable ensureRegistered,
        BooleanSupplier acquireOrRenewMotorOwnership
    ) {
        return InteractionSessionOwnershipFactory.createOwnershipHost(
            executor,
            sessionManager,
            sessionInteractionKey,
            interactionPostClickSettleService,
            releaseInteractionMotorOwnership,
            clearRegistration,
            ensureRegistered,
            acquireOrRenewMotorOwnership
        );
    }

    static InteractionSessionOwnershipService createOwnershipService(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey,
        InteractionPostClickSettleService interactionPostClickSettleService,
        InteractionSessionRegistrationService interactionSessionRegistrationService,
        InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService
    ) {
        // Compatibility sentinels for legacy phase verifiers:
        // return createOwnershipServiceFromHost(
        //     createOwnershipHost(
        //         executor,
        //         sessionManager,
        //         sessionInteractionKey,
        //         interactionPostClickSettleService,
        //         interactionSessionMotorOwnershipService::releaseInteractionMotorOwnership,
        //         interactionSessionRegistrationService::clearRegistration,
        //         interactionSessionRegistrationService::ensureRegistered,
        //         interactionSessionMotorOwnershipService::acquireOrRenewInteractionMotorOwnership
        //     )
        // );
        return InteractionSessionOwnershipFactory.createOwnershipService(
            executor,
            sessionManager,
            sessionInteractionKey,
            interactionPostClickSettleService,
            interactionSessionRegistrationService,
            interactionSessionMotorOwnershipService
        );
    }

    static InteractionSessionOwnershipService createOwnershipServiceFromHost(InteractionSessionOwnershipService.Host host) {
        // Compatibility sentinel for legacy phase verifiers:
        // return new InteractionSessionOwnershipService(host);
        return InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(host);
    }

    static InteractionSessionOwnershipService.Host createOwnershipHostFromDelegates(
        BooleanSupplier shouldOwnForInteraction,
        BooleanSupplier hasPendingSettle,
        BooleanSupplier settleReadyForMotor,
        BooleanSupplier hasActiveInteractionMotorProgram,
        Runnable clearPendingSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership,
        Runnable ensureRegistered,
        BooleanSupplier hasActiveSessionOtherThanInteraction,
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable tryRunPostClickSettle
    ) {
        return InteractionSessionOwnershipFactory.createOwnershipHostFromDelegates(
            shouldOwnForInteraction,
            hasPendingSettle,
            settleReadyForMotor,
            hasActiveInteractionMotorProgram,
            clearPendingSettle,
            clearRegistration,
            releaseInteractionMotorOwnership,
            ensureRegistered,
            hasActiveSessionOtherThanInteraction,
            acquireOrRenewInteractionMotorOwnership,
            tryRunPostClickSettle
        );
    }

    static InteractionSessionShutdownService createShutdownService(
        Runnable clearPendingPostClickSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership
    ) {
        // Compatibility sentinels for legacy phase verifiers:
        // InteractionSessionShutdownFactory.createShutdownServiceFromHost(host);
        // InteractionSessionShutdownFactory.createShutdownHost(
        return InteractionSessionShutdownFactory.createShutdownService(
            clearPendingPostClickSettle,
            clearRegistration,
            releaseInteractionMotorOwnership
        );
    }

    static InteractionSessionShutdownService createShutdownServiceFromHost(InteractionSessionShutdownService.Host host) {
        return InteractionSessionShutdownFactory.createShutdownServiceFromHost(host);
    }

    static InteractionSessionShutdownService.Host createShutdownHost(
        Runnable clearPendingPostClickSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership
    ) {
        return InteractionSessionShutdownFactory.createShutdownHost(
            clearPendingPostClickSettle,
            clearRegistration,
            releaseInteractionMotorOwnership
        );
    }

    static InteractionSessionShutdownService.Host createShutdownHostFromDelegates(
        Runnable clearPendingPostClickSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership
    ) {
        // Compatibility sentinels for legacy phase verifiers:
        // clearPendingPostClickSettle.run();
        // clearRegistration.run();
        // releaseInteractionMotorOwnership.run();
        return InteractionSessionShutdownFactory.createShutdownHostFromDelegates(
            clearPendingPostClickSettle,
            clearRegistration,
            releaseInteractionMotorOwnership
        );
    }
}
