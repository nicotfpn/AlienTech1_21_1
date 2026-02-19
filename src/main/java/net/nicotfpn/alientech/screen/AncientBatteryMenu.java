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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.entity.AncientBatteryBlockEntity;
import net.nicotfpn.alientech.registration.AlienBlocks;

/**
 * Menu for Ancient Battery.
 * Layout:
 * - Top Slot (Charge/Output): Item receives energy from battery.
 * - Bottom Slot (Discharge/Input): Item gives energy to battery.
 * - Center: Large Energy Bar.
 */
public class AncientBatteryMenu extends AbstractContainerMenu {

    public final AncientBatteryBlockEntity blockEntity;
    private final ContainerData data;

    // Slot indices
    // Slot 0: Charge (Output)
    // Slot 1: Discharge (Input)
    // Player Inventory: 2-28
    // Player Hotbar: 29-37
    private static final int TE_SLOT_COUNT = 2;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36;

    public AncientBatteryMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(5));
    }

    public AncientBatteryMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ANCIENT_BATTERY_MENU.get(), containerId);
        checkContainerSize(inv, 2); // 2 slots now
        blockEntity = ((AncientBatteryBlockEntity) entity);
        this.data = data;

        // Slot positions based on 176x166 GUI
        // Charge Slot (Output - Top): x=26, y=20
        // Discharge Slot (Input - Bottom): x=26, y=50
        // Energy Bar (Center): x=66... (Visual only)

        // Slot 0: Charge Item (Battery -> Item)
        this.addSlot(new EnergySlot(blockEntity.getItemHandler(), 0, 26, 20));

        // Slot 1: Discharge Item (Item -> Battery)
        this.addSlot(new EnergySlot(blockEntity.getItemHandler(), 1, 26, 50));

        addDataSlots(data);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ==================== Energy Data ====================

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
        int max = getMaxEnergy();
        return max > 0 ? (float) getEnergyStored() / max : 0;
    }

    public int getScaledEnergy(int maxHeight) {
        int energy = getEnergyStored();
        int maxEnergy = getMaxEnergy();
        return maxEnergy > 0 ? (int) ((long) energy * maxHeight / maxEnergy) : 0;
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
                // Determine destination
                IEnergyStorage energy = sourceStack.getCapability(Capabilities.EnergyStorage.ITEM);
                if (energy != null) {
                    // Similar to Mekanism:
                    // If item can EXTRACT energy (it has energy to give), it goes to Discharge
                    // (Slot 1).
                    // If item can RECEIVE energy (it needs charge), it goes to Charge (Slot 0).
                    // If it can do both, prioritize based on current state (is it full?).

                    boolean canReceive = energy.canReceive();
                    boolean canExtract = energy.canExtract();
                    boolean hasPower = energy.getEnergyStored() > 0;
                    boolean isFull = energy.getEnergyStored() == energy.getMaxEnergyStored();

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
                        // Default to Discharge (Input) if unclear? Or Charge?
                        // Let's try Discharge first (1), then Charge (0).
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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
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
