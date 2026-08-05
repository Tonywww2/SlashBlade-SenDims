package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class ExternalLeaderController {
    private ExternalLeaderController() {
    }

    public static void tick(LivingEntity entity, ServerLevel level) {
        LeaderManager.tickExternal(entity);
        if (LeaderApi.isParried(entity)) {
            SBSDLeader.tickParried(entity, level, entity.getPersistentData());
        }
    }
}