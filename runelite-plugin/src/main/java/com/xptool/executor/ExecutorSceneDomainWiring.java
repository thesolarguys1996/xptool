package com.xptool.executor;

import com.xptool.systems.SceneCacheScanner;
import net.runelite.api.Client;

final class ExecutorSceneDomainWiring {
    private ExecutorSceneDomainWiring() {
    }

    static SceneCacheScanner.Host createSceneCacheScannerHost(CommandExecutor executor, Client client) {
        return new SceneCacheScanner.Host() {
            @Override
            public Client client() {
                return client;
            }

            @Override
            public net.runelite.api.TileObject[] collectTreeObjectCandidates(net.runelite.api.Tile tile) {
                return executor.collectTreeObjectCandidates(tile);
            }

            @Override
            public boolean isNormalTreeObjectCandidate(net.runelite.api.TileObject object) {
                return executor.isNormalTreeObjectCandidate(object);
            }

            @Override
            public boolean isOakTreeObjectCandidate(net.runelite.api.TileObject object) {
                return executor.isOakTreeObjectCandidate(object);
            }

            @Override
            public boolean isWillowTreeObjectCandidate(net.runelite.api.TileObject object) {
                return executor.isWillowTreeObjectCandidate(object);
            }

            @Override
            public net.runelite.api.TileObject[] collectRockObjectCandidates(net.runelite.api.Tile tile) {
                return executor.collectRockObjectCandidates(tile);
            }

            @Override
            public net.runelite.api.TileObject[] collectBankObjectCandidates(net.runelite.api.Tile tile) {
                return executor.collectBankObjectCandidates(tile);
            }
        };
    }
}
