package net.nicotfpn.alientech.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Energy storage implementation for items using CustomData component.
 * Stores energy in the item's NBT data via DataComponents.
 *
 * Robustness guarantees:
 * - Null-safe: Checks stack validity before any operation.
 * - Bounds-safe: Energy is always clamped to [0, capacity].
 * - Thread-safe reads: getStoredEnergy() creates a copy of the tag.
 */
public class ItemEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;
    private static final String ENERGY_KEY = "Energy";

    public ItemEnergyStorage(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
        this.stack = stack;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    private int getStoredEnergy() {
        if (stack.isEmpty())
            return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getInt(ENERGY_KEY);
        }
        return 0;
    }

    private void setStoredEnergy(int energy) {
        if (stack.isEmpty())
            return;
        int clamped = Math.max(0, Math.min(capacity, energy));
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt(ENERGY_KEY, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!canReceive() || toReceive <= 0 || stack.isEmpty())
            return 0;

        int stored = getStoredEnergy();
        int space = capacity - stored;
        if (space <= 0)
            return 0;

        int energyReceived = Math.min(space, Math.min(maxReceive, toReceive));

        if (!simulate) {
            setStoredEnergy(stored + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (!canExtract() || toExtract <= 0 || stack.isEmpty())
            return 0;

        int stored = getStoredEnergy();
        if (stored <= 0)
            return 0;

        int energyExtracted = Math.min(stored, Math.min(maxExtract, toExtract));

        if (!simulate) {
            setStoredEnergy(stored - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return getStoredEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
}
