package net.nicotfpn.alientech.util;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * SyncableEnergyStorage
 * 
 * An extension of NeoForge's EnergyStorage that provides:
 * 1. A public setter for energy (crucial for syncing/loading NBT without
 * reflection).
 * 2. callback support for onEnergyChanged (usually markDirty/setChanged).
 */
public class SyncableEnergyStorage extends EnergyStorage {

    private final Runnable onEnergyChanged;

    public SyncableEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onEnergyChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onEnergyChanged = onEnergyChanged;
    }

    public SyncableEnergyStorage(int capacity, int maxTransfer, Runnable onEnergyChanged) {
        this(capacity, maxTransfer, maxTransfer, onEnergyChanged);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) {
            if (onEnergyChanged != null)
                onEnergyChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) {
            if (onEnergyChanged != null)
                onEnergyChanged.run();
        }
        return extracted;
    }

    /**
     * Sets the energy directly. Useful for NBT loading and Packet syncing.
     * Clamps to [0, capacity]. Does NOT trigger the onChanged callback
     * to avoid sync loops during load/deserialization.
     * 
     * @param energy The new energy value.
     */
    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, capacity));
    }

    /**
     * Sets the energy directly, bypassing receiveEnergy/extractEnergy limits.
     * Use this for generators (maxReceive=0) that need to restore stored energy
     * from NBT without being blocked by transfer rate limits.
     * Clamps to [0, capacity]. Does NOT trigger the onChanged callback.
     *
     * @param energy The energy value to set.
     */
    public void setEnergyDirectly(int energy) {
        this.energy = Math.max(0, Math.min(energy, capacity));
    }

    // Helper to expose capacity publicly if needed
    public int getCapacity() {
        return this.capacity;
    }
}
