package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;

public abstract class AlienComponent {
    protected final AlienMachineBlockEntity tile;

    public AlienComponent(AlienMachineBlockEntity tile) {
        this.tile = tile;
    }

    /**
     * Unique ID to namespace this component's NBT saves (e.g., "Energy",
     * "Inventory").
     */
    public abstract String getId();

    /**
     * Determines if this component needs to be ticked. Idle components are removed
     * from the active loop.
     */
    public boolean isActive() {
        return true;
    }

    /**
     * Defines how often the component ticks (e.g., 1 for every tick, 20 for once a
     * second).
     */
    public int getTickInterval() {
        return 1;
    }

    /**
     * Notifies the parent tile that this component's active state has changed.
     */
    protected final void markActiveStateDirty() {
        if (this.tile != null) {
            this.tile.updateActiveState(this);
        }
    }

    /**
     * Called when it's time to tick.
     * 
     * @return true if the tile should be marked dirty/changed.
     */
    public boolean tickServer() {
        return false;
    }

    public abstract void save(CompoundTag tag, HolderLookup.Provider provider);

    public abstract void load(CompoundTag tag, HolderLookup.Provider provider);
}
