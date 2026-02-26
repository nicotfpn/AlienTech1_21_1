package net.nicotfpn.alientech.machine.core.component;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Custom EnergyStorage to expose energy manipulation for NBT saving/loading and
 * component delegation.
 */
public class AlienEnergyStorage extends EnergyStorage {

    public AlienEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, this.capacity));
    }
}
