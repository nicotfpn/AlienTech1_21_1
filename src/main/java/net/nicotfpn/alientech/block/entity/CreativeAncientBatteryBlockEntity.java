package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class CreativeAncientBatteryBlockEntity extends BlockEntity {

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return maxExtract;
        }

        @Override
        public int getEnergyStored() {
            return 1_000_000_000;
        }

        @Override
        public int getMaxEnergyStored() {
            return 1_000_000_000;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public CreativeAncientBatteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CREATIVE_ANCIENT_BATTERY_BE.get(), pos, blockState);
    }

    public void tick() {
        if (level == null || level.isClientSide)
            return;

        // Push energy to all 6 sides
        for (Direction dir : Direction.values()) {
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir),
                    dir.getOpposite());
            if (target != null && target.canReceive()) {
                target.receiveEnergy(100_000, false); // Pushing 100k FE per tick per side
            }
        }
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
