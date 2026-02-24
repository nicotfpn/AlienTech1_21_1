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
import net.nicotfpn.alientech.pyramid.PyramidNetwork;
import net.nicotfpn.alientech.pyramid.PyramidTier;
import net.nicotfpn.alientech.entropy.EntropyStorage;
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

    // ==================== Container Data (extended values synced to client)
    // ====================
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> processor.getProgress();
                case 1 -> getProcess().getProcessTime();
                case 2 -> energy.getBurnTime();
                case 3 -> energy.getMaxBurnTime();
                case 4 -> net.nicotfpn.alientech.util.EnergyUtils.lowBits(energy.getEnergyStored());
                case 5 -> net.nicotfpn.alientech.util.EnergyUtils.highBits(energy.getEnergyStored());
                case 6 -> net.nicotfpn.alientech.util.EnergyUtils.lowBits(energy.getCapacity());
                case 7 -> net.nicotfpn.alientech.util.EnergyUtils.highBits(energy.getCapacity());
                // Entropy fields appended for backward compatibility
                case 8 -> net.nicotfpn.alientech.util.EnergyUtils
                        .lowBits(getEntropyStorage() != null ? getEntropyStorage().getEntropy() : 0);
                case 9 -> net.nicotfpn.alientech.util.EnergyUtils
                        .highBits(getEntropyStorage() != null ? getEntropyStorage().getEntropy() : 0);
                case 10 -> net.nicotfpn.alientech.util.EnergyUtils
                        .lowBits(getEntropyStorage() != null ? getEntropyStorage().getMaxEntropy() : 0);
                case 11 -> net.nicotfpn.alientech.util.EnergyUtils
                        .highBits(getEntropyStorage() != null ? getEntropyStorage().getMaxEntropy() : 0);
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
                // case 4-7: energy is managed by MachineEnergy
            }
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    // ==================== Constructor ====================

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int energyCapacity, int maxReceive, int maxExtract, int slotCount) {
        super(type, pos, state);

        // Inventory component: marks inventory dirty on change
        this.inventory = new MachineInventory(slotCount, () -> {
            markInventoryDirty();
        });
        // Slot validator uses virtual dispatch
        inventory.setSlotValidator(this::isSlotValid);

        // Energy component: no lambda needed, dirty sync flag handles changes
        // mathematically
        this.energy = new MachineEnergy(energyCapacity, maxReceive, maxExtract, () -> {
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

        // Pull entropy from PyramidNetwork into local buffer (if present).
        try {
            net.nicotfpn.alientech.entropy.EntropyStorage local = getEntropyStorage();
            if (local != null && level != null) {
                int capacityLeft = local.getMaxEntropy() - local.getEntropy();
                if (capacityLeft > 0) {
                    int maxTransfer = net.nicotfpn.alientech.Config.ENTROPY_CABLE_TRANSFER_RATE.get();
                    int toPull = Math.min(capacityLeft, maxTransfer);
                    int extracted = net.nicotfpn.alientech.pyramid.PyramidNetwork.get(level).extractEntropy(toPull,
                            false);
                    if (extracted > 0) {
                        local.insertEntropy(extracted, false);
                        setChanged();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Delegate all tick logic to MachineTicker with speed multiplier.
        float multiplier = getSpeedMultiplier();
        int ticksProcessed = getOrCreateTicker().tickServer(inventory, energy, processor, automation,
                getProcess(), level, worldPosition, multiplier);

        // Deduct proportional Entropy mathematically linked to true processor
        // iterations
        if (ticksProcessed > 0 && getEntropyPerTick() > 0) {
            EntropyStorage storage = getEntropyStorage();
            if (storage != null) {
                storage.extractEntropy(getEntropyPerTick() * ticksProcessed, false);
            }
        }

        // Deferred Dirty Sync Flag Protocol
        int currentEnergy = energy.getEnergyStored();
        int currentProgress = processor.getProgress();
        int currentBurnTime = energy.getBurnTime();

        EntropyStorage local = getEntropyStorage();
        int currentEntropy = local != null ? local.getEntropy() : 0;

        if (currentEnergy != lastStateEnergy || currentProgress != lastStateProgress ||
                currentBurnTime != lastStateBurnTime || currentEntropy != lastStateEntropy) {

            setChanged();

            // If energy shifted drastically, flag for throttled network sync
            if (currentEnergy != lastStateEnergy || currentEntropy != lastStateEntropy) {
                markEnergyDirty();
            }

            lastStateEnergy = currentEnergy;
            lastStateProgress = currentProgress;
            lastStateBurnTime = currentBurnTime;
            lastStateEntropy = currentEntropy;
        }

        // Handle sync channels
        handleSync();
    }

    private int lastStateEnergy = -1;
    private int lastStateProgress = -1;
    private int lastStateBurnTime = -1;
    private int lastStateEntropy = -1;

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

    // ==================== Entropy / Speed Multiplier Hook ====================

    /**
     * Optional hook for subclasses to expose their local entropy buffer.
     * Default implementation returns null (machine has no entropy buffer).
     */
    protected EntropyStorage getEntropyStorage() {
        return null;
    }

    /**
     * Entropy cost per tick for the active process. Subclasses should override
     * if they consume entropy per tick.
     */
    protected int getEntropyPerTick() {
        return 0;
    }

    /**
     * FE cost per tick for the active process. Subclasses should override if
     * they consume FE per tick.
     */
    protected int getFEPerTick() {
        return 0;
    }

    /**
     * Compute the current speed multiplier based on Pyramid tier and available
     * resources (local entropy buffer + FE storage). This method follows the
     * architecture rules and must not modify processor logic directly.
     */
    public float getSpeedMultiplier() {
        if (level == null)
            return 0f;

        PyramidTier tier = PyramidNetwork.get(level).getTier();

        EntropyStorage entropy = getEntropyStorage();
        boolean hasEntropy = entropy != null && entropy.getEntropy() >= getEntropyPerTick();
        boolean hasFE = energy.getEnergyStored() >= getFEPerTick();

        if (tier == PyramidTier.TIER_1) {
            return hasEntropy ? 1.0f : 0f;
        } else if (tier == PyramidTier.TIER_2) {
            if (hasEntropy && hasFE)
                return 2.0f;
            if (hasEntropy || hasFE)
                return 1.0f;
            return 0f;
        } else if (tier == PyramidTier.TIER_3) {
            if (hasEntropy && hasFE)
                return 3.0f;
            if (hasEntropy || hasFE)
                return 1.5f;
            return 0f;
        }
        return 0f;
    }
}
