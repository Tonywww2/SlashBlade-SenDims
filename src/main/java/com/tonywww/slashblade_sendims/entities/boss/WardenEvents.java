package com.tonywww.slashblade_sendims.entities.boss;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParryAttemptEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class WardenEvents {
    private WardenEvents() {
    }

    /** 被招架后的输出窗口（破防易伤）与眩晕均为 4 秒。 */
    @SubscribeEvent
    public static void onParryAttempt(LeaderParryAttemptEvent event) {
        if (event.getTarget() instanceof TwilightBladeWarden) {
            event.setParriedTicks(TwilightBladeWarden.PARRIED_TICKS);
            event.setStunTicks(TwilightBladeWarden.PARRIED_TICKS);
        }
    }
}
