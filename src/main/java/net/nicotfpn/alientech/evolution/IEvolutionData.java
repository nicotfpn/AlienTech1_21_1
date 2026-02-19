package net.nicotfpn.alientech.evolution;

import net.minecraft.nbt.CompoundTag;

/**
 * Interface defining a player's evolution state.
 * <p>
 * Tracks evolution stage, entropy capacity, and stored entropy.
 * This is the foundation for future alien abilities powered by entropy.
 * <p>
 * Designed for attachment-based persistence (survives saves and death).
 */
public interface IEvolutionData {

    /** @return current evolution stage (0 = baseline, higher = more evolved) */
    int getEvolutionStage();

    /** Set the evolution stage. */
    void setEvolutionStage(int stage);

    /** @return maximum entropy this player can store */
    int getEntropyCapacity();

    /** Set entropy capacity (clamped >= 0). */
    void setEntropyCapacity(int capacity);

    /** @return current entropy stored in the player */
    int getStoredEntropy();

    /** Set stored entropy directly (clamped to [0, capacity]). */
    void setStoredEntropy(int entropy);

    /** @return true if the player can receive more entropy */
    boolean canAcceptEntropy();

    /**
     * Insert entropy into the player's storage.
     *
     * @param amount amount to insert (>= 0)
     * @return amount actually inserted
     */
    int insertEntropy(int amount);

    /**
     * Extract entropy from the player's storage.
     *
     * @param amount amount to extract (>= 0)
     * @return amount actually extracted
     */
    int extractEntropy(int amount);

    /** Serialize this evolution data to NBT. */
    CompoundTag serializeNBT();

    /** Deserialize evolution data from NBT. */
    void deserializeNBT(CompoundTag tag);
}
