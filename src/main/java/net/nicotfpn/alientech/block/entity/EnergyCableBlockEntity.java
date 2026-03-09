package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.nicotfpn.alientech.block.custom.EnergyCableBlock;
import net.nicotfpn.alientech.util.CapabilityUtils;
import net.nicotfpn.alientech.util.EnergyUtils;
import net.nicotfpn.alientech.util.SyncableEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Energy Cable Block Entity — transports Forge Energy (FE) between adjacent
 * blocks.
 * <p>
 * Uses a small internal buffer to allow energy to flow through chains of
 * cables.
 */
public class EnergyCableBlockEntity extends BlockEntity {

    private static final int TICK_INTERVAL = 5;
    private int tickCount = 0;

    private final SyncableEnergyStorage energyStorage;

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE_BE.get(), pos, state);

        int baseRate = getBaseRateForBlock(state.getBlock());
        int maxTransfer = Math.max(1, baseRate);
        int capacity = maxTransfer * 4; // small buffer to smooth transfer across multiple segments
        this.energyStorage = new SyncableEnergyStorage(capacity, maxTransfer, maxTransfer, this::setChanged);
    }

    private static int getBaseRateForBlock(net.minecraft.world.level.block.Block block) {
        if (block instanceof EnergyCableBlock energyCableBlock) {
            return energyCableBlock.getTransferRate();
        }
        // Fallback
        return 1000;
    }

    public void serverTick() {
        tickCount++;
        if (!CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        // Throttle: only process every TICK_INTERVAL ticks
        if (tickCount % TICK_INTERVAL != 0) {
            return;
        }

        int transferRate = getBaseRateForBlock(getBlockState().getBlock()) * TICK_INTERVAL;
        if (transferRate <= 0) {
            return;
        }

        transferEnergy(transferRate);
    }

    private void transferEnergy(int transferRate) {
        if (transferRate <= 0 || level == null) {
            return;
        }

        // Pull energy from neighbors into local buffer
        for (Direction dir : Direction.values()) {
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (neighbor == null || !neighbor.canExtract()) {
                continue;
            }
            EnergyUtils.pullEnergy(energyStorage, neighbor, transferRate);
        }

        // Push energy from local buffer to neighbors
        for (Direction dir : Direction.values()) {
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (neighbor == null || !neighbor.canReceive()) {
                continue;
            }
            EnergyUtils.pushEnergy(energyStorage, neighbor, transferRate);
        }
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Energy")) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}
