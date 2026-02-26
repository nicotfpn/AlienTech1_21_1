package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.base.AbstractMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.IMachineProcess;
import net.nicotfpn.alientech.machine.core.SlotAccessRules;
import net.nicotfpn.alientech.screen.AncientBatteryMenu;
import net.nicotfpn.alientech.util.EnergyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Ancient Battery — compact high-capacity energy storage.
 * Ported to AbstractMachineBlockEntity framework.
 */
public class AncientBatteryBlockEntity extends AbstractMachineBlockEntity implements IMachineProcess, SlotAccessRules {

    @NotNull
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.alientech.ancient_battery");
    }

    /** Expose energy storage for capability registration and neighbor logic. */
    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorage() {
        return energy.getStorage();
    }

    // ==================== Slot Constants ====================
    public static final int CHARGE_SLOT = 0; // Battery -> Item
    public static final int DISCHARGE_SLOT = 1; // Item -> Battery
    private static final int SLOT_COUNT = 2;

    // ==================== Transfer Settings ====================
    private static final int MAX_TRANSFER = 100_000;

    // ==================== Constructor ====================

    public AncientBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_BATTERY_BE.get(), pos, state,
                Config.ANCIENT_BATTERY_CAPACITY.get(), MAX_TRANSFER, MAX_TRANSFER, SLOT_COUNT);
    }

    // ==================== Tick Logic Overrides ====================

    @Override
    protected void onUpdateServer() {
        // Framework handles auto-push/pull to neighbors via MachineTicker/Automation
        super.onUpdateServer();

        // Specific Battery logic: Internal Item Charging/Discharging
        ItemStack dischargeStack = inventory.getHandler().getStackInSlot(DISCHARGE_SLOT);
        if (!dischargeStack.isEmpty()) {
            int space = energy.getCapacity() - energy.getEnergyStored();
            if (space > 0) {
                int pulled = EnergyUtils.dischargeItem(dischargeStack, Math.min(MAX_TRANSFER, space));
                if (pulled > 0) {
                    energy.generateEnergy(pulled);
                    setChanged();
                }
            }
        }

        ItemStack chargeStack = inventory.getHandler().getStackInSlot(CHARGE_SLOT);
        if (!chargeStack.isEmpty()) {
            int available = energy.getEnergyStored();
            if (available > 0) {
                int pushed = EnergyUtils.chargeItem(chargeStack, Math.min(MAX_TRANSFER, available));
                if (pushed > 0) {
                    energy.getEnergyStorage().extractEnergy(pushed, false);
                    setChanged();
                }
            }
        }
    }

    // ==================== IMachineProcess Implementation ====================

    @Override
    public boolean canProcess() {
        return false; // Storage blocks don't "process" recipes
    }

    @Override
    public void onProcessComplete() {
    }

    @Override
    public int getProcessTime() {
        return 0;
    }

    @Override
    public int getEnergyCost() {
        return 0;
    }

    // ==================== Framework Hooks ====================

    @Override
    protected IMachineProcess getProcess() {
        return this;
    }

    @Override
    public SlotAccessRules getSlotAccessRules() {
        return this;
    }

    @Override
    protected int getFuelSlot() {
        return -1;
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[0];
    }

    @Override
    protected Predicate<ItemStack> getFuelValidator() {
        return stack -> false;
    }

    @Override
    protected ToIntFunction<ItemStack> getBurnTimeFunction() {
        return stack -> 0;
    }

    @Override
    protected int getEnergyPushRate() {
        return MAX_TRANSFER;
    }

    @Override
    protected int getEnergyPullRate() {
        return MAX_TRANSFER;
    }

    @Override
    protected boolean isSlotValid(int slot, @NotNull ItemStack stack) {
        return EnergyUtils.chargeItem(stack, 0) >= 0 || EnergyUtils.dischargeItem(stack, 0) >= 0; // Quick test for
                                                                                                  // capability
    }

    // ==================== SlotAccessRules Implementation ====================

    @Override
    public boolean canInsert(int slot, @NotNull ItemStack stack, @Nullable Direction side) {
        return isSlotValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, @Nullable Direction side) {
        return true;
    }

    // ==================== MenuProvider ====================

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        return new AncientBatteryMenu(containerId, playerInventory, this, this.data);
    }

    /** Helper for menu/automation access. */
    public net.neoforged.neoforge.items.ItemStackHandler getItemHandler() {
        return inventory.getHandler();
    }
}
