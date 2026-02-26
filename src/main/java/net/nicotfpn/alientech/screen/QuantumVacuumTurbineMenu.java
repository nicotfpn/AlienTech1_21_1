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
import net.nicotfpn.alientech.machine.turbine.QuantumVacuumTurbineBlockEntity;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.nicotfpn.alientech.ui.sync.impl.SyncableLong;

public class QuantumVacuumTurbineMenu extends AlienContainerMenu {

    public final QuantumVacuumTurbineBlockEntity blockEntity;

    // Client-side cached values from SyncableLong
    private long energyStored;
    private long maxEnergy;
    private long burnTime;
    private long maxBurnTime;

    public QuantumVacuumTurbineMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public QuantumVacuumTurbineMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.QUANTUM_VACUUM_TURBINE_MENU.get(), containerId, inv.player);
        this.blockEntity = (QuantumVacuumTurbineBlockEntity) entity;

        // Fuel slot (Decaying Graviton) — same position from GUI spec
        this.addSlot(new SlotItemHandler(blockEntity.getFuelInventory(), 0, 91, 26));

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
                () -> (long) blockEntity.getBurnTime(),
                val -> this.burnTime = val));
        track(SyncableLong.create(
                () -> (long) blockEntity.getMaxBurnTime(),
                val -> this.maxBurnTime = val));
    }

    // ==================== UI Getters ====================

    public long getEnergyStored() {
        return energyStored;
    }

    public long getMaxEnergy() {
        return maxEnergy;
    }

    public long getBurnTime() {
        return burnTime;
    }

    public long getMaxBurnTime() {
        return maxBurnTime;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public int getScaledBurnTime() {
        if (maxBurnTime == 0)
            return 0;
        return (int) (burnTime * 14L / maxBurnTime);
    }

    public int getScaledEnergy() {
        if (maxEnergy == 0)
            return 0;
        return (int) (energyStored * 44L / maxEnergy);
    }

    // ==================== Quick Move ====================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem())
            return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        // Machine slot (index 0) → player inventory
        if (index == 0) {
            if (!moveItemStackTo(sourceStack, 1, 1 + 36, false)) {
                return ItemStack.EMPTY;
            }
        }
        // Player inventory → machine fuel slot
        else if (index < 1 + 36) {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
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
                player, ModBlocks.QUANTUM_VACUUM_TURBINE.get());
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
