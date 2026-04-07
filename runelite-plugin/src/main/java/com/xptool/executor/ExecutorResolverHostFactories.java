package com.xptool.executor;

import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.SceneCacheScanner;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.util.function.BiPredicate;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

final class ExecutorResolverHostFactories {
    private ExecutorResolverHostFactories() {
    }

    static Supplier<WoodcuttingTargetResolver.Host> createWoodcuttingTargetResolverHostSupplier(
        Supplier<WorldPoint> localPlayerWorldPoint,
        Supplier<WorldPoint> lockedWoodcutWorldPoint,
        Supplier<WorldPoint> preferredSelectedWoodcutWorldPoint,
        IntSupplier selectedWoodcutTargetCount,
        Supplier<Iterable<TileObject>> cachedTreeObjects,
        Supplier<Iterable<TileObject>> cachedNormalTreeObjects,
        Supplier<Iterable<TileObject>> cachedOakTreeObjects,
        Supplier<Iterable<TileObject>> cachedWillowTreeObjects,
        Predicate<WorldPoint> hasSelectedTreeTargetNear,
        BiPredicate<WorldPoint, WorldPoint> worldPointsMatch,
        BiFunction<Iterable<TileObject>, WoodcuttingTargetResolver.WorldDistanceProvider, Optional<TileObject>>
            selectBestCursorAwareTarget
    ) {
        return () -> ExecutorSkillingDomainWiring.createWoodcuttingTargetResolverHost(
            localPlayerWorldPoint,
            lockedWoodcutWorldPoint,
            preferredSelectedWoodcutWorldPoint,
            selectedWoodcutTargetCount,
            cachedTreeObjects,
            cachedNormalTreeObjects,
            cachedOakTreeObjects,
            cachedWillowTreeObjects,
            hasSelectedTreeTargetNear,
            worldPointsMatch,
            selectBestCursorAwareTarget
        );
    }

    static Supplier<MiningTargetResolver.Host> createMiningTargetResolverHostSupplier(
        IntSupplier selectedMiningTargetCount,
        Supplier<WorldPoint> localPlayerWorldPoint,
        Supplier<Iterable<TileObject>> cachedRockObjects,
        Predicate<TileObject> isRockObjectCandidate,
        Predicate<WorldPoint> isMiningRockSuppressed,
        Predicate<WorldPoint> hasSelectedRockTargetNear,
        BiFunction<Iterable<TileObject>, MiningTargetResolver.WorldDistanceProvider, Optional<TileObject>>
            selectBestCursorAwareTarget
    ) {
        return () -> ExecutorSkillingDomainWiring.createMiningTargetResolverHost(
            selectedMiningTargetCount,
            localPlayerWorldPoint,
            cachedRockObjects,
            isRockObjectCandidate,
            isMiningRockSuppressed,
            hasSelectedRockTargetNear,
            selectBestCursorAwareTarget
        );
    }

    static Supplier<FishingTargetResolver.Host> createFishingTargetResolverHostSupplier(
        CommandExecutor executor,
        Client client
    ) {
        return () -> ExecutorSkillingDomainWiring.createFishingTargetResolverHost(executor, client);
    }

    static Supplier<CombatTargetPolicy.Host> createCombatTargetPolicyHostSupplier(
        CommandExecutor executor,
        Client client
    ) {
        return () -> ExecutorCombatDomainWiring.createCombatTargetPolicyHost(executor, client);
    }

    static Function<CombatTargetPolicy, CombatTargetResolver.Host> createCombatTargetResolverHostFactory(
        CommandExecutor executor,
        Client client
    ) {
        return combatPolicy -> ExecutorCombatDomainWiring.createCombatTargetResolverHost(executor, client, combatPolicy);
    }

    static Supplier<SceneCacheScanner.Host> createSceneCacheScannerHostSupplier(
        CommandExecutor executor,
        Client client
    ) {
        return () -> ExecutorSceneDomainWiring.createSceneCacheScannerHost(executor, client);
    }
}
