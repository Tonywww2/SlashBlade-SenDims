package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import net.minecraft.server.level.ServerLevel;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;

public final class NagaLeaderController {
    private NagaLeaderController() {
    }

    public static void tick(Naga naga, ServerLevel level) {
        if (naga.getTarget() == null) {
            return;
        }
        if (LeaderApi.isParried(naga)) {
            if (SBSDLeader.tickParried(naga, level, naga.getPersistentData())) {
                naga.getMovementAI().doDaze();
            }
            return;
        }

        if (naga.getMovementAI().getState() == NagaMovementPattern.MovementState.INTIMIDATE) {
            SBSDLeader.doLeaderParryIndicator(naga, level, 10);
            LeaderManager.openParryWindow(naga);
        } else {
            LeaderManager.closeParryWindow(naga);
        }
    }
}