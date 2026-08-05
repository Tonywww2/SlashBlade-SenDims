package com.tonywww.slashblade_sendims.api.leader;

public enum ParryResult {
    SUCCESS,
    NOT_LEADER,
    NOT_PARRYABLE,
    WRONG_SIDE,
    ABSORBED;

    public boolean isAccepted() {
        return this == SUCCESS || this == ABSORBED;
    }
}