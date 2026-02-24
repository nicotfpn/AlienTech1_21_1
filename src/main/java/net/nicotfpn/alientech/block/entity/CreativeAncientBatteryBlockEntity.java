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

    // NeoForge Native 1.20.4+ BlockCapabilityCache array for all 6 faces
    @SuppressWarnings("unchecked")
    private final net.neoforged.neoforge.capabilities.BlockCapabilityCache<IEnergyStorage, Direction>[] energyCaches = new net.neoforged.neoforge.capabilities.BlockCapabilityCache[6];

    public CreativeAncientBatteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CREATIVE_ANCIENT_BATTERY_BE.get(), pos, blockState);
    }

    public void tick() {
        if (level == null || level.isClientSide)
            return;

        // Extreme Optimization: Only push every 10 ticks, staggered mathematically by
        // chunk pos to avoid server-wide spikes
        if ((level.getGameTime() + worldPosition.asLong()) % 10 != 0)
            return;

        // Push massive energy to all 6 sides
        for (Direction dir : Direction.values()) {
            int i = dir.ordinal();

            // Cache resolution
            if (energyCaches[i] == null) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLvl) {
                    energyCaches[i] = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                            Capabilities.EnergyStorage.BLOCK, serverLvl, worldPosition.relative(dir),
                            dir.getOpposite());
                } else {
                    continue;
                }
            }

            IEnergyStorage target = energyCaches[i].getCapability();
            if (target != null && target.canReceive()) {
                // Push 1,000,000 FE per stagger (equivalent to pushing 100k per tick)
                // The receiver will swallow what it can hold.
                target.receiveEnergy(1_000_000, false);
            }
        }
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
