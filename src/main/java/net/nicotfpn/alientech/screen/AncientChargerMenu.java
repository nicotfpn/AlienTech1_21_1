package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.AncientChargerBlockEntity;

public class AncientChargerMenu extends AbstractContainerMenu {

    public final AncientChargerBlockEntity blockEntity;
    private final ContainerData data;

    public AncientChargerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(4));
    }

    public AncientChargerMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ANCIENT_CHARGER_MENU.get(), containerId);
        checkContainerSize(inv, 1);
        blockEntity = ((AncientChargerBlockEntity) entity);
        this.data = data;

        // Slot 0: Charge Slot
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 80, 35));

        addDataSlots(data);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public int getEnergyStored() {
        int low = data.get(0) & 0xFFFF;
        int high = data.get(1) & 0xFFFF;
        return low | (high << 16);
    }

    public int getMaxEnergy() {
        int low = data.get(2) & 0xFFFF;
        int high = data.get(3) & 0xFFFF;
        return low | (high << 16);
    }

    public float getEnergyPercentage() {
        long max = getMaxEnergy();
        return max > 0 ? (float) getEnergyStored() / max : 0;
    }

    public int getScaledEnergy(int maxHeight) {
        int energy = getEnergyStored();
        int maxEnergy = getMaxEnergy();
        return maxEnergy > 0 ? (int) ((long) energy * maxHeight / maxEnergy) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem())
            return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // TE Slot is index 0 (added first)
        // Player Inv: index 1-27
        // Player Hotbar: index 28-36

        if (index == 0) {
            // TE -> Player (try inventory first, then hotbar)
            if (!moveItemStackTo(sourceStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player -> TE (only if item has energy capability)
            if (hasEnergyCapability(sourceStack)) {
                if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    private boolean hasEnergyCapability(ItemStack stack) {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.ANCIENT_CHARGER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
}
