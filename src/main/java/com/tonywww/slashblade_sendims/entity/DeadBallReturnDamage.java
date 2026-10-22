package com.tonywww.slashblade_sendims.entity;

/** Server-side return-hit override; the native ball keeps its collision/explosion damage. */
public interface DeadBallReturnDamage {
    void sendims$setSlashArtReturnDamage(float damage);
}
