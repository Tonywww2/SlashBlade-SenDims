package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import net.minecraft.server.level.ServerLevel;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;

public final class NagaLeaderController {
    private NagaLeaderController() {
    }

    public static void tick(Naga naga, ServerLevel level) {
        if (LeaderApi.isParried(naga)) {
            if (SBSDLeader.tickParried(naga, level, naga.getPersistentData())) {
                naga.getMovementAI().doDaze();
            }
            return;
        }
        if (naga.getTarget() == null) {
            LeaderStateStorage.clearAutomaticWindowSuppression(naga);
            LeaderManager.closeParryWindow(naga);
            return;
        }

        if (naga.getMovementAI().getState() == NagaMovementPattern.MovementState.INTIMIDATE) {
            SBSDLeader.doLeaderParryIndicator(naga, level, 10);
            if (!LeaderStateStorage.isAutomaticWindowSuppressed(naga)) {
                LeaderManager.openParryWindow(naga);
            }
        } else {
            LeaderStateStorage.clearAutomaticWindowSuppression(naga);
            LeaderManager.closeParryWindow(naga);
        }
    }
}