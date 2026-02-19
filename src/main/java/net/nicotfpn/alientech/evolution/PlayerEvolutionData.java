package net.nicotfpn.alientech.evolution;

import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.util.StateValidator;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.AlienTechDebug;

/**
 * Concrete implementation of {@link IEvolutionData}.
 * <p>
 * Stores evolution stage, entropy capacity, and current entropy.
 * Used as a NeoForge data attachment on players.
 * <p>
 * All entropy operations clamp to [0, capacity].
 */
public class PlayerEvolutionData implements IEvolutionData {

    private static final String TAG_STAGE = "EvolutionStage";
    private static final String TAG_CAPACITY = "EntropyCapacity";
    private static final String TAG_STORED = "StoredEntropy";

    private static final int DEFAULT_STAGE = 0;
    private static final int DEFAULT_CAPACITY = 1000;
    private static final int DEFAULT_STORED = 0;

    private int evolutionStage;
    private int entropyCapacity;
    private int storedEntropy;

    /** Default constructor — creates baseline evolution state. */
    public PlayerEvolutionData() {
        this.evolutionStage = DEFAULT_STAGE;
        this.entropyCapacity = DEFAULT_CAPACITY;
        this.storedEntropy = DEFAULT_STORED;
    }

    // ==================== Evolution Stage ====================

    @Override
    public int getEvolutionStage() {
        return evolutionStage;
    }

    @Override
    public void setEvolutionStage(int stage) {
        this.evolutionStage = Math.max(0, stage);
    }

    // ==================== Entropy Capacity ====================

    @Override
    public int getEntropyCapacity() {
        return entropyCapacity;
    }

    @Override
    public void setEntropyCapacity(int capacity) {
        this.entropyCapacity = Math.max(0, capacity);
        // Clamp stored if capacity shrank
        if (storedEntropy > entropyCapacity) {
            storedEntropy = entropyCapacity;
        }
    }

    // ==================== Stored Entropy ====================

    @Override
    public int getStoredEntropy() {
        return storedEntropy;
    }

    @Override
    public void setStoredEntropy(int entropy) {
        this.storedEntropy = StateValidator.clampEntropy(entropy, entropyCapacity);
    }

    /**
     * Internal state validation method.
     * Ensures all values are within valid ranges.
     * 
     * @return true if state was valid, false if correction was needed
     */
    public boolean validateState() {
        boolean valid = true;
        
        // Validate evolution stage
        int oldStage = evolutionStage;
        evolutionStage = StateValidator.ensureNonNegative(evolutionStage);
        if (oldStage != evolutionStage) {
            AlienTechDebug.EVOLUTION.log("Evolution stage corrected: {} -> {}", oldStage, evolutionStage);
            valid = false;
        }
        
        // Validate capacity
        int oldCapacity = entropyCapacity;
        entropyCapacity = Math.max(DEFAULT_CAPACITY, entropyCapacity);
        if (oldCapacity != entropyCapacity) {
            AlienTechDebug.EVOLUTION.log("Entropy capacity corrected: {} -> {}", oldCapacity, entropyCapacity);
            valid = false;
        }
        
        // Validate stored entropy
        int oldStored = storedEntropy;
        storedEntropy = StateValidator.clampEntropy(storedEntropy, entropyCapacity);
        if (oldStored != storedEntropy) {
            AlienTechDebug.EVOLUTION.log("Stored entropy corrected: {} -> {} (capacity: {})", 
                    oldStored, storedEntropy, entropyCapacity);
            valid = false;
        }
        
        return valid;
    }

    @Override
    public boolean canAcceptEntropy() {
        return storedEntropy < entropyCapacity;
    }

    @Override
    public int insertEntropy(int amount) {
        if (amount <= 0)
            return 0;
        int space = entropyCapacity - storedEntropy;
        int toInsert = Math.min(amount, space);
        storedEntropy += toInsert;
        
        // Validate state after mutation
        validateState();
        
        AlienTechDebug.EVOLUTION.log("Player inserted {} entropy (total: {}/{})", toInsert, storedEntropy, entropyCapacity);
        return toInsert;
    }

    @Override
    public int extractEntropy(int amount) {
        if (amount <= 0)
            return 0;
        int toExtract = Math.min(amount, storedEntropy);
        storedEntropy -= toExtract;
        
        // Validate state after mutation
        validateState();
        
        AlienTechDebug.EVOLUTION.log("Player extracted {} entropy (remaining: {}/{})", toExtract, storedEntropy, entropyCapacity);
        return toExtract;
    }

    // ==================== Serialization ====================

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_STAGE, evolutionStage);
        tag.putInt(TAG_CAPACITY, entropyCapacity);
        tag.putInt(TAG_STORED, storedEntropy);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) {
            // Invalid tag - use defaults
            this.evolutionStage = DEFAULT_STAGE;
            this.entropyCapacity = DEFAULT_CAPACITY;
            this.storedEntropy = DEFAULT_STORED;
            return;
        }

        // Load with safe defaults
        this.evolutionStage = StateValidator.ensureNonNegative(SafeNBT.getInt(tag, TAG_STAGE, DEFAULT_STAGE));
        this.entropyCapacity = Math.max(DEFAULT_CAPACITY, SafeNBT.getInt(tag, TAG_CAPACITY, DEFAULT_CAPACITY));
        this.storedEntropy = SafeNBT.getInt(tag, TAG_STORED, DEFAULT_STORED);
        
        // Validate state after load
        validateState();
    }

    @Override
    public String toString() {
        return "PlayerEvolutionData{stage=" + evolutionStage +
                ", capacity=" + entropyCapacity +
                ", stored=" + storedEntropy + "}";
    }
}
