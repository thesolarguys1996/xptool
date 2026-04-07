package com.xptool.sessions.idle;

public interface IdleSkillPolicy {
    IdleSkillContext context();

    IdleBehaviorProfile profile();
}

