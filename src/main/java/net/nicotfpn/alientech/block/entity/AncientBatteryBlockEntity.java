package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.EnergyComponent;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;
import net.nicotfpn.alientech.machine.core.component.AutoTransferComponent;
import net.nicotfpn.alientech.screen.AncientBatteryMenu;
import net.nicotfpn.alientech.util.EnergyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ancient Battery — compact high-capacity energy storage.
 * ECS Architecture:
 * - {@link InventoryComponent}: 2 slots (charge item, discharge item)
 * - {@link EnergyComponent}: massive FE buffer
 */
public class AncientBatteryBlockEntity extends AlienMachineBlockEntity implements net.minecraft.world.MenuProvider {

    // ==================== Slot Constants ====================
    public static final int CHARGE_SLOT = 0; // Battery -> Item
    public static final int DISCHARGE_SLOT = 1; // Item -> Battery
    private static final int SLOT_COUNT = 2;

    // ==================== Transfer Settings ====================
    private static final int MAX_TRANSFER = 100_000;

    // ==================== Components ====================
    public final InventoryComponent inventoryComponent;
    public final EnergyComponent energyComponent;
    public final SideConfigComponent sideConfig;
    public final AutoTransferComponent autoTransfer;

    // ==================== Constructor ====================

    public AncientBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_BATTERY_BE.get(), pos, state);

        this.inventoryComponent = new InventoryComponent(this, SLOT_COUNT, this::isSlotValid) {
            @Override
            public void save(CompoundTag tag, HolderLookup.Provider provider) {
                super.save(tag, provider);
            }
        };
        registerComponent(this.inventoryComponent);

        this.energyComponent = new EnergyComponent(this,
                Config.ANCIENT_BATTERY_CAPACITY.get(), MAX_TRANSFER, MAX_TRANSFER);
        registerComponent(this.energyComponent);

        // Wave 3: Side Configuration
        this.sideConfig = new SideConfigComponent(this);
        registerComponent(this.sideConfig);

        // Wave 3: Auto Transfer
        this.autoTransfer = new AutoTransferComponent(this);
        this.autoTransfer.injectSideConfig(this.sideConfig);
        registerComponent(this.autoTransfer);

        // Inicializar wrappers
        initSidedWrappers();
    }

    // ==================== Slot Validation ====================

    private boolean isSlotValid(int slot, @NotNull ItemStack stack) {
        return EnergyUtils.chargeItem(stack, 0) >= 0 || EnergyUtils.dischargeItem(stack, 0) >= 0;
    }

    // ==================== Tick Logic ====================

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide)
            return;

        boolean didWork = false;

        // Discharge Item -> Battery
        ItemStack dischargeStack = inventoryComponent.getHandler().getStackInSlot(DISCHARGE_SLOT);
        if (!dischargeStack.isEmpty()) {
            int space = energyComponent.getEnergyStorage().getMaxEnergyStored()
                    - energyComponent.getEnergyStorage().getEnergyStored();
            if (space > 0) {
                int pulled = EnergyUtils.dischargeItem(dischargeStack, Math.min(MAX_TRANSFER, space));
                if (pulled > 0) {
                    energyComponent.getEnergyStorage().receiveEnergy(pulled, false);
                    didWork = true;
                }
            }
        }

        // Charge Battery -> Item
        ItemStack chargeStack = inventoryComponent.getHandler().getStackInSlot(CHARGE_SLOT);
        if (!chargeStack.isEmpty()) {
            int available = energyComponent.getEnergyStorage().getEnergyStored();
            if (available > 0) {
                int pushed = EnergyUtils.chargeItem(chargeStack, Math.min(MAX_TRANSFER, available));
                if (pushed > 0) {
                    energyComponent.getEnergyStorage().extractEnergy(pushed, false);
                    didWork = true;
                }
            }
        }

        // Push energy to adjacent blocks
        if (energyComponent.getEnergyStorage().getEnergyStored() > 0) {
            didWork |= pushEnergyToNeighbors();
        }

        if (didWork) {
            setChanged();
        }

        super.tickServer();
    }

    private boolean pushEnergyToNeighbors() {
        if (level == null)
            return false;
        boolean pushedAny = false;

        for (Direction dir : Direction.values()) {
            if (energyComponent.getEnergyStorage().getEnergyStored() <= 0)
                break;

            IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(dir),
                    dir.getOpposite());
            if (cap != null && cap.canReceive()) {
                int toSend = Math.min(MAX_TRANSFER, energyComponent.getEnergyStorage().getEnergyStored());
                int accepted = cap.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    energyComponent.getEnergyStorage().extractEnergy(accepted, false);
                    pushedAny = true;
                }
            }
        }
        return pushedAny;
    }

    // ==================== Accessors ====================

    public net.neoforged.neoforge.items.IItemHandler getItemHandler() {
        return inventoryComponent.getHandler();
    }

    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorage() {
        return energyComponent.getEnergyStorage();
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.ancient_battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new AncientBatteryMenu(id, inv, this);
    }

    // ==================== Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        // Legacy NBT Migration
        if (!tag.contains("Components")) {
            if (tag.contains("MachineEnergy")) {
                CompoundTag et = tag.getCompound("MachineEnergy");
                if (et.contains("Stored")) {
                    energyComponent.getEnergyStorage().setEnergy(et.getInt("Stored"));
                }
            }
        }
    }

    // ==================== Drops ====================

    @Override
    public void drops() {
        if (level != null) {
            inventoryComponent.dropAll(level, worldPosition);
        }
    }
}
