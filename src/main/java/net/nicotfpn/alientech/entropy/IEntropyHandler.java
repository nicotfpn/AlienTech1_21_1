package net.nicotfpn.alientech.entropy;

/**
 * Entropy capability interface — a custom energy system separate from Forge
 * Energy (FE).
 * <p>
 * Entropy represents biomechanical decay energy extracted from living
 * organisms.
 * It exists as an independent energy tier in the AlienTech progression chain:
 * Mob → Decay → Entropy → Gravitons → FE
 * <p>
 * All values use {@code long} to prevent overflow at high scales.
 * Registered as a NeoForge BlockCapability via {@link ModCapabilities}.
 */
public interface IEntropyHandler {

    /**
     * @return current entropy stored
     */
    long getEntropy();

    /**
     * @return maximum entropy capacity
     */
    long getMaxEntropy();

    /**
     * Insert entropy into this handler.
     *
     * @param amount   amount to insert (must be >= 0)
     * @param simulate if true, do not actually modify storage
     * @return amount actually inserted
     */
    long insertEntropy(long amount, boolean simulate);

    /**
     * Extract entropy from this handler.
     *
     * @param amount   amount to extract (must be >= 0)
     * @param simulate if true, do not actually modify storage
     * @return amount actually extracted
     */
    long extractEntropy(long amount, boolean simulate);

    /**
     * @return true if this handler can receive entropy
     */
    boolean canInsert();

    /**
     * @return true if this handler can provide entropy
     */
    boolean canExtract();
}
