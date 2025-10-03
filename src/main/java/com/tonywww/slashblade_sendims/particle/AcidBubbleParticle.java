package com.tonywww.slashblade_sendims.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class AcidBubbleParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected AcidBubbleParticle(ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.quadSize *= 0.9F + world.random.nextFloat() * 0.5F;
        this.hasPhysics = true;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.spriteSet = spriteSet;
        this.friction = 0.95F;
        this.lifetime = 10 + world.random.nextInt(10);
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        int ageAt = this.lifetime - 7;
        int sprite = this.age >= ageAt ?
                Math.min(this.age - ageAt, 7) :
                0;
        this.setSprite(spriteSet.get(sprite, 8));
        if (sprite > 0) {
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(@NotNull SimpleParticleType typeIn, @NotNull ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            AcidBubbleParticle heartParticle = new AcidBubbleParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
            heartParticle.setSprite(spriteSet.get(0, 1));
            return heartParticle;
        }
    }
}