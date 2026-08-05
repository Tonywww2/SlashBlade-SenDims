package com.tonywww.slashblade_sendims.api.leader;

/** Mutually exclusive phases of a registered Leader. */
public enum LeaderPhase {
    NORMAL,
    /** The entity can currently be parried. */
    PARRYABLE,
    /** A parry succeeded and the entity is in its vulnerable state. */
    PARRIED
}