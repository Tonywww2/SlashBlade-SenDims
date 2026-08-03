package com.tonywww.slashblade_sendims.mixin.netherman;

import com.benji.netherman.entity.GildedGolemEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = GildedGolemEntity.class, remap = false)
public abstract class GildedGolemEntityMixin {

    @Unique
    private static final TagKey<Item> slashblade_sendims$gildedGolemAbsorbable = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("slashblade_sendims", "gilded_golem_absorbable")
    );

    @Redirect(
            method = "performHealingSuck",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    ordinal = 0,
                    remap = true
            ),
            remap = false
    )
    private List<ItemEntity> slashblade_sendims$getAbsorbableItems(Level level, Class<ItemEntity> entityClass, AABB bounds) {
        return level.getEntitiesOfClass(
                entityClass,
                bounds,
                itemEntity -> itemEntity.getItem().is(slashblade_sendims$gildedGolemAbsorbable)
        );
    }
}