package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.AncientChargerBlockEntity;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.nicotfpn.alientech.ui.sync.impl.SyncableLong;

public class AncientChargerMenu extends AlienContainerMenu {

    public final AncientChargerBlockEntity blockEntity;

    // Sync Trackers
    private final SyncableLong energy;
    private final SyncableLong maxEnergy;

    public AncientChargerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AncientChargerMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.ANCIENT_CHARGER_MENU.get(), containerId, inv.player);
        blockEntity = ((AncientChargerBlockEntity) entity);

        // Slot 0: Charge Slot (from alientech_gui_gen.py: centered in machine area,
        // x=91, y=41)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 91, 41));

        this.energy = new SyncableLong(() -> (long) blockEntity.getEnergyStorage().getEnergyStored(), null);
        this.maxEnergy = new SyncableLong(() -> (long) blockEntity.getEnergyStorage().getMaxEnergyStored(), null);

        track(energy);
        track(maxEnergy);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public long getEnergyStored() {
        return energy.get();
    }

    public long getMaxEnergy() {
        return maxEnergy.get();
    }

    public float getEnergyPercentage() {
        long max = getMaxEnergy();
        return max > 0 ? (float) getEnergyStored() / max : 0;
    }

    public int getScaledEnergy(int maxHeight) {
        long e = getEnergyStored();
        long maxE = getMaxEnergy();
        return maxE > 0 ? (int) (e * maxHeight / maxE) : 0;
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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 10 + col * 18, 86 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 10 + col * 18, 144));
        }
    }
}
