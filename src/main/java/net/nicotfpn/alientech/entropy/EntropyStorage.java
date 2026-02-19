package net.nicotfpn.alientech.entropy;

import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.util.StateValidator;
import net.nicotfpn.alientech.util.AlienTechDebug;

/**
 * Concrete implementation of {@link IEntropyHandler}.
 * <p>
 * Stores entropy with configurable capacity, insert/extract limits, and access
 * mode.
 * All operations are clamped — no overflow, no underflow, no negative values.
 * <p>
 * NBT key: "Entropy" (single int).
 * <p>
 * Thread safety: All mutation methods are internally consistent (no torn
 * reads),
 * but external synchronization is needed for cross-thread access if applicable.
 */
public class EntropyStorage implements IEntropyHandler {

    private int entropy;
    private final int capacity;
    private final int maxInsert;
    private final int maxExtract;
    private final boolean allowInsert;
    private final boolean allowExtract;
    private final Runnable onChanged;

    /**
     * Full constructor with fine-grained control.
     *
     * @param capacity   maximum entropy capacity (must be > 0)
     * @param maxInsert  maximum entropy insertable per operation (0 = no limit
     *                   beyond capacity)
     * @param maxExtract maximum entropy extractable per operation (0 = no limit
     *                   beyond stored)
     * @param canInsert  whether this storage accepts entropy
     * @param canExtract whether this storage provides entropy
     * @param onChanged  callback invoked on any mutation (for dirty marking / sync)
     */
    public EntropyStorage(int capacity, int maxInsert, int maxExtract,
            boolean canInsert, boolean canExtract, Runnable onChanged) {
        this.capacity = Math.max(1, capacity);
        this.maxInsert = Math.max(0, maxInsert);
        this.maxExtract = Math.max(0, maxExtract);
        this.allowInsert = canInsert;
        this.allowExtract = canExtract;
        this.onChanged = onChanged != null ? onChanged : () -> {
        };
        this.entropy = 0;
    }

    /**
     * Convenience constructor for a bidirectional storage with unlimited transfer
     * rates.
     */
    public EntropyStorage(int capacity, Runnable onChanged) {
        this(capacity, 0, 0, true, true, onChanged);
    }

    // ==================== IEntropyHandler ====================

    @Override
    public int getEntropy() {
        return entropy;
    }

    @Override
    public int getMaxEntropy() {
        return capacity;
    }

    @Override
    public int insertEntropy(int amount, boolean simulate) {
        // Validate input
        if (!allowInsert || amount <= 0) {
            return 0;
        }

        // Prevent integer overflow
        if (amount < 0 || entropy < 0 || capacity < 0) {
            return 0; // Invalid state
        }

        int space = capacity - entropy;
        if (space <= 0) {
            return 0; // Full
        }

        int toInsert = Math.min(amount, space);

        // Apply transfer rate limit if configured
        if (maxInsert > 0) {
            toInsert = Math.min(toInsert, maxInsert);
        }

        // Final validation before commit
        if (toInsert <= 0) {
            return 0;
        }

        // Prevent overflow
        if (entropy > Integer.MAX_VALUE - toInsert) {
            toInsert = Integer.MAX_VALUE - entropy;
            if (toInsert <= 0) {
                return 0;
            }
        }

        if (!simulate && toInsert > 0) {
            entropy += toInsert;
            // Validate state after mutation
            validateState();
            onChanged.run();
            
            AlienTechDebug.ENTROPY.log("Inserted {} entropy (total: {}/{})", toInsert, entropy, capacity);
        }
        return toInsert;
    }

    @Override
    public int extractEntropy(int amount, boolean simulate) {
        // Validate input
        if (!allowExtract || amount <= 0) {
            return 0;
        }

        // Prevent negative entropy
        if (entropy <= 0) {
            return 0; // Empty
        }

        int toExtract = Math.min(amount, entropy);

        // Apply transfer rate limit if configured
        if (maxExtract > 0) {
            toExtract = Math.min(toExtract, maxExtract);
        }

        // Final validation before commit
        if (toExtract <= 0 || toExtract > entropy) {
            return 0;
        }

        if (!simulate && toExtract > 0) {
            entropy -= toExtract;
            // Validate state after mutation
            validateState();
            onChanged.run();
            
            AlienTechDebug.ENTROPY.log("Extracted {} entropy (remaining: {}/{})", toExtract, entropy, capacity);
        }
        return toExtract;
    }

    @Override
    public boolean canInsert() {
        return allowInsert;
    }

    @Override
    public boolean canExtract() {
        return allowExtract;
    }

    // ==================== Direct Setters ====================

    /**
     * Set entropy directly. Used for client-side sync and NBT deserialization.
     * Clamped to [0, capacity].
     * 
     * @param value the entropy value to set (will be clamped)
     */
    public void setEntropy(int value) {
        // Validate capacity is valid
        if (capacity <= 0) {
            this.entropy = 0;
            return;
        }
        // Clamp to valid range and prevent negative values
        this.entropy = Math.max(0, Math.min(Math.max(0, value), capacity));
    }

    // ==================== NBT Persistence ====================

    /**
     * Save entropy value to the given tag.
     */
    public void save(CompoundTag tag) {
        tag.putInt("Entropy", entropy);
    }

    /**
     * Load entropy value from the given tag.
     */
    public void load(CompoundTag tag) {
        if (tag != null && tag.contains("Entropy")) {
            int loaded = tag.getInt("Entropy");
            setEntropy(loaded);
            // Validate after load
            validateState();
        } else {
            // No entropy data - reset to 0
            entropy = 0;
        }
    }

    // ==================== Utility ====================

    /**
     * @return the fill ratio as a float [0.0, 1.0]
     */
    public float getFillRatio() {
        if (capacity <= 0)
            return 0f;
        return (float) entropy / capacity;
    }

    /**
     * @return true if entropy >= capacity
     */
    public boolean isFull() {
        return entropy >= capacity;
    }

    /**
     * @return true if entropy <= 0
     */
    public boolean isEmpty() {
        return entropy <= 0;
    }
}
