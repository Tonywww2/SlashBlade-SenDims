package com.tonywww.slashblade_sendims.items;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EstusFlask extends Item {
    public static final ArrayList<EstusFlask> ALL_ESTUS_FLASKS = new ArrayList<>(16);

    public final int CONST_AMOUNT;
    public final float PERCENTAGE_AMOUNT;
    public final int CD;

    public EstusFlask(Properties pProperties, int constAmount, float percentageAmount, int cd) {
        super(pProperties);
        CONST_AMOUNT = constAmount;
        PERCENTAGE_AMOUNT = percentageAmount;
        CD = cd;
        ALL_ESTUS_FLASKS.add(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        if (level.isClientSide()) return super.use(level, player, hand);

        float loseAmount = player.getMaxHealth() - player.getHealth();
        float healAmount = Math.min(loseAmount, CONST_AMOUNT + (loseAmount * PERCENTAGE_AMOUNT));
        player.heal(healAmount);
        ALL_ESTUS_FLASKS.forEach((flask) -> {
            player.getCooldowns().addCooldown(flask, CD);

        });
        player.playSound(SoundEvents.HONEY_DRINK, 1.0f, 0.75f);

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltips, TooltipFlag advanced) {
        tooltips.add(Component.translatable("ui.slashblade_sendims.estus_flask.base").append(String.valueOf(CONST_AMOUNT)));
        tooltips.add(Component.translatable("ui.slashblade_sendims.estus_flask.percentage").append(String.valueOf((int) (PERCENTAGE_AMOUNT * 100f)) + '%'));

        super.appendHoverText(stack, level, tooltips, advanced);
    }
}
