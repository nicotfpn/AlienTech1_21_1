package net.nicotfpn.alientech.block.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.machine.core.IMachineProcess;
import net.nicotfpn.alientech.machine.core.MachineAutomation;
import net.nicotfpn.alientech.machine.core.MachineEnergy;
import net.nicotfpn.alientech.machine.core.MachineInventory;
import net.nicotfpn.alientech.machine.core.MachineProcessor;
import net.nicotfpn.alientech.machine.core.MachineTicker;
import net.nicotfpn.alientech.machine.core.SlotAccessRules;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Thin orchestrator for all processing machines in AlienTech.
 * Composes independent modules via composition over inheritance:
 * <ul>
 * <li>{@link MachineInventory} — slot management, validation, NBT, drops</li>
 * <li>{@link MachineEnergy} — FE storage + fuel burn tracking</li>
 * <li>{@link MachineProcessor} — progress tracking, tick advancement</li>
 * <li>{@link MachineAutomation} — sided item handlers, auto-push</li>
 * <li>{@link MachineTicker} — centralized tick orchestration</li>
 * </ul>
 *
 * Subclasses only define: slot layout, fuel/output config, recipe logic
 * ({@link IMachineProcess}),
 * and sided access rules ({@link SlotAccessRules}).
 *
 * Extends {@link AlienBlockEntity} directly (NOT AlienElectricBlockEntity).
 * Energy is managed by the MachineEnergy component.
 */
public abstract class AbstractMachineBlockEntity extends AlienBlockEntity {

    // ==================== Composed Modules ====================
    protected final MachineInventory inventory;
    protected final MachineEnergy energy;
    protected final MachineProcessor processor;
    protected final MachineAutomation automation;

    // Lazy-initialized: depends on abstract methods that aren't safe to call in
    // constructor
    private MachineTicker ticker;

    // ==================== Sync Channels ====================
    // Replicated from AlienElectricBlockEntity for machines using composition
    private boolean needsEnergySync = false;
    private boolean needsInventorySync = false;
    private int energySyncCooldown = 0;
    private static final int ENERGY_SYNC_INTERVAL = 10;

    // ==================== Container Data (6 values synced to client)
    // ====================
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> processor.getProgress();
                case 1 -> getProcess().getProcessTime();
                case 2 -> energy.getBurnTime();
                case 3 -> energy.getMaxBurnTime();
                case 4 -> energy.getEnergyStored();
                case 5 -> energy.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> processor.setProgress(value);
                // case 1: maxProgress is derived from getProcessTime(), not settable
                case 2 -> energy.setBurnTime(value);
                case 3 -> energy.setMaxBurnTime(value);
                // case 4, 5: energy is managed by MachineEnergy
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    // ==================== Constructor ====================

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int energyCapacity, int maxReceive, int maxExtract, int slotCount) {
        super(type, pos, state);

        // Inventory component: marks inventory dirty on change
        this.inventory = new MachineInventory(slotCount, () -> {
            setChanged();
            markInventoryDirty();
        });
        // Slot validator uses virtual dispatch — safe because it's only called at
        // runtime, not in constructor
        inventory.setSlotValidator(this::isSlotValid);

        // Energy component: marks energy dirty on change
        this.energy = new MachineEnergy(energyCapacity, maxReceive, maxExtract, () -> {
            setChanged();
            markEnergyDirty();
        });

        // Processor component: marks block dirty on progress change
        this.processor = new MachineProcessor(this::setChanged);

        // Automation component
        this.automation = new MachineAutomation();
    }

    // ==================== Abstract Hooks (subclasses MUST implement)
    // ====================

    /** Get the IMachineProcess for this machine (typically return `this`). */
    protected abstract IMachineProcess getProcess();

    /** Get the SlotAccessRules for sided automation. */
    public abstract SlotAccessRules getSlotAccessRules();

    /**
     * Validate whether an item can go into a specific slot (used by
     * ItemStackHandler).
     */
    protected abstract boolean isSlotValid(int slot, @NotNull ItemStack stack);

    /**
     * Get the slot index used for fuel (coal blocks). Return -1 if no fuel slot.
     */
    protected abstract int getFuelSlot();

    /** Get the slot indices used for output. Used for auto-push. */
    protected abstract int[] getOutputSlots();

    /** Predicate to check if an item is valid fuel. */
    protected abstract Predicate<ItemStack> getFuelValidator();

    /** Function returning burn time in ticks for a fuel item. */
    protected abstract ToIntFunction<ItemStack> getBurnTimeFunction();

    // ==================== Component Accessors ====================

    public MachineInventory getInventory() {
        return inventory;
    }

    public MachineEnergy getEnergy() {
        return energy;
    }

    public MachineProcessor getProcessor() {
        return processor;
    }

    public MachineAutomation getAutomation() {
        return automation;
    }

    public ContainerData getContainerData() {
        return data;
    }

    // ==================== Server Tick ====================

    @Override
    protected void onUpdateServer() {
        // Validate level is valid before processing
        if (!net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        super.onUpdateServer();

        // Delegate all tick logic to MachineTicker
        getOrCreateTicker().tickServer(inventory, energy, processor, automation,
                getProcess(), level, worldPosition);

        // Handle sync channels
        handleSync();
    }

    /**
     * Lazy initialize the MachineTicker.
     * Cannot be created in constructor because it depends on abstract method return
     * values.
     */
    private MachineTicker getOrCreateTicker() {
        if (ticker == null) {
            ticker = new MachineTicker(getFuelSlot(), getFuelValidator(), getBurnTimeFunction(),
                    getOutputSlots(), getSlotAccessRules());
        }
        return ticker;
    }

    // ==================== Sync Logic ====================

    protected void markEnergyDirty() {
        this.needsEnergySync = true;
    }

    protected void markInventoryDirty() {
        this.needsInventorySync = true;
    }

    private void handleSync() {
        boolean didSync = false;

        // Channel 1: Inventory sync — immediate (next tick)
        if (needsInventorySync) {
            sendBlockUpdate();
            needsInventorySync = false;
            didSync = true;
            // Reset energy cooldown since we just synced everything
            needsEnergySync = false;
            energySyncCooldown = ENERGY_SYNC_INTERVAL;
        }

        // Channel 2: Energy sync — throttled
        if (energySyncCooldown > 0) {
            energySyncCooldown--;
        }
        if (needsEnergySync && energySyncCooldown <= 0 && !didSync) {
            sendBlockUpdate();
            needsEnergySync = false;
            energySyncCooldown = ENERGY_SYNC_INTERVAL;
        }
    }

    private void sendBlockUpdate() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== NBT Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        inventory.save(tag, registries);
        energy.save(tag);
        processor.save(tag);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.load(tag, registries);
        energy.load(tag);
        processor.load(tag);
    }

    // ==================== Block Removal ====================

    /**
     * Drop all inventory contents when block is broken.
     */
    public void drops() {
        if (level != null) {
            inventory.dropAll(level, worldPosition);
        }
    }
}
