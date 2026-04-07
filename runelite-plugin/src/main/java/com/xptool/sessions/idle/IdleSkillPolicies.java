package com.xptool.sessions.idle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class IdleSkillPolicies {
    private static final IdleBehaviorProfile GLOBAL_PROFILE =
        new IdleBehaviorProfile(28, 22, 0, 12, 38, 2, 50);
    private static final IdleBehaviorProfile FISHING_PROFILE =
        new IdleBehaviorProfile(20, 16, 10, 8, 46, 2, 34);
    private static final IdleBehaviorProfile WOODCUTTING_PROFILE =
        new IdleBehaviorProfile(20, 16, 10, 8, 46, 2, 34);
    private static final IdleBehaviorProfile MINING_PROFILE =
        new IdleBehaviorProfile(24, 24, 0, 12, 40, 2, 44);
    private static final IdleBehaviorProfile COMBAT_PROFILE =
        new IdleBehaviorProfile(18, 16, 0, 8, 58, 3, 34);

    private IdleSkillPolicies() {
    }

    public static Map<IdleSkillContext, IdleSkillPolicy> defaults() {
        EnumMap<IdleSkillContext, IdleSkillPolicy> out = new EnumMap<>(IdleSkillContext.class);
        out.put(IdleSkillContext.GLOBAL, new FixedIdleSkillPolicy(IdleSkillContext.GLOBAL, GLOBAL_PROFILE));
        out.put(IdleSkillContext.FISHING, new FixedIdleSkillPolicy(IdleSkillContext.FISHING, FISHING_PROFILE));
        out.put(IdleSkillContext.WOODCUTTING, new FixedIdleSkillPolicy(IdleSkillContext.WOODCUTTING, WOODCUTTING_PROFILE));
        out.put(IdleSkillContext.MINING, new FixedIdleSkillPolicy(IdleSkillContext.MINING, MINING_PROFILE));
        out.put(IdleSkillContext.COMBAT, new FixedIdleSkillPolicy(IdleSkillContext.COMBAT, COMBAT_PROFILE));
        return Collections.unmodifiableMap(out);
    }
}
