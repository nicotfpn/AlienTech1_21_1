package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.entity.AncientBatteryBlockEntity;
import net.nicotfpn.alientech.registration.AlienBlocks;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.nicotfpn.alientech.ui.sync.impl.SyncableLong;

/**
 * Menu for Ancient Battery.
 * Layout:
 * - Top Slot (Charge/Output): Item receives energy from battery.
 * - Bottom Slot (Discharge/Input): Item gives energy to battery.
 * - Center: Large Energy Bar.
 */
public class AncientBatteryMenu extends AlienContainerMenu {

    public final AncientBatteryBlockEntity blockEntity;

    // Sync Trackers
    private final SyncableLong energy;
    private final SyncableLong maxEnergy;

    // Slot indices
    // Slot 0: Charge (Output)
    // Slot 1: Discharge (Input)
    // Player Inventory: 2-28
    // Player Hotbar: 29-37
    private static final int TE_SLOT_COUNT = 2;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36;

    public AncientBatteryMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AncientBatteryMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.ANCIENT_BATTERY_MENU.get(), containerId, inv.player);
        blockEntity = ((AncientBatteryBlockEntity) entity);

        // Slot positions from alientech_gui_gen.py: x=79/103, y=63
        // Slot 0: Charge Item (Battery -> Item)
        this.addSlot(new EnergySlot(blockEntity.getItemHandler(), 0, 79, 63));

        // Slot 1: Discharge Item (Item -> Battery)
        this.addSlot(new EnergySlot(blockEntity.getItemHandler(), 1, 103, 63));

        this.energy = new SyncableLong(() -> (long) blockEntity.getEnergyStorage().getEnergyStored(), null);
        this.maxEnergy = new SyncableLong(() -> (long) blockEntity.getEnergyStorage().getMaxEnergyStored(), null);

        track(energy);
        track(maxEnergy);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ==================== Energy Data ====================

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

    // ==================== Shift-Click ====================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX) {
            // From TE (0 or 1) to player inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,
                    true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From player to TE
            if (hasEnergyCapability(sourceStack)) {
                IEnergyStorage energyCap = sourceStack.getCapability(Capabilities.EnergyStorage.ITEM);
                if (energyCap != null) {
                    boolean canReceive = energyCap.canReceive();
                    boolean canExtract = energyCap.canExtract();
                    boolean hasPower = energyCap.getEnergyStored() > 0;
                    boolean isFull = energyCap.getEnergyStored() == energyCap.getMaxEnergyStored();

                    if (canExtract && hasPower) {
                        // Has power to give -> Discharge Slot (1)
                        if (!moveItemStackTo(sourceStack, 1, 2, false)) {
                            // If full or fail, maybe try charge?
                            if (canReceive && !moveItemStackTo(sourceStack, 0, 1, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    } else if (canReceive && !isFull) {
                        // Needs power -> Charge Slot (0)
                        if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // Default to Discharge (Input) then Charge (Output)
                        if (!moveItemStackTo(sourceStack, 1, 2, false)) {
                            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
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
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return energy != null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, AlienBlocks.ANCIENT_BATTERY.get());
    }

    // ==================== Player Inventory ====================

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

    // ==================== Energy Slot ====================

    private static class EnergySlot extends SlotItemHandler {
        public EnergySlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            return energy != null;
        }
    }
}
