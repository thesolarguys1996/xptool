package com.xptool.sessions.idle;

final class FixedIdleSkillPolicy implements IdleSkillPolicy {
    private final IdleSkillContext context;
    private final IdleBehaviorProfile profile;

    FixedIdleSkillPolicy(IdleSkillContext context, IdleBehaviorProfile profile) {
        this.context = context == null ? IdleSkillContext.GLOBAL : context;
        this.profile = profile == null
            ? new IdleBehaviorProfile(45, 35, 0, 10, 10, 2, 42)
            : profile;
    }

    @Override
    public IdleSkillContext context() {
        return context;
    }

    @Override
    public IdleBehaviorProfile profile() {
        return profile;
    }
}
