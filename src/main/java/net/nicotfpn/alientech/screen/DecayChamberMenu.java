package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
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
import net.nicotfpn.alientech.machine.decay.DecayChamberControllerBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for the Decay Chamber Controller.
 * <p>
 * Slot layout (3 machine slots + 36 player slots):
 * 0 = Input slot
 * 1 = Fuel slot (coal_block only)
 * 2 = Output slot
 * <p>
 * Data sync (5 values):
 * 0 = decayProgress, 1 = decayMaxProgress
 * 2 = entropy (low), 3 = entropy (high)
 * 4 = maxEntropy (low), 5 = maxEntropy (high)
 * 6 = hasMob (0 or 1)
 */
public class DecayChamberMenu extends AbstractContainerMenu {

    public final DecayChamberControllerBlockEntity blockEntity;
    private final ContainerData data;

    // ==================== Slot Layout Constants ====================
    private static final int TE_SLOT_COUNT = 3;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36; // 27 inventory + 9 hotbar

    // ==================== Constructor (Network / Client) ====================

    public DecayChamberMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(12));
    }

    // ==================== Constructor (Server) ====================

    public DecayChamberMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.DECAY_CHAMBER_MENU.get(), containerId);
        this.blockEntity = (DecayChamberControllerBlockEntity) entity;
        this.data = data;

        // Input slot (draw at 56,34 -> item pos +1)
        this.addSlot(new SlotItemHandler(blockEntity.getOutputInventory(), 0, 58, 36));

        // Fuel slot: under energy bar (draw at 8,54 -> item pos +1)
        this.addSlot(new FuelSlot(blockEntity.getOutputInventory(), 1, 10, 56));

        // Output slot: right side (draw at 120,33 -> item pos +1)
        this.addSlot(new OutputSlot(blockEntity.getOutputInventory(), 2, 122, 35));

        // Sync all 5 data values
        addDataSlots(data);

        // Player inventory and hotbar
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ==================== Data Accessors ====================

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int barWidth = 34; // pixel width of progress bar (from create_decay_chamber_gui.py)
        return maxProgress != 0 && progress != 0 ? progress * barWidth / maxProgress : 0;
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getEntropy() {
        return net.nicotfpn.alientech.util.EnergyUtils.fromBits(data.get(2), data.get(3));
    }

    public int getMaxEntropy() {
        return net.nicotfpn.alientech.util.EnergyUtils.fromBits(data.get(4), data.get(5));
    }

    public int getScaledEnergy() {
        int entropy = getEntropy();
        int maxEntropy = getMaxEntropy();
        int barHeight = 44; // pixel height of energy bar (from create_decay_chamber_gui.py)
        return maxEntropy != 0 && entropy != 0 ? (int) ((long) entropy * barHeight / maxEntropy) : 0;
    }

    public boolean hasMob() {
        return data.get(6) != 0;
    }

    /**
     * Get the display entity for mob silhouette rendering.
     * Only valid on the client side when the blockEntity reference is live.
     */
    @Nullable
    public LivingEntity getEntityForDisplay() {
        if (blockEntity != null) {
            return blockEntity.getEntityForDisplay();
        }
        return null;
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
                if (!moveItemStackTo(sourceStack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try input slot
                if (!moveItemStackTo(sourceStack, 0, 1, false)) {
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
                player, ModBlocks.DECAY_CHAMBER_CONTROLLER.get());
    }

    // ==================== Player Inventory Setup ====================

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 10 + l * 18, 86 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 10 + i * 18, 144));
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
