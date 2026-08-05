package com.tonywww.slashblade_sendims.leader;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class ManagedLeaderController {
    private ManagedLeaderController() {
    }

    public static void tick(LivingEntity entity, ServerLevel level) {
        SBSDLeader.tickLeader(entity, level, entity.getPersistentData(), entity.tickCount);
    }
}