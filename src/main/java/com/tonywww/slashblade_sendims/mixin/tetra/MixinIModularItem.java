//package com.tonywww.slashblade_sendims.mixin.tetra;
//
//import com.google.common.collect.ArrayListMultimap;
//import com.google.common.collect.Multimap;
//import com.google.common.collect.Multimaps;
//import net.minecraft.world.entity.ai.attributes.Attribute;
//import net.minecraft.world.entity.ai.attributes.AttributeModifier;
//import net.minecraft.world.item.ItemStack;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//import se.mickelus.tetra.items.modular.IModularItem;
//import se.mickelus.tetra.properties.AttributeHelper;
//import se.mickelus.tetra.module.ItemModule;
//import se.mickelus.tetra.module.data.SynergyData;
//import se.mickelus.tetra.module.data.EffectData;
//import se.mickelus.tetra.module.data.ItemProperties;
//
//import java.util.Map;
//import java.util.Optional;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Objects;
//import java.util.stream.Stream;
//
//@Mixin(IModularItem.class)
//public interface MixinIModularItem {
//
//    @Shadow(remap = false)
//    Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack itemStack);
//
//    @Shadow(remap = false)
//    Multimap<Attribute, AttributeModifier> fixIdentifiers(Multimap<Attribute, AttributeModifier> modifiers);
//
//    @Shadow(remap = false)
//    Collection<ItemModule> getAllModules(ItemStack itemStack);
//
//    @Shadow(remap = false)
//    SynergyData[] getSynergyData(ItemStack itemStack);
//
//    @Inject(
//            method = "getAttributeModifiersCollapsed",
//            at = @At("HEAD"),
//            remap = false,
//            cancellable = true)
//    private void injectGetAttributeModifiersCollapsed(ItemStack itemStack, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
//        cir.setReturnValue(
//                Optional.ofNullable(this.getAttributeModifiers(itemStack))
//                        .map(
//                                (modifiers) ->
//                                        (ArrayListMultimap) modifiers.asMap()
//                                                .entrySet().stream()
//                                                .collect(Multimaps.flatteningToMultimap(Map.Entry::getKey,
//                                                        (entry) ->
//                                                                AttributeHelper.collapse(entry.getValue()).stream(), ArrayListMultimap::create)))
//                        .map(this::fixIdentifiers).orElse(null));
//    }
//
//    @Inject(
//            method = "getEffectData",
//            at = @At("HEAD"),
//            remap = false,
//            cancellable = true)
//    private void injectGetEffectData(ItemStack itemStack, CallbackInfoReturnable<EffectData> cir) {
//        cir.setReturnValue(
//                Stream.concat(
//                                this.getAllModules(itemStack).stream()
//                                        .map(module -> module.getEffectData(itemStack)),
//                                Arrays.stream(this.getSynergyData(itemStack))
//                                        .map(synergy -> synergy.effects))
//                        .filter(Objects::nonNull)
//                        .reduce(null, EffectData::merge)
//        );
//    }
//
//    @Inject(
//            method = "getProperties(Lnet/minecraft/world/item/ItemStack;)Lse/mickelus/tetra/module/data/ItemProperties;",
//            at = @At("HEAD"),
//            remap = false,
//            cancellable = true)
//    private void injectGetProperties(ItemStack itemStack, CallbackInfoReturnable<ItemProperties> cir) {
//        cir.setReturnValue(
//                Stream.concat(
//                                this.getAllModules(itemStack).stream().map(module -> module.getProperties(itemStack)),
//                                Arrays.stream(this.getSynergyData(itemStack)))
//                        .reduce(new ItemProperties(), ItemProperties::merge)
//        );
//    }
//}
