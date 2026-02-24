package net.nicotfpn.alientech.machine.core;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.nicotfpn.alientech.pyramid.PyramidNetwork;
import net.nicotfpn.alientech.pyramid.PyramidTier;

/**
 * Wrapper for IEnergyStorage that restricts receiveEnergy based on Pyramid tier.
 * Always exposes the capability but blocks insertion when tier == TIER_1.
 */
public class TierRestrictedEnergyStorage implements IEnergyStorage {

    private final IEnergyStorage delegate;
    private final Level level;

    public TierRestrictedEnergyStorage(IEnergyStorage delegate, Level level) {
        this.delegate = delegate;
        this.level = level;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        try {
            if (level != null && PyramidNetwork.get(level).getTier() == PyramidTier.TIER_1) {
                return 0;
            }
        } catch (Exception e) {
            // Fail open (allow) in case of unexpected errors to avoid locking players out
        }
        return delegate.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return delegate.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return delegate.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public boolean canReceive() {
        try {
            if (level != null && PyramidNetwork.get(level).getTier() == PyramidTier.TIER_1) {
                return false;
            }
        } catch (Exception e) {
            // fail open
        }
        return delegate.canReceive();
    }

    @Override
    public boolean canExtract() {
        return delegate.canExtract();
    }
}
