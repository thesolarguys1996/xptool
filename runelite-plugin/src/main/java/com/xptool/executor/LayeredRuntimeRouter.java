package com.xptool.executor;

import com.xptool.activities.Activity;
import com.xptool.activities.ActivityEngine;
import com.xptool.activities.CowCombatActivity;
import com.xptool.activities.FishingActivity;
import com.xptool.activities.HillGiantCombatActivity;
import com.xptool.activities.MiningActivity;
import com.xptool.activities.WoodcuttingActivity;
import com.xptool.idle.IdleEngine;
import com.xptool.models.Snapshot;
import com.xptool.motor.BaseMotorEngine;
import com.xptool.systems.AutoLoginSystem;
import com.xptool.systems.BankSystem;
import com.xptool.systems.BreakSystem;
import com.xptool.systems.DropSystem;
import com.xptool.systems.LootSystem;
import com.xptool.systems.RandomEventSystem;
import com.xptool.systems.WalkSystem;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;

public final class LayeredRuntimeRouter {
    private static final boolean ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.layeredRuntimeEnabled", "false"));
    private static final boolean LEGACY_ALLOWED =
        Boolean.parseBoolean(System.getProperty("xptool.layeredRuntimeLegacyAllowed", "false"));

    private final Logger log;
    private final BooleanSupplier hasPendingCommandRows;
    private final AutoLoginSystem autoLoginSystem;
    private final BreakSystem breakSystem;
    private final RandomEventSystem randomEventSystem;
    private final DropSystem dropSystem;
    private final BankSystem bankSystem;
    private final ActivityEngine activityEngine;
    private final IdleEngine idleEngine;

    public LayeredRuntimeRouter(
        BaseMotorEngine motorEngine,
        String configuredDropItemIds,
        BooleanSupplier hasPendingCommandRows,
        Logger log
    ) {
        this.log = log;
        this.hasPendingCommandRows = hasPendingCommandRows;

        Set<Integer> dropItemIds = parseItemIds(configuredDropItemIds);
        this.dropSystem = new DropSystem(motorEngine, dropItemIds);
        this.bankSystem = new BankSystem(motorEngine, dropSystem);

        WalkSystem walkSystem = new WalkSystem(motorEngine);
        LootSystem lootSystem = new LootSystem(motorEngine);
        this.autoLoginSystem = new AutoLoginSystem(motorEngine);
        this.breakSystem = new BreakSystem(motorEngine);
        this.randomEventSystem = new RandomEventSystem(motorEngine);

        List<Activity> activities = List.of(
            new WoodcuttingActivity(dropSystem, walkSystem),
            new MiningActivity(dropSystem, walkSystem),
            new FishingActivity(dropSystem, walkSystem),
            new CowCombatActivity(lootSystem, walkSystem),
            new HillGiantCombatActivity(lootSystem, walkSystem)
        );
        this.activityEngine = new ActivityEngine(activities, System.getProperty("xptool.activity", ""));
        this.idleEngine = new IdleEngine(motorEngine);
    }

    public void onGameTick(Snapshot snapshot) {
        if (!ENABLED || !LEGACY_ALLOWED || snapshot == null) {
            return;
        }
        if (hasPendingCommandRows != null && hasPendingCommandRows.getAsBoolean()) {
            return;
        }

        if (autoLoginSystem.shouldRun(snapshot) && autoLoginSystem.run(snapshot)) {
            emitRouted(autoLoginSystem.name(), snapshot);
            return;
        }
        if (breakSystem.shouldRun(snapshot) && breakSystem.run(snapshot)) {
            emitRouted(breakSystem.name(), snapshot);
            return;
        }
        if (randomEventSystem.shouldRun(snapshot) && randomEventSystem.run(snapshot)) {
            emitRouted(randomEventSystem.name(), snapshot);
            return;
        }

        if (dropSystem.shouldRun(snapshot) && dropSystem.run(snapshot)) {
            emitRouted(dropSystem.name(), snapshot);
            return;
        }
        if (bankSystem.shouldRun(snapshot) && bankSystem.run(snapshot)) {
            emitRouted(bankSystem.name(), snapshot);
            return;
        }

        if (activityEngine.hasActiveActivity() && activityEngine.run(snapshot)) {
            emitRouted(activityEngine.activeActivityName(), snapshot);
            return;
        }
        if (idleEngine.run(snapshot)) {
            emitRouted("IdleEngine", snapshot);
        }
    }

    private void emitRouted(String subsystem, Snapshot snapshot) {
        if (log == null || subsystem == null || subsystem.isBlank()) {
            return;
        }
        log.debug(
            "xptool.layered_route subsystem={} tick={} inventoryUsed={} activity={}",
            subsystem,
            snapshot == null ? -1 : snapshot.tick,
            snapshot == null ? -1 : snapshot.inventorySlotsUsed(),
            activityEngine.activeActivityName()
        );
    }

    private static Set<Integer> parseItemIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<Integer> out = new LinkedHashSet<>();
        String[] tokens = raw.split(",");
        for (String token : tokens) {
            String trimmed = token == null ? "" : token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int itemId = Integer.parseInt(trimmed);
                if (itemId > 0) {
                    out.add(itemId);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed ids and keep valid entries.
            }
        }
        return out;
    }
}

