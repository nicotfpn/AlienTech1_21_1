package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;

/**
 * Handles Entropy storage and logic taking 'long' types to prevent overflow.
 */
public class EntropyComponent extends AlienComponent {

    private long entropyStored;
    private final long maxEntropy;

    public EntropyComponent(AlienMachineBlockEntity tile, long maxEntropy) {
        super(tile);
        this.maxEntropy = maxEntropy;
    }

    @Override
    public String getId() {
        return "Entropy";
    }

    @Override
    public boolean isActive() {
        return false; // Assuming it's just a passive storage buffer unless logic is added
    }

    public long getEntropyStored() {
        return entropyStored;
    }

    public long getMaxEntropy() {
        return maxEntropy;
    }

    public void setEntropyStored(long entropyStored) {
        this.entropyStored = Math.max(0, Math.min(entropyStored, this.maxEntropy));
    }

    public void addEntropy(long amount) {
        this.setEntropyStored(this.entropyStored + amount);
    }

    public void consumeEntropy(long amount) {
        this.setEntropyStored(this.entropyStored - amount);
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("EntropyStored", this.entropyStored);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("EntropyStored")) {
            this.entropyStored = tag.getLong("EntropyStored");
        }
    }
}
