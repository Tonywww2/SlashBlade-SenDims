package com.tonywww.bossrefactoraether.integration;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.AvoidObstaclesGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.BackOffAfterAttackGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.CollideGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.CrushGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.SetPathUpOrDownGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.SliderMoveGoal;
import com.mojang.logging.LogUtils;
import com.tonywww.bossrefactoraether.mixin.LivingEntityDamageBlockAccessor;
import com.tonywww.bossrefactoraether.slider.SliderStateAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.util.Arrays;

@Mod.EventBusSubscriber(modid = "bossrefactoraether", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BossMixinIntegrationAssertion {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BossMixinIntegrationAssertion() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        require(SliderStateAccess.class.isAssignableFrom(Slider.class),
                "SliderMixin interface was not applied");
        require(LivingEntityDamageBlockAccessor.class.isAssignableFrom(LivingEntity.class),
            "LivingEntityDamageBlockAccessor was not applied");
        requireInjectedMethod(CollideGoal.class, "bossRefactorAether$suppressCollision");
        requireInjectedMethod(SliderMoveGoal.class, "bossRefactorAether$suppressMove");
        requireInjectedMethod(CrushGoal.class, "bossRefactorAether$suppressPathing");
        requireInjectedMethod(AvoidObstaclesGoal.class, "bossRefactorAether$suppressPathing");
        requireInjectedMethod(BackOffAfterAttackGoal.class, "bossRefactorAether$suppressPathing");
        requireInjectedMethod(SetPathUpOrDownGoal.class, "bossRefactorAether$suppressPathing");
        LOGGER.info("Verified all BossRefactorAether Slider mixins in SenDimS integration runtime");
    }

    private static void requireInjectedMethod(Class<?> target, String methodName) {
        boolean present = Arrays.stream(target.getDeclaredMethods())
            .anyMatch(method -> method.getName().contains(methodName));
        String declaredMethods = Arrays.stream(target.getDeclaredMethods())
            .map(method -> method.getName())
            .sorted()
            .toList()
            .toString();
        require(present, target.getName() + " is missing " + methodName
            + "; declared methods: " + declaredMethods);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}