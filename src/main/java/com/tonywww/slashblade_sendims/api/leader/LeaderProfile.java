package com.tonywww.slashblade_sendims.api.leader;

/** Defines who controls a Leader's parry window and attack cycle. */
public enum LeaderProfile {
    /** SenDims controls initialization, attacks, parry windows, and recovery. */
    MANAGED,
    /** The integrating mod controls parry windows through {@link LeaderApi}. */
    EXTERNAL
}