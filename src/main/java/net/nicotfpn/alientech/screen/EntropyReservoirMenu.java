package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.machine.entropy.EntropyReservoirBlockEntity;
import net.nicotfpn.alientech.network.AlienEnergyNetwork;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.nicotfpn.alientech.ui.sync.impl.SyncableLong;

public class EntropyReservoirMenu extends AlienContainerMenu {

    public final EntropyReservoirBlockEntity blockEntity;

    private long entropyStored;
    private long instabilityLevel;

    public EntropyReservoirMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public EntropyReservoirMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.ENTROPY_RESERVOIR_MENU.get(), containerId, inv.player);
        this.blockEntity = (EntropyReservoirBlockEntity) entity;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        track(SyncableLong.create(
                () -> {
                    AlienEnergyNetwork net = blockEntity.getConnectedNetwork();
                    return net != null ? net.getEntropyStored() : blockEntity.entropyComponent.getEntropyStored();
                },
                val -> this.entropyStored = val));

        track(SyncableLong.create(
                () -> {
                    AlienEnergyNetwork net = blockEntity.getConnectedNetwork();
                    return net != null ? net.getInstabilityLevel() : 0L;
                },
                val -> this.instabilityLevel = val));
    }

    public long getEntropyStored() {
        return entropyStored;
    }

    public long getInstabilityLevel() {
        return instabilityLevel;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem())
            return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < 36) {
            if (!moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.ENTROPY_RESERVOIR.get());
    }

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
