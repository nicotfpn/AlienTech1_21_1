package net.nicotfpn.alientech.machine.decay;

/**
 * Represents the state of a mob being processed in the Decay Chamber.
 * <p>
 * State machine progression:
 * EMPTY → CAPTURED → STABILIZED → DECAYING → CONSUMED → EMPTY
 */
public enum MobDecayState {
    /** No mob in chamber */
    EMPTY,
    /** Mob just inserted, not yet processing */
    CAPTURED,
    /** Mob stabilized, ready for decay processing */
    STABILIZED,
    /** Active decay in progress — generating entropy biomass */
    DECAYING,
    /** Decay complete — mob fully consumed */
    CONSUMED;

    /**
     * @return the next state in the decay progression
     */
    public MobDecayState next() {
        return switch (this) {
            case EMPTY -> EMPTY;
            case CAPTURED -> STABILIZED;
            case STABILIZED -> DECAYING;
            case DECAYING -> CONSUMED;
            case CONSUMED -> EMPTY;
        };
    }

    /**
     * @return true if the chamber is actively processing
     */
    public boolean isActive() {
        return this == STABILIZED || this == DECAYING;
    }
}
