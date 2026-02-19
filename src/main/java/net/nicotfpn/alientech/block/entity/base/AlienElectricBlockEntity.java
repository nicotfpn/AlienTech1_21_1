package net.nicotfpn.alientech.block.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.nicotfpn.alientech.util.SyncableEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all electric blocks in AlienTech.
 * Implements robust energy handling, separated sync channels, and clean
 * persistence.
 *
 * Sync Architecture:
 * - Energy sync: Throttled (every ENERGY_SYNC_INTERVAL ticks) to reduce network
 * load.
 * - Inventory sync: Immediate (next tick) for instant visual feedback on item
 * changes.
 *
 * Subclasses should call {@link #markEnergyDirty()} when energy changes, and
 * {@link #markInventoryDirty()} when inventory contents change.
 */
public abstract class AlienElectricBlockEntity extends AlienBlockEntity {

    protected final SyncableEnergyStorage energyStorage;

    // --- Sync Channels ---
    private boolean needsEnergySync = false;
    private int energySyncCooldown = 0;
    private static final int ENERGY_SYNC_INTERVAL = 10; // 0.5s between energy syncs

    private boolean needsInventorySync = false;
    // Inventory sync fires on the NEXT tick (no cooldown) for immediate feedback.

    public AlienElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int capacity,
            int maxTransfer) {
        this(type, pos, blockState, capacity, maxTransfer, maxTransfer);
    }

    public AlienElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int capacity,
            int maxReceive, int maxExtract) {
        super(type, pos, blockState);
        this.energyStorage = new SyncableEnergyStorage(capacity, maxReceive, maxExtract, () -> {
            setChanged();
            markEnergyDirty();
        });
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

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEnergyStorage getEnergyStorage(@Nullable net.minecraft.core.Direction side) {
        return energyStorage;
    }

    // ==================== Sync Logic ====================

    /**
     * Marks energy data as needing synchronization.
     * Will be sent on the next available energy sync window (throttled).
     */
    protected void markEnergyDirty() {
        this.needsEnergySync = true;
    }

    /**
     * Marks inventory data as needing synchronization.
     * Will be sent on the NEXT server tick for immediate visual feedback.
     * Use this in ItemStackHandler#onContentsChanged().
     */
    protected void markInventoryDirty() {
        this.needsInventorySync = true;
    }

    /**
     * Legacy method for backward compatibility.
     * Marks both energy and inventory for sync.
     */
    protected void markForSync() {
        this.needsEnergySync = true;
        this.needsInventorySync = true;
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();

        boolean didSync = false;

        // Channel 1: Inventory sync — immediate (next tick)
        if (needsInventorySync) {
            sendBlockUpdate();
            needsInventorySync = false;
            didSync = true;
            // Reset energy cooldown since we just synced everything
            needsEnergySync = false;
            energySyncCooldown = ENERGY_SYNC_INTERVAL;
        }

        // Channel 2: Energy sync — throttled
        if (energySyncCooldown > 0) {
            energySyncCooldown--;
        }
        if (needsEnergySync && energySyncCooldown <= 0 && !didSync) {
            sendBlockUpdate();
            needsEnergySync = false;
            energySyncCooldown = ENERGY_SYNC_INTERVAL;
        }
    }

    /**
     * Sends a block update packet to sync data to clients.
     */
    private void sendBlockUpdate() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
