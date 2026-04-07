package com.xptool.executor;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.WorldView;

final class WorldViewResolver {
    private final IntFunction<WorldView> resolveById;
    private final Supplier<WorldView> resolveTopLevel;

    WorldViewResolver(IntFunction<WorldView> resolveById, Supplier<WorldView> resolveTopLevel) {
        this.resolveById = resolveById;
        this.resolveTopLevel = resolveTopLevel;
    }

    static WorldViewResolver fromClient(Client client) {
        return new WorldViewResolver(
            worldViewId -> client == null ? null : client.getWorldView(worldViewId),
            () -> client == null ? null : client.getTopLevelWorldView()
        );
    }

    WorldView resolveByIdOrTopLevel(int worldViewId) {
        if (worldViewId >= 0) {
            WorldView byId = resolveById.apply(worldViewId);
            if (byId != null) {
                return byId;
            }
        }
        return topLevel();
    }

    WorldView topLevel() {
        return resolveTopLevel.get();
    }
}
