package com.tonywww.slashblade_sendims.twilight;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class FinalCastleEvents {
    private FinalCastleEvents() {
    }

    /** Key guards spawned by {@link FinalCastleDecorator} become managed Leaders (parry windows, slash arts). */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living.getTags().contains(FinalCastleDecorator.GUARD_TAG) && !LeaderApi.isLeader(living)) {
            LeaderApi.registerLeader(living, LeaderProfile.MANAGED);
        }
    }
}
