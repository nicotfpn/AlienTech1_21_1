package net.nicotfpn.alientech.machine.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.nicotfpn.alientech.util.SyncableEnergyStorage;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Encapsulates all energy and fuel logic for a machine.
 * Supports a hybrid power system: FE energy (priority) + coal block fuel
 * (fallback).
 *
 * NBT keys preserved: "Energy", "BurnTime", "MaxBurnTime"
 */
public class MachineEnergy {

    private final SyncableEnergyStorage storage;
    private int burnTime = 0;
    private int maxBurnTime = 0;

    /**
     * @param capacity   maximum FE storage
     * @param maxReceive maximum FE received per tick
     * @param maxExtract maximum FE extracted per tick (0 for machines that don't
     *                   export)
     * @param onChanged  callback for dirty marking when energy changes
     */
    public MachineEnergy(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        this.storage = new SyncableEnergyStorage(capacity, maxReceive, maxExtract, onChanged);
    }

    // ==================== Energy Accessors ====================

    public SyncableEnergyStorage getStorage() {
        return storage;
    }

    public IEnergyStorage getEnergyStorage() {
        return storage;
    }

    public int getEnergyStored() {
        return storage.getEnergyStored();
    }

    public int getCapacity() {
        return storage.getMaxEnergyStored();
    }

    // ==================== Fuel Accessors ====================

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    // ==================== Fuel Setters (for client-side ContainerData sync)
    // ====================

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public void setMaxBurnTime(int maxBurnTime) {
        this.maxBurnTime = maxBurnTime;
    }

    // ==================== Power System ====================

    /**
     * Check if the machine currently has power: enough FE energy OR active fuel
     * burn.
     */
    public boolean hasPower(int cost) {
        return storage.getEnergyStored() >= cost || burnTime > 0;
    }

    /**
     * Consume power with priority: FE energy first. If insufficient FE,
     * fuel burn time is ticking down passively (no additional consumption needed).
     */
    public void consumePower(int cost) {
        if (storage.getEnergyStored() >= cost) {
            storage.extractEnergy(cost, false);
        }
    }

    /**
     * Try to consume a fuel item from the fuel slot to start burning.
     * Called by MachineTicker only when: no burn time, insufficient energy, and
     * recipe available.
     *
     * @param inventory        the machine inventory
     * @param fuelSlot         slot index for fuel
     * @param fuelValidator    predicate to check if an item is valid fuel
     * @param burnTimeFunction function returning burn time in ticks for a fuel item
     */
    public void tryConsumeFuel(MachineInventory inventory, int fuelSlot,
            Predicate<ItemStack> fuelValidator,
            ToIntFunction<ItemStack> burnTimeFunction) {
        ItemStack fuelStack = inventory.getHandler().getStackInSlot(fuelSlot);
        if (!fuelStack.isEmpty() && fuelValidator.test(fuelStack)) {
            int fuelTime = burnTimeFunction.applyAsInt(fuelStack);
            if (fuelTime > 0) {
                burnTime = fuelTime;
                maxBurnTime = fuelTime;
                inventory.getHandler().extractItem(fuelSlot, 1, false);
            }
        }
    }

    /**
     * Tick down active fuel burn time. Called once per server tick.
     */
    public void tickBurnTime() {
        if (burnTime > 0) {
            burnTime--;
        }
    }

    // ==================== NBT Persistence ====================

    public void save(CompoundTag tag) {
        tag.putInt("Energy", storage.getEnergyStored());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Energy")) {
            storage.setEnergy(tag.getInt("Energy"));
        }
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
    }
}
