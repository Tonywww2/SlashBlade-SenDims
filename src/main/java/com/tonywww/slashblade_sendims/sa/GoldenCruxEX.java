package com.tonywww.slashblade_sendims.sa;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.util.Comparator;
import java.util.List;

public class GoldenCruxEX {

    private static final String TAG_TARGETS = "GoldenCruxEX_Targets";
    private static final String TAG_START_POS = "GoldenCruxEX_StartPos";
    private static final double SEARCH_RANGE = 8.0;
    private static final int SELECT_NUM = 5;

    // 第 0 tick: 搜寻目标，保存起始位置，赋予无敌和移除碰撞
    public static void doGoldenCruxEXStart(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        if (!(attacker.getMainHandItem().getItem() instanceof ItemSlashBlade)) return;

        // 保存初始位置
        CompoundTag persistentData = attacker.getPersistentData();
        CompoundTag startPosTag = new CompoundTag();
        startPosTag.putDouble("X", attacker.getX());
        startPosTag.putDouble("Y", attacker.getY());
        startPosTag.putDouble("Z", attacker.getZ());
        persistentData.put(TAG_START_POS, startPosTag);

        // 搜寻范围内的敌人并按当前血量降序排序
        AABB searchBox = attacker.getBoundingBox().inflate(SEARCH_RANGE);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != attacker && e.isAlive() && !e.isAlliedTo(attacker));

        enemies.sort(Comparator.comparing(LivingEntity::getHealth).reversed());

        // 如果没有找到敌人，就存一个空目标数组
        int[] targetIds = new int[SELECT_NUM];
        if (!enemies.isEmpty()) {
            for (int i = 0; i < SELECT_NUM; i++) {
                // 如果不足5个则循环取
                LivingEntity target = enemies.get(i % enemies.size());
                targetIds[i] = target.getId();
            }
        }
        persistentData.putIntArray(TAG_TARGETS, targetIds);

        // 移除碰撞体积与设置无敌
        attacker.noPhysics = true;
        attacker.setInvulnerable(true);
        attacker.setDeltaMovement(0, 0, 0); // 浮空
        attacker.setNoGravity(true);
    }

    // 第 2, 5, 8, 11, 14 tick 执行攻击 (tickIndex 分别对应 0, 1, 2, 3, 4)
    public static void doGoldenCruxEXHit(LivingEntity attacker, int hitIndex) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        CompoundTag persistentData = attacker.getPersistentData();

        attacker.setDeltaMovement(0, 0, 0); // 持续滞空
        if (!persistentData.contains(TAG_TARGETS)) return;

        int[] targetIds = persistentData.getIntArray(TAG_TARGETS);
        if (targetIds.length > hitIndex) {
            Entity target = level.getEntity(targetIds[hitIndex]);
            if (target instanceof LivingEntity) {
                // 传送到敌人周围 1 格的随机位置
                double randomAngle = level.random.nextDouble() * 2 * Math.PI;
                double offsetX = Math.cos(randomAngle);
                double offsetZ = Math.sin(randomAngle);

                double tpX = target.getX() + offsetX;
                double tpY = target.getY() + 1.25d; // 保持与敌人同一高度或稍微调整
                double tpZ = target.getZ() + offsetZ;

                attacker.teleportTo(tpX, tpY, tpZ);

                // 使玩家朝向该敌人
                attacker.lookAt(attacker.createCommandSourceStack().getAnchor(), target.position());

                // 调用原有逻辑生成小刺效果
                GoldenCrux.doGoldenCruxImpactSmall(attacker);
            }
        }
    }

    // 为了方便 Registry 调用，定义各个 tick 的包装方法
    public static void doGoldenCruxEXHit0(LivingEntity attacker) { doGoldenCruxEXHit(attacker, 0); }
    public static void doGoldenCruxEXHit1(LivingEntity attacker) { doGoldenCruxEXHit(attacker, 1); }
    public static void doGoldenCruxEXHit2(LivingEntity attacker) { doGoldenCruxEXHit(attacker, 2); }
    public static void doGoldenCruxEXHit3(LivingEntity attacker) { doGoldenCruxEXHit(attacker, 3); }
    public static void doGoldenCruxEXHit4(LivingEntity attacker) { doGoldenCruxEXHit(attacker, 4); }

    // 第 17 tick 将玩家传送回去并还原碰撞与无敌状态
    public static void doGoldenCruxEXEnd(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel)) return;
        CompoundTag persistentData = attacker.getPersistentData();

        // 还原物理碰撞体积与碰撞判定
        attacker.noPhysics = false;
        attacker.setInvulnerable(false);
        attacker.setNoGravity(false);

        if (persistentData.contains(TAG_START_POS)) {
            CompoundTag startPos = persistentData.getCompound(TAG_START_POS);
            attacker.teleportTo(startPos.getDouble("X"), startPos.getDouble("Y"), startPos.getDouble("Z"));

            // 归还完毕后清理 NBT
            persistentData.remove(TAG_START_POS);
            persistentData.remove(TAG_TARGETS);
        }
    }
}