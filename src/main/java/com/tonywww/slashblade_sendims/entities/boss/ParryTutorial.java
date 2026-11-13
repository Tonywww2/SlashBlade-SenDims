package com.tonywww.slashblade_sendims.entities.boss;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 招架教程：守卫开始一个招式时，向附近玩家的动作栏显示一句简短的蓝刀（蓝色）或红刀（红色）提示，不含招式名。
 * 玩家用石英右键守卫后永久关闭（存于玩家持久数据，死亡不丢），用钻石右键重新开启。
 */
public final class ParryTutorial {
    private static final String DISABLED_KEY = "slashblade_sendims.parry_tutorial_disabled";
    private static final double RANGE = 32.0D;
    private static final int BLUE_TEXT = 0x55B4FF;
    private static final int RED_TEXT = 0xFF5555;

    private ParryTutorial() {
    }

    static void announce(TwilightBladeWarden warden, WardenAttacks.Move move) {
        if (!(warden.level() instanceof ServerLevel level)) {
            return;
        }
        Component text = Component.translatable(
                        move.red ? "message.slashblade_sendims.parry_tutorial.red"
                                : "message.slashblade_sendims.parry_tutorial.blue")
                .withStyle(style -> style.withColor(move.red ? RED_TEXT : BLUE_TEXT));
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(warden) <= RANGE * RANGE && !isDisabled(player)) {
                player.displayClientMessage(text, true);
            }
        }
    }

    public static boolean isDisabled(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(DISABLED_KEY);
    }

    static void setDisabled(ServerPlayer player, boolean disabled) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (disabled) {
            persisted.putBoolean(DISABLED_KEY, true);
        } else {
            persisted.remove(DISABLED_KEY);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        player.sendSystemMessage(Component.translatable(disabled
                        ? "message.slashblade_sendims.parry_tutorial.disabled"
                        : "message.slashblade_sendims.parry_tutorial.enabled")
                .withStyle(ChatFormatting.GRAY));
    }
}
