package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.PyramidCoreBlockEntity;
import net.nicotfpn.alientech.pyramid.PyramidNetwork;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.nicotfpn.alientech.ui.sync.impl.SyncableLong;

public class PyramidCoreMenu extends AlienContainerMenu {

    public final PyramidCoreBlockEntity blockEntity;

    // Client-synced values
    private long energyStored;
    private long maxEnergy;
    private long entropyAvailable;
    private long entropyCapacity;
    private long isActiveLong;

    public PyramidCoreMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public PyramidCoreMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.PYRAMID_CORE_MENU.get(), containerId, inv.player);
        this.blockEntity = (PyramidCoreBlockEntity) entity;

        // Alloy slot (ISA) — position from GUI spec
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 78, 41));

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // === SyncableLong Trackers ===
        track(SyncableLong.create(
                () -> (long) blockEntity.getEnergyStorage().getEnergyStored(),
                val -> this.energyStored = val));
        track(SyncableLong.create(
                () -> (long) blockEntity.getEnergyStorage().getMaxEnergyStored(),
                val -> this.maxEnergy = val));
        track(SyncableLong.create(
                () -> blockEntity.getLevel() != null
                        ? (long) PyramidNetwork.get(blockEntity.getLevel()).getEntropyAvailable()
                        : 0L,
                val -> this.entropyAvailable = val));
        track(SyncableLong.create(
                () -> blockEntity.getLevel() != null
                        ? (long) PyramidNetwork.get(blockEntity.getLevel()).getNetworkCapacity()
                        : 0L,
                val -> this.entropyCapacity = val));
        track(SyncableLong.create(
                () -> blockEntity.isActive() ? 1L : 0L,
                val -> this.isActiveLong = val));
    }

    // ==================== UI Getters ====================

    public long getEnergyStored() {
        return energyStored;
    }

    public long getMaxEnergy() {
        return maxEnergy;
    }

    public long getEntropy() {
        return entropyAvailable;
    }

    public long getMaxEntropy() {
        return entropyCapacity;
    }

    public boolean isActive() {
        return isActiveLong != 0;
    }

    // ==================== Quick Move ====================

    private static final int TE_SLOT_COUNT = 1;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem())
            return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < TE_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_SLOT_COUNT, TE_SLOT_COUNT + 36, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(sourceStack, 0, TE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty())
            sourceSlot.set(ItemStack.EMPTY);
        else
            sourceSlot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.PYRAMID_CORE.get());
    }

    // ==================== Player Inventory Helpers ====================

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
