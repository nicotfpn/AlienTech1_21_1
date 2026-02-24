package net.nicotfpn.alientech.block.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;

public abstract class AlienBlockEntity extends BlockEntity implements MenuProvider {

    public int ticker;
    protected boolean isRemote;

    public AlienBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static void tickClient(Level level, BlockPos pos, BlockState state, AlienBlockEntity tile) {
        // Validate inputs (mirror tickServer safety)
        if (level == null || pos == null || state == null || tile == null) {
            return;
        }
        if (!level.isClientSide) {
            return;
        }

        tile.isRemote = true;
        tile.onUpdateClient();
        // Prevent ticker overflow (resets after ~3.4 years of continuous ticking)
        if (tile.ticker >= Integer.MAX_VALUE - 1) {
            tile.ticker = 0;
        } else {
            tile.ticker++;
        }
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, AlienBlockEntity tile) {
        // Validate inputs
        if (level == null || pos == null || state == null || tile == null) {
            return;
        }

        // Only tick on server side
        if (level.isClientSide) {
            return;
        }

        tile.isRemote = false;
        tile.onUpdateServer();
        // Prevent ticker overflow (resets after ~3.4 years of continuous ticking)
        if (tile.ticker >= Integer.MAX_VALUE - 1) {
            tile.ticker = 0;
        } else {
            tile.ticker++;
        }
    }

    protected void onUpdateClient() {
    }

    protected void onUpdateServer() {
    }

    @Override
    public abstract Component getDisplayName();

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player);

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }

    /**
     * Called when the block entity is loaded from NBT or when the chunk loads.
     * Override to perform initialization or state validation.
     */
    public void onLoad() {
        // Override in subclasses if needed
    }

    /**
     * Called when the block entity is removed.
     * Override to clean up resources or cached references.
     */
    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clear any cached references
        // Override in subclasses if needed
    }
    
    /**
     * Convenience helper to request a full block update / sync to clients.
     * Some subclasses call this when multiple synced channels change.
     */
    protected void markForSync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}
