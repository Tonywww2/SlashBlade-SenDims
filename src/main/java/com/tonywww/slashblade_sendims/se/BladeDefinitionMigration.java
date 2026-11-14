package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * 拔刀剑的 SE 和耐久在生成物品时写进 NBT，修改刀的定义不会影响已有的刀。
 * 这里把已有的刀补到当前定义：玩家背包里的刀在更新时检查一次，处理后打上版本标记。
 * 按当前定义新生成的刀在生成时直接打上标记（见 SlashBladeDefinitionMixin），不会被重复处理。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class BladeDefinitionMigration {
    public static final String REVISION_TAG = "sbsd_def_rev";
    public static final int REVISION = 1;

    /**
     * addEffects：缺少就补上的 SE；maxDamageDelta：耐久上限的增量；
     * minMaxDamage：耐久上限至少补到这个值（0 表示不检查）；minEnchantments：附魔至少补到对应等级。
     */
    public record Rule(List<ResourceLocation> addEffects, int maxDamageDelta, int minMaxDamage,
                       Map<ResourceLocation, Integer> minEnchantments) {
        public Rule(List<ResourceLocation> addEffects, int maxDamageDelta) {
            this(addEffects, maxDamageDelta, 0, Map.of());
        }
    }

    private static final ResourceLocation AVALON = SenDims.prefix("avalon");
    private static final ResourceLocation CROW = SenDims.prefix("amagumo_crow");
    private static final ResourceLocation WIND = SenDims.prefix("amagumo_wind");
    private static final ResourceLocation BLEEDING = new ResourceLocation("slashblade", "bleeding_cutting");
    private static final ResourceLocation BLESSING_AND_BANE = SenDims.prefix("blessing_and_bane");
    private static final ResourceLocation FORTUNE = new ResourceLocation("minecraft", "fortune");

    public static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("item.slashblade_sendims.caliburn", new Rule(List.of(), 120)),
            Map.entry("item.slashblade_sendims.excalibur", new Rule(List.of(AVALON), 120)),
            Map.entry("item.last_smith.amagumo_kaze", new Rule(List.of(CROW), 0)),
            Map.entry("item.last_smith.amagumo_kumo", new Rule(List.of(CROW), 0)),
            Map.entry("item.last_smith.amagumo_munin", new Rule(List.of(CROW, WIND), 0)),
            Map.entry("item.last_smith.uma_tachyon", new Rule(List.of(CROW, WIND), 0)),
            Map.entry("item.last_smith.roukanken_nether", new Rule(List.of(BLEEDING), 0)),
            Map.entry("item.slashblade_addon.nihilul", new Rule(List.of(BLEEDING), 0)),
            Map.entry("item.slashblade_addon.nihilbx", new Rule(List.of(BLEEDING), 0)),
            Map.entry("item.sjap_adder.nihilulex", new Rule(List.of(BLEEDING), 0)),
            // 天顶剑（整合包定义）：研究刀后来补了 SE、耐久或换了研究刀，已经合成的天顶剑按当前定义补齐
            Map.entry("item.slashblade_sendims.zenith_false_kingship",
                    new Rule(List.of(AVALON, CROW, WIND), 0, 160, Map.of())),
            Map.entry("item.slashblade_sendims.zenith_false_black_crown",
                    new Rule(List.of(BLESSING_AND_BANE), 0, 0, Map.of(FORTUNE, 7)))
    );

    private BladeDefinitionMigration() {}

    /** 新生成的刀已经符合当前定义，直接打标记。 */
    public static void stamp(ItemStack blade, String translationKey) {
        if (!blade.isEmpty() && translationKey != null && RULES.containsKey(translationKey)) {
            blade.getOrCreateTag().putInt(REVISION_TAG, REVISION);
        }
    }

    /** 返回是否修改了这把刀。 */
    public static boolean migrate(ItemStack blade, ISlashBladeState state) {
        if (blade.isEmpty() || state == null) return false;
        if (blade.hasTag() && blade.getTag().getInt(REVISION_TAG) >= REVISION) return false;
        Rule rule = RULES.get(state.getTranslationKey());
        if (rule == null) return false;
        for (ResourceLocation effect : rule.addEffects()) {
            if (!state.hasSpecialEffect(effect)) state.addSpecialEffect(effect);
        }
        if (rule.maxDamageDelta() != 0) {
            state.setMaxDamage(Math.max(1, state.getMaxDamage() + rule.maxDamageDelta()));
        }
        if (rule.minMaxDamage() > 0 && state.getMaxDamage() < rule.minMaxDamage()) {
            state.setMaxDamage(rule.minMaxDamage());
        }
        if (!rule.minEnchantments().isEmpty()) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(blade);
            boolean changed = false;
            for (Map.Entry<ResourceLocation, Integer> entry : rule.minEnchantments().entrySet()) {
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(entry.getKey());
                if (enchantment == null || enchantments.getOrDefault(enchantment, 0) >= entry.getValue()) continue;
                enchantments.put(enchantment, entry.getValue());
                changed = true;
            }
            if (changed) EnchantmentHelper.setEnchantments(enchantments, blade);
        }
        blade.getOrCreateTag().putInt(REVISION_TAG, REVISION);
        return true;
    }

    @SubscribeEvent
    public static void onUpdate(SlashBladeEvent.UpdateEvent event) {
        if (event.getLevel() == null || event.getLevel().isClientSide() || !(event.getEntity() instanceof Player)) return;
        migrate(event.getBlade(), event.getSlashBladeState());
    }
}
