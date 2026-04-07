package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import java.awt.Point;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

final class InteractionPostClickSettleFactory {
    private InteractionPostClickSettleFactory() {
        // Static factory utility.
    }

    static InteractionPostClickSettleService createPostClickSettleService(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey
    ) {
        return createPostClickSettleServiceFromHost(
            createPostClickSettleHost(executor, sessionManager, sessionInteractionKey)
        );
    }

    static InteractionPostClickSettleService createPostClickSettleServiceFromHost(
        InteractionPostClickSettleService.Host host
    ) {
        return new InteractionPostClickSettleService(host);
    }

    static InteractionPostClickSettleService.Host createPostClickSettleHost(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey
    ) {
        return createPostClickSettleHostFromDelegates(
            () -> sessionManager.hasActiveSessionOtherThan(sessionInteractionKey),
            executor::hasPendingCommandRows,
            executor::getMotorActionSerial,
            executor::performInteractionPostClickSettleMove,
            System::currentTimeMillis,
            () -> ThreadLocalRandom.current().nextInt(100),
            (minInclusive, maxInclusive) -> ThreadLocalRandom.current().nextLong(minInclusive, maxInclusive + 1L)
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
        return new InteractionPostClickSettleService.Host() {
            @Override
            public boolean hasActiveSessionOtherThanInteraction() {
                return hasActiveSessionOtherThanInteraction.getAsBoolean();
            }

            @Override
            public boolean hasPendingCommandRows() {
                return hasPendingCommandRows.getAsBoolean();
            }

            @Override
            public long currentMotorActionSerial() {
                return currentMotorActionSerial.getAsLong();
            }

            @Override
            public boolean performInteractionPostClickSettleMove(Point settleAnchor) {
                return performInteractionPostClickSettleMove.test(settleAnchor);
            }

            @Override
            public long nowMs() {
                return nowMs.getAsLong();
            }

            @Override
            public int randomPercentRoll() {
                return randomPercentRoll.getAsInt();
            }

            @Override
            public long randomLongInclusive(long minInclusive, long maxInclusive) {
                return randomLongInclusive.applyAsLong(minInclusive, maxInclusive);
            }
        };
    }
}
