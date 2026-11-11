package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class TofuAccessoryEvents {
    private TofuAccessoryEvents() {
    }

    /** Called after successful removal and Forge's native canHarvestBlock check. */
    public static void onOreMined(Player player, BlockState state, ItemStack tool) {
        if (!(player instanceof ServerPlayer) || !state.is(Tags.Blocks.ORES)
                || !CuriosApi.getCuriosInventory(player)
                    .map(inventory -> inventory.findFirstCurio(SBSDItems.TOFU_MINER_LANTERN.get()).isPresent())
                    .orElse(false)) {
            return;
        }
        if (tool.isDamageableItem() && tool.isDamaged()) {
            tool.setDamageValue(tool.getDamageValue() - 1);
        }
        if (player.getRandom().nextFloat() < 0.25F) {
            player.getFoodData().eat(1, 0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player
                || !CuriosApi.getCuriosInventory(player)
                    .map(inventory -> inventory.findFirstCurio(SBSDItems.TOFU_HUNTER_KNIFE.get()).isPresent())
                    .orElse(false)) {
            return;
        }
        player.getFoodData().eat(1, 0.0F);
    }
}
