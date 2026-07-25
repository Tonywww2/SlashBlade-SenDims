package com.tonywww.slashblade_sendims.utils;

import com.brandon3055.brandonscore.client.particle.IntParticleType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class ChaoticSlashArtEffects {
    public static final int PRIMARY_COLOR = 0x730D05;
    public static final int HIGHLIGHT_COLOR = 0xFFD63B;
    public static final float VISUAL_SCALE = 2.0F;
    public static final float DAMAGE_MULTIPLIER = 1.25F;

    private static final ResourceLocation SPARK = de("spark");
    private static final ResourceLocation ENERGY = de("energy");
    private static final ResourceLocation GUARDIAN_CLOUD = de("guardian_cloud");
    private static final ResourceLocation GUARDIAN_PROJECTILE = de("guardian_projectile");
    private static final ResourceLocation BLINK = de("blink");

    private ChaoticSlashArtEffects() {
    }

    public static void spawnJudgementCutParticles(Entity entity, RandomSource random) {
        Level level = entity.level();
        if (!level.isClientSide()) {
            return;
        }

        Vec3 center = entity.position().add(0.0D, 0.35D, 0.0D);
        double phase = entity.tickCount * 0.72D;

        ParticleOptions energyParticle = energy(190, 20, 8, 1.4F);
        if (energyParticle != null) {
            for (int layer = 0; layer < 2; layer++) {
                double angle = phase + layer * Math.PI + random.nextDouble() * 0.4D;
                double radius = 2.2D + layer * 0.7D + random.nextDouble() * 0.4D;
                Vec3 ring = center.add(
                        Math.cos(angle) * radius,
                        (random.nextDouble() - 0.5D) * 2.4D,
                        Math.sin(angle) * radius
                );
                level.addParticle(energyParticle, ring.x, ring.y, ring.z, center.x, center.y, center.z);
            }
        }

        ParticleOptions sparkParticle = spark(255, 214, 59, 0.5F, 0.05F, 10, 6, 0.0F, 0.94F);
        if (sparkParticle != null) {
            double angle = phase * 1.3D + random.nextDouble();
            Vec3 pos = center.add(
                    Math.cos(angle) * 1.7D,
                    (random.nextDouble() - 0.5D) * 2.0D,
                    Math.sin(angle) * 1.7D
            );
            Vec3 inward = center.subtract(pos).scale(0.06D);
            level.addParticle(sparkParticle, pos.x, pos.y, pos.z, inward.x, inward.y, inward.z);
        }

        if (entity.tickCount == 1 || entity.tickCount == 12) {
            addSimple(level, GUARDIAN_CLOUD, center, Vec3.ZERO);
        }
    }

    public static void spawnSlashParticles(
            Level level,
            RandomSource random,
            Vec3 position,
            Vec3 normal,
            Vec3 direction,
            float size
    ) {
        if (!level.isClientSide()) {
            return;
        }

        Vec3 particlePosition = position.add(direction.scale((random.nextDouble() - 0.5D) * size));
        Vec3 velocity = direction.scale(0.12D).add(normal.scale(0.03D));

        ParticleOptions goldSpark = directedSpark(255, 190, 60, 0.55F, 0.05F, 8, 4, 0.0F, 0.9F);
        if (goldSpark != null) {
            level.addParticle(goldSpark, particlePosition.x, particlePosition.y, particlePosition.z,
                    velocity.x, velocity.y, velocity.z);
        }

        if (random.nextInt(3) == 0) {
            ParticleOptions redSpark = spark(150, 20, 10, 0.45F, 0.05F, 7, 3, 0.0F, 0.92F);
            if (redSpark != null) {
                level.addParticle(redSpark, particlePosition.x, particlePosition.y, particlePosition.z,
                        -normal.x * 0.05D, -normal.y * 0.05D, -normal.z * 0.05D);
            }
        }
    }

    public static void spawnBlisteringSwordParticles(
            Entity entity,
            RandomSource random,
            boolean fired,
            boolean justFired
    ) {
        Level level = entity.level();
        if (!level.isClientSide()) {
            return;
        }

        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        if (justFired) {
            ParticleOptions projectile = simple(GUARDIAN_PROJECTILE);
            for (int index = 0; index < 8; index++) {
                double angle = Math.PI * 2.0D * index / 8.0D;
                Vec3 offset = new Vec3(Math.cos(angle) * 0.8D, 0.0D, Math.sin(angle) * 0.8D);
                if (projectile != null) {
                    level.addParticle(projectile,
                            center.x + offset.x, center.y + offset.y, center.z + offset.z,
                            offset.x * 0.12D, (random.nextDouble() - 0.5D) * 0.05D, offset.z * 0.12D);
                }
            }
            return;
        }

        if (fired) {
            if (entity.tickCount % 2 == 0) {
                Vec3 movement = entity.getDeltaMovement();
                Vec3 trail = movement.lengthSqr() > 1.0E-7D
                        ? movement.normalize().scale(-0.1D)
                        : Vec3.ZERO;
                Vec3 jitter = new Vec3(
                        (random.nextDouble() - 0.5D) * 0.16D,
                        (random.nextDouble() - 0.5D) * 0.16D,
                        (random.nextDouble() - 0.5D) * 0.16D
                );
                ParticleOptions trailSpark = directedSpark(255, 185, 55, 0.5F, 0.05F, 8, 3, 0.0F, 0.9F);
                if (trailSpark != null) {
                    level.addParticle(trailSpark,
                            center.x + jitter.x, center.y + jitter.y, center.z + jitter.z,
                            trail.x, trail.y, trail.z);
                }
            }
        } else if (entity.tickCount % 3 == 0) {
            double angle = entity.tickCount * 0.65D + entity.getId();
            Vec3 offset = new Vec3(Math.cos(angle) * 0.65D, 0.2D * Math.sin(angle * 0.5D), Math.sin(angle) * 0.65D);
            addSimple(level, BLINK, center.add(offset), Vec3.ZERO);
        }
    }

    private static ResourceLocation de(String path) {
        return ResourceLocation.fromNamespaceAndPath("draconicevolution", path);
    }

    private static ParticleOptions spark(
            int red,
            int green,
            int blue,
            float baseSize,
            float endSize,
            int lifetime,
            int lifetimeRandom,
            float gravity,
            float friction
    ) {
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(SPARK);
        if (type == null) {
            return null;
        }
        return new IntParticleType.IntParticleData(
                type,
                red,
                green,
                blue,
                Math.round(baseSize * 1000.0F),
                Math.round(endSize * 1000.0F),
                lifetime,
                lifetimeRandom,
                Math.round(gravity * 1000.0F),
                Math.round(friction * 1000.0F)
        );
    }

    private static ParticleOptions directedSpark(
            int red,
            int green,
            int blue,
            float baseSize,
            float endSize,
            int lifetime,
            int lifetimeRandom,
            float gravity,
            float friction
    ) {
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(SPARK);
        if (type == null) {
            return null;
        }
        return new IntParticleType.IntParticleData(
                type,
                red,
                green,
                blue,
                Math.round(baseSize * 1000.0F),
                Math.round(endSize * 1000.0F),
                lifetime,
                lifetimeRandom,
                Math.round(gravity * 1000.0F),
                Math.round(friction * 1000.0F),
                1
        );
    }

    private static ParticleOptions energy(int red, int green, int blue, float scale) {
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(ENERGY);
        if (type == null) {
            return null;
        }
        return new IntParticleType.IntParticleData(type, red, green, blue, Math.round(scale * 100.0F));
    }

    private static ParticleOptions simple(ResourceLocation id) {
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(id);
        return type instanceof ParticleOptions options ? options : null;
    }

    private static void addSimple(Level level, ResourceLocation id, Vec3 position, Vec3 velocity) {
        ParticleOptions particle = simple(id);
        if (particle != null) {
            level.addParticle(particle, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }
}