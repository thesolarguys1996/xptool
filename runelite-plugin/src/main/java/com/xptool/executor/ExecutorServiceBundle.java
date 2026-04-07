package com.xptool.executor;

import com.xptool.core.runtime.ActivityRegistry;
import com.xptool.sessions.BankSession;
import com.xptool.sessions.DropSession;
import com.xptool.sessions.InteractionSession;
import com.xptool.sessions.SessionManager;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.SceneCacheScanner;

final class ExecutorServiceBundle {
    final CombatTargetPolicy combatTargetPolicy;
    final NpcClickPointResolver npcClickPointResolver;
    final BrutusCombatSystem brutusCombatSystem;
    final SceneCacheScanner sceneCacheScanner;
    final BankSession bankSession;
    final DropSession dropSession;
    final SessionManager sessionManager;
    final InteractionSession interactionSession;
    final IdleRuntime idleRuntime;
    final ActivityRegistry activityRegistry;

    ExecutorServiceBundle(
        CombatTargetPolicy combatTargetPolicy,
        NpcClickPointResolver npcClickPointResolver,
        BrutusCombatSystem brutusCombatSystem,
        SceneCacheScanner sceneCacheScanner,
        BankSession bankSession,
        DropSession dropSession,
        SessionManager sessionManager,
        InteractionSession interactionSession,
        IdleRuntime idleRuntime,
        ActivityRegistry activityRegistry
    ) {
        this.combatTargetPolicy = combatTargetPolicy;
        this.npcClickPointResolver = npcClickPointResolver;
        this.brutusCombatSystem = brutusCombatSystem;
        this.sceneCacheScanner = sceneCacheScanner;
        this.bankSession = bankSession;
        this.dropSession = dropSession;
        this.sessionManager = sessionManager;
        this.interactionSession = interactionSession;
        this.idleRuntime = idleRuntime;
        this.activityRegistry = activityRegistry;
    }
}
