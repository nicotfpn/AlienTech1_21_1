package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;

/**
 * Handles Forge Energy (FE) buffering for a machine.
 */
public class EnergyComponent extends AlienComponent {
    private final AlienEnergyStorage energyStorage;

    public EnergyComponent(AlienMachineBlockEntity tile, int capacity, int maxReceive, int maxExtract) {
        super(tile);
        this.energyStorage = new AlienEnergyStorage(capacity, maxReceive, maxExtract);
    }

    @Override
    public String getId() {
        return "Energy";
    }

    public AlienEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public boolean isActive() {
        return false; // Typically, mere storage doesn't need to tick
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("Energy")) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
    }
}
