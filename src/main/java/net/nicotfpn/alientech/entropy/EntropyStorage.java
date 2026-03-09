package net.nicotfpn.alientech.entropy;

import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.util.AlienTechDebug;

/**
 * Concrete implementation of {@link IEntropyHandler}.
 * <p>
 * Stores entropy with configurable capacity, insert/extract limits, and access
 * mode. All values use {@code long} to prevent overflow at high tiers.
 * All operations are clamped — no overflow, no underflow, no negative values.
 * <p>
 * NBT key: "Entropy" (long).
 */
public class EntropyStorage implements IEntropyHandler {

    private long entropy;
    private final long capacity;
    private final long maxInsert;
    private final long maxExtract;
    private final boolean allowInsert;
    private final boolean allowExtract;
    private final Runnable onChanged;

    /**
     * Full constructor with fine-grained control.
     */
    public EntropyStorage(long capacity, long maxInsert, long maxExtract,
            boolean canInsert, boolean canExtract, Runnable onChanged) {
        this.capacity = Math.max(1L, capacity);
        this.maxInsert = Math.max(0L, maxInsert);
        this.maxExtract = Math.max(0L, maxExtract);
        this.allowInsert = canInsert;
        this.allowExtract = canExtract;
        this.onChanged = onChanged != null ? onChanged : () -> {
        };
        this.entropy = 0L;
    }

    /**
     * Convenience constructor for a bidirectional storage with unlimited transfer
     * rates.
     */
    public EntropyStorage(long capacity, Runnable onChanged) {
        this(capacity, 0L, 0L, true, true, onChanged);
    }

    /**
     * Legacy int-capacity constructor for backwards-compat call sites.
     */
    public EntropyStorage(int capacity, Runnable onChanged) {
        this((long) capacity, 0L, 0L, true, true, onChanged);
    }

    /**
     * Legacy full int constructor for backwards-compat call sites.
     */
    public EntropyStorage(int capacity, int maxInsert, int maxExtract,
            boolean canInsert, boolean canExtract, Runnable onChanged) {
        this((long) capacity, (long) maxInsert, (long) maxExtract, canInsert, canExtract, onChanged);
    }

    // ==================== IEntropyHandler ====================

    @Override
    public long getEntropy() {
        return entropy;
    }

    @Override
    public long getMaxEntropy() {
        return capacity;
    }

    @Override
    public long insertEntropy(long amount, boolean simulate) {
        if (!allowInsert || amount <= 0L) {
            return 0L;
        }

        long space = capacity - entropy;
        if (space <= 0L) {
            return 0L;
        }

        long toInsert = Math.min(amount, space);
        if (maxInsert > 0L) {
            toInsert = Math.min(toInsert, maxInsert);
        }
        if (toInsert <= 0L) {
            return 0L;
        }

        if (!simulate) {
            entropy += toInsert;
            validateState();
            onChanged.run();
        }
        return toInsert;
    }

    @Override
    public long extractEntropy(long amount, boolean simulate) {
        if (!allowExtract || amount <= 0L) {
            return 0L;
        }
        if (entropy <= 0L) {
            return 0L;
        }

        long toExtract = Math.min(amount, entropy);
        if (maxExtract > 0L) {
            toExtract = Math.min(toExtract, maxExtract);
        }
        if (toExtract <= 0L || toExtract > entropy) {
            return 0L;
        }

        if (!simulate) {
            entropy -= toExtract;
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
     */
    public void setEntropy(long value) {
        this.entropy = Math.max(0L, Math.min(value, capacity));
    }

    /**
     * Legacy int overload for backwards-compat call sites.
     */
    public void setEntropy(int value) {
        setEntropy((long) value);
    }

    // ==================== NBT Persistence ====================

    /**
     * Save entropy value to the given tag.
     */
    public void save(CompoundTag tag) {
        tag.putLong("Entropy", entropy);
    }

    /**
     * Load entropy value from the given tag. Supports both old int and new long
     * keys.
     */
    public void load(CompoundTag tag) {
        if (tag == null) {
            entropy = 0L;
            return;
        }
        if (tag.contains("Entropy")) {
            // getLong works for both putInt and putLong values
            setEntropy(tag.getLong("Entropy"));
            validateState();
        } else {
            entropy = 0L;
        }
    }

    // ==================== Utility ====================

    /**
     * @return the fill ratio as a float [0.0, 1.0]
     */
    public float getFillRatio() {
        if (capacity <= 0L)
            return 0f;
        return (float) entropy / (float) capacity;
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
        return entropy <= 0L;
    }

    /**
     * Internal state validation — clamps entropy to [0, capacity].
     */
    public void validateState() {
        if (entropy < 0L)
            entropy = 0L;
        if (entropy > capacity)
            entropy = capacity;
    }
}
