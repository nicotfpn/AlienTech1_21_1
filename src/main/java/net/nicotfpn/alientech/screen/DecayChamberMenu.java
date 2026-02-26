package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.machine.decay.DecayChamberControllerBlockEntity;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.nicotfpn.alientech.ui.sync.impl.SyncableLong;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for the Decay Chamber Controller.
 * Upgraded to AlienContainerMenu to natively support VarLong payload
 * compression.
 */
public class DecayChamberMenu extends AlienContainerMenu {

    public final DecayChamberControllerBlockEntity blockEntity;

    // ==================== Slot Layout Constants ====================
    private static final int TE_SLOT_COUNT = 3;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36; // 27 inventory + 9 hotbar

    // ==================== Local UI Values ====================
    private int progress;
    private int maxProgress;
    private long entropy;
    private long maxEntropy;
    private boolean hasMob;

    // ==================== Constructor (Network / Client) ====================

    public DecayChamberMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // ==================== Constructor (Server) ====================

    public DecayChamberMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.DECAY_CHAMBER_MENU.get(), containerId, inv.player);
        this.blockEntity = (DecayChamberControllerBlockEntity) entity;

        this.addSlot(new SlotItemHandler(blockEntity.getOutputInventory(), 0, 66, 41));
        this.addSlot(new FuelSlot(blockEntity.getOutputInventory(), 1, 10, 56));
        this.addSlot(new OutputSlot(blockEntity.getOutputInventory(), 2, 116, 41));

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        track(SyncableLong.create(
                () -> (long) blockEntity.processingComponent.getProgress(),
                val -> this.progress = (int) val));
        track(SyncableLong.create(
                () -> (long) blockEntity.processingComponent.getMaxProgress(),
                val -> this.maxProgress = (int) val));
        track(SyncableLong.create(
                () -> blockEntity.entropyComponent.getEntropyStored(),
                val -> this.entropy = val));
        track(SyncableLong.create(
                () -> blockEntity.entropyComponent.getMaxEntropy(),
                val -> this.maxEntropy = val));
        track(SyncableLong.create(
                () -> blockEntity.hasEntityInProcess() ? 1L : 0L,
                val -> this.hasMob = (val == 1L)));
    }

    // ==================== Data Accessors ====================

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public int getScaledProgress() {
        int barWidth = 24;
        return maxProgress != 0 && progress != 0 ? progress * barWidth / maxProgress : 0;
    }

    public boolean isCrafting() {
        return progress > 0;
    }

    public long getEntropy() {
        return entropy;
    }

    public long getMaxEntropy() {
        return maxEntropy;
    }

    // Compat variables for FE UI checks (Decay Chamber uses Entropy only)
    public int getEnergy() {
        return 0;
    }

    public int getMaxEnergy() {
        return 0;
    }

    public int getScaledEnergy() {
        int barHeight = 44;
        return maxEntropy != 0 && entropy != 0 ? (int) ((double) entropy * barHeight / maxEntropy) : 0;
    }

    public boolean hasMob() {
        return hasMob;
    }

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
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,
                    true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (sourceStack.is(Items.COAL_BLOCK)) {
                if (!moveItemStackTo(sourceStack, 1, 2, false))
                    return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(sourceStack, 0, 1, false))
                    return ItemStack.EMPTY;
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

    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(net.neoforged.neoforge.items.IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

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
