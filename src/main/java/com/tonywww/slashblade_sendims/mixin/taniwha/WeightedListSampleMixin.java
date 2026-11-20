package com.tonywww.slashblade_sendims.mixin.taniwha;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Biome Makeover 1.11.4 自带的 taniwha 5.4.4：WeightedList.sample() 每次先对内部列表做 Collections.shuffle（原地乱序），
 * 再按 totalWeight 抽取。女巫任务分类、任务物品数量和任务奖励都存放在静态的 WeightedList 里，女巫构造时就会抽取：
 * 单人游戏的服务端线程和客户端线程共用同一份列表，专用服务端上世界生成线程（C2ME）也会和主线程同时构造女巫。
 * 两个线程同时原地乱序会让列表里某些条目被复制、某些丢失，条目权重之和与 totalWeight 对不上，之后每次抽取都可能抛出
 * "Weighted List Was Empty"，直到重启重建列表。按权重抽取不需要先乱序，这里去掉乱序，列表在抽取时保持只读。
 */
@Pseudo
@Mixin(targets = "party.lemons.taniwha.util.collections.WeightedList", remap = false)
public abstract class WeightedListSampleMixin {
    @Redirect(method = "sample", at = @At(value = "INVOKE", target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V", remap = false), require = 0, remap = false)
    private void sdbf$keepOrder(List<?> entries) {
    }
}
