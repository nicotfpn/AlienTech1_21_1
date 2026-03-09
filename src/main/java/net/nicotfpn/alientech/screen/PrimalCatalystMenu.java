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
import net.nicotfpn.alientech.block.entity.PrimalCatalystBlockEntity;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.nicotfpn.alientech.ui.sync.impl.SyncableLong;

/**
 * Container menu for the Primal Catalyst.
 *
 * Slot layout (5 machine slots + 36 player slots):
 * 0, 1, 2 = Input slots (top row)
 * 3 = Fuel slot (middle-left, coal_block only - deprecated/inert)
 * 4 = Output slot (right side)
 */
public class PrimalCatalystMenu extends AlienContainerMenu {

    public final PrimalCatalystBlockEntity blockEntity;

    // ==================== Sync Trackers ====================
    private final SyncableLong progress;
    private final SyncableLong maxProgress;
    private final SyncableLong energy;
    private final SyncableLong maxEnergy;
    private final SyncableLong entropy;
    private final SyncableLong maxEntropy;

    // ==================== Slot Layout Constants ====================
    private static final int TE_SLOT_COUNT = 5;
    private static final int VANILLA_FIRST_SLOT_INDEX = TE_SLOT_COUNT;
    private static final int VANILLA_SLOT_COUNT = 36; // 27 inventory + 9 hotbar

    // ==================== Constructor (Network) ====================

    public PrimalCatalystMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // ==================== Constructor (Server) ====================

    public PrimalCatalystMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.PRIMAL_CATALYST_MENU.get(), containerId, inv.player);
        blockEntity = (PrimalCatalystBlockEntity) entity;

        // Input slots: vertical column (from alientech_gui_gen.py: x=66, y=23/41/59)
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 66, 23)); // Input 1
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 1, 66, 41)); // Input 2
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 2, 66, 59)); // Input 3

        // Fuel slot: under energy bar (from alientech_gui_gen.py: positioned near
        // entropy bar)
        this.addSlot(new FuelSlot(blockEntity.getItemHandler(), 3, 10, 56));

        // Output slot: right side (from alientech_gui_gen.py: x=116, y=41)
        this.addSlot(new OutputSlot(blockEntity.getItemHandler(), 4, 116, 41));

        // Sync Data Registration
        this.progress = new SyncableLong(() -> (long) blockEntity.processingComponent.getProgress(), null);
        this.maxProgress = new SyncableLong(() -> (long) blockEntity.processingComponent.getMaxProgress(), null);
        this.energy = new SyncableLong(() -> (long) blockEntity.getEnergyStorage().getEnergyStored(), null);
        this.maxEnergy = new SyncableLong(() -> (long) blockEntity.getEnergyStorage().getMaxEnergyStored(), null);
        this.entropy = new SyncableLong(() -> blockEntity.entropyComponent.getEntropyStored(), null);
        this.maxEntropy = new SyncableLong(() -> blockEntity.entropyComponent.getMaxEntropy(), null);

        track(progress);
        track(maxProgress);
        track(energy);
        track(maxEnergy);
        track(entropy);
        track(maxEntropy);

        // Player inventory and hotbar
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    // ==================== Data Accessors ====================

    public boolean isCrafting() {
        return progress.get() > 0;
    }

    public int getScaledProgress() {
        long p = progress.get();
        long maxP = maxProgress.get();
        int barWidth = 24; // pixel width of progress arrow (from alientech_gui_gen.py)
        return maxP != 0 && p != 0 ? (int) (p * barWidth / maxP) : 0;
    }

    // Deprecated for Primal Catalyst ECS (fuel no longer supported)
    public boolean isBurning() {
        return false;
    }

    public int getScaledBurnTime() {
        return 0;
    }

    public int getScaledEnergy() {
        long e = energy.get();
        long maxE = maxEnergy.get();
        int barHeight = 44; // pixel height of energy bar (from mc_gui_generator.py)
        return maxE != 0 && e != 0 ? (int) (e * barHeight / maxE) : 0;
    }

    public long getEnergy() {
        return energy.get();
    }

    public long getMaxEnergy() {
        return maxEnergy.get();
    }

    public long getEntropy() {
        return entropy.get();
    }

    public long getMaxEntropy() {
        return maxEntropy.get();
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
            // Moving FROM player TO machine (Try input slots: 0, 1, 2)
            if (!moveItemStackTo(sourceStack, 0, 3, false)) {
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
     * Fuel slot: inert for Primal Catalyst in ECS architecture.
     */
    private static class FuelSlot extends SlotItemHandler {
        public FuelSlot(net.neoforged.neoforge.items.IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // Fuel not supported for Primal Catalyst
        }
    }
}