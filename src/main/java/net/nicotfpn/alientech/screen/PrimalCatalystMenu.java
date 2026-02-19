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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.PrimalCatalystBlockEntity;

/**
 * Container menu for the Primal Catalyst.
 *
 * Slot layout (5 machine slots + 36 player slots):
 * 0, 1, 2 = Input slots (top row)
 * 3 = Fuel slot (middle-left, coal_block only)
 * 4 = Output slot (right side)
 *
 * Data sync (6 values):
 * 0 = progress, 1 = maxProgress
 * 2 = burnTime, 3 = maxBurnTime
 * 4 = energy, 5 = maxEnergy
 */
public class PrimalCatalystMenu extends AbstractContainerMenu {

    public final PrimalCatalystBlockEntity blockEntity;
    private final ContainerData data;

    // ==================== Slot Layout Constants ====================
    private static final int TE_SLOT_COUNT = 5;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36; // 27 inventory + 9 hotbar

    // ==================== Constructor (Network) ====================

    public PrimalCatalystMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(6));
    }

    // ==================== Constructor (Server) ====================

    public PrimalCatalystMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PRIMAL_CATALYST_MENU.get(), containerId);
        blockEntity = (PrimalCatalystBlockEntity) entity;
        this.data = data;

        // Input slots: top row (3 slots)
        this.addSlot(new SlotItemHandler(blockEntity.getInventory().getHandler(), 0, 30, 17)); // Input 1
        this.addSlot(new SlotItemHandler(blockEntity.getInventory().getHandler(), 1, 48, 17)); // Input 2
        this.addSlot(new SlotItemHandler(blockEntity.getInventory().getHandler(), 2, 66, 17)); // Input 3

        // Fuel slot: middle-left
        this.addSlot(new FuelSlot(blockEntity.getInventory().getHandler(), 3, 30, 53));

        // Output slot: right side
        this.addSlot(new OutputSlot(blockEntity.getInventory().getHandler(), 4, 124, 35));

        // Sync all 6 data values
        addDataSlots(data);

        // Player inventory and hotbar
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ==================== Data Accessors ====================

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int arrowWidth = 26; // pixel width of progress arrow
        return maxProgress != 0 && progress != 0 ? progress * arrowWidth / maxProgress : 0;
    }

    public boolean isBurning() {
        return data.get(2) > 0;
    }

    public int getScaledBurnTime() {
        int burnTime = data.get(2);
        int maxBurnTime = data.get(3);
        int flameHeight = 14; // pixel height of flame icon
        return maxBurnTime != 0 && burnTime != 0 ? burnTime * flameHeight / maxBurnTime : 0;
    }

    public int getScaledEnergy() {
        int energy = data.get(4);
        int maxEnergy = data.get(5);
        int barHeight = 52; // pixel height of energy bar
        return maxEnergy != 0 && energy != 0 ? energy * barHeight / maxEnergy : 0;
    }

    public int getEnergy() {
        return data.get(4);
    }

    public int getMaxEnergy() {
        return data.get(5);
    }

    // ==================== Shift-Click Transfer ====================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem())
            return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX) {
            // Moving FROM machine TO player
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX,
                    VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Moving FROM player TO machine
            // Try fuel slot first if it's a coal block
            if (sourceStack.is(Items.COAL_BLOCK)) {
                if (!moveItemStackTo(sourceStack, 3, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try input slots
                if (!moveItemStackTo(sourceStack, 0, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.PRIMAL_CATALYST.get());
    }

    // ==================== Player Inventory Setup ====================

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

    // ==================== Custom Slot Types ====================

    /**
     * Output slot: cannot place items into it manually.
     */
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(net.neoforged.neoforge.items.IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /**
     * Fuel slot: only accepts valid fuel items (coal_block).
     */
    private static class FuelSlot extends SlotItemHandler {
        public FuelSlot(net.neoforged.neoforge.items.IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.COAL_BLOCK);
        }
    }
}