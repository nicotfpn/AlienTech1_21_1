package net.nicotfpn.alientech.machine.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.block.entity.base.AlienBlockEntity;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.util.StateValidator;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.AlienTechDebug;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Quantum Vacuum Turbine — a generator that converts decaying gravitons into FE
 * energy.
 * <p>
 * NOT a processing machine — it's a generator. Burns gravitons like fuel to
 * produce FE.
 * Extends AlienBlockEntity directly (does NOT use AbstractMachineBlockEntity
 * framework
 * because generators have different semantics than crafting machines).
 * <p>
 * Slot layout:
 * - Slot 0: Fuel input (decaying gravitons only)
 * <p>
 * Behavior:
 * - Consumes 1 decaying graviton when internal burn runs out
 * - Generates configurable FE/tick while burning
 * - Auto-pushes energy to adjacent blocks
 */
public class QuantumVacuumTurbineBlockEntity extends AlienBlockEntity {

    // ==================== Slot Layout ====================
    public static final int FUEL_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    // ==================== NBT Keys ====================
    private static final String KEY_BURN_TIME = "BurnTime";
    private static final String KEY_MAX_BURN_TIME = "MaxBurnTime";

    // ==================== State ====================
    private int burnTime = 0;
    private int maxBurnTime = 0;

    // === Phase 6: Pyramid Boost ===
    private static final String KEY_PYRAMID_BOOST = "PyramidBoost";
    private float pyramidBoostMultiplier = 1.0f;
    private int boostTicksRemaining = 0;
    private static final int BOOST_EXPIRY_TICKS = 250; // Slightly longer than scan interval

    // ==================== Components ====================

    private final ItemStackHandler fuelInventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == FUEL_SLOT && stack.is(ModItems.DECAYING_GRAVITON.get());
        }
    };

    private final EnergyStorage energyStorage;

    // ==================== Constructor ====================

    public QuantumVacuumTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_VACUUM_TURBINE_BE.get(), pos, state);
        this.energyStorage = new EnergyStorage(
                Config.QVT_ENERGY_CAPACITY.get(),
                0, // maxReceive = 0 (generator, no input)
                Config.QVT_ENERGY_CAPACITY.get() // maxExtract = capacity (unlimited push)
        );
    }

    // ==================== Tick Logic ====================

    @Override
    protected void onUpdateServer() {
        // Validate level is valid
        if (!net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        boolean wasBurning = isBurning();

        // Try to consume fuel if not burning
        if (!isBurning()) {
            tryConsumeFuel();
        }

        // Expire stale boost (deterministic countdown)
        if (boostTicksRemaining > 0) {
            boostTicksRemaining--;
        } else if (pyramidBoostMultiplier > 1.0f) {
            pyramidBoostMultiplier = 1.0f;
            setChanged();
        }

        // Generate energy while burning (with pyramid boost)
        if (isBurning()) {
            // Validate burn time
            if (burnTime <= 0) {
                burnTime = 0;
                maxBurnTime = 0;
                setChanged();
            } else {
                burnTime--;
                
                int baseFE = Config.QVT_FE_PER_TICK.get();
                if (baseFE > 0 && pyramidBoostMultiplier >= 1.0f) {
                    // Calculate boosted FE (prevent overflow)
                    long boostedFELong = (long) baseFE * (long) Math.max(1.0f, pyramidBoostMultiplier);
                    int boostedFE = boostedFELong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) boostedFELong;
                    
                    int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
                    if (space > 0) {
                        int toGenerate = Math.min(boostedFE, space);
                        if (toGenerate > 0) {
                            energyStorage.receiveEnergy(toGenerate, false);
                            AlienTechDebug.MACHINE.log("QVT generated {} FE (boost: {}x)", toGenerate, pyramidBoostMultiplier);
                        }
                    }
                }
                setChanged();
            }
        }

        // Auto-push energy to neighbors
        if (energyStorage.getEnergyStored() > 0) {
            pushEnergyToNeighbors();
        }

        // Sync if burn state changed
        if (wasBurning != isBurning()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void tryConsumeFuel() {
        ItemStack fuel = fuelInventory.getStackInSlot(FUEL_SLOT);
        if (fuel.isEmpty() || !fuel.is(ModItems.DECAYING_GRAVITON.get())) {
            return;
        }

        int burnTimePerGraviton = Config.QVT_BURN_TIME_PER_GRAVITON.get();
        if (burnTimePerGraviton <= 0) {
            return; // Invalid config
        }

        maxBurnTime = burnTimePerGraviton;
        burnTime = maxBurnTime;
        fuel.shrink(1);
        setChanged();
    }

    private void pushEnergyToNeighbors() {
        if (!net.nicotfpn.alientech.util.CapabilityUtils.isPositionLoaded(level, worldPosition)) {
            return;
        }

        int remaining = energyStorage.getEnergyStored();
        if (remaining <= 0) {
            return;
        }

        int maxPush = Config.QVT_MAX_PUSH_PER_TICK.get();
        if (maxPush <= 0) {
            return; // Invalid config
        }

        // Process directions deterministically
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) {
                break;
            }

            BlockPos neighborPos = worldPosition.relative(dir);
            if (!net.nicotfpn.alientech.util.CapabilityUtils.isPositionLoaded(level, neighborPos)) {
                continue;
            }

            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    neighborPos, dir.getOpposite());
            if (neighbor != null && neighbor.canReceive()) {
                int toPush = Math.min(remaining, maxPush);
                int pushed = neighbor.receiveEnergy(toPush, false);
                if (pushed > 0) {
                    int extracted = energyStorage.extractEnergy(pushed, false);
                    if (extracted != pushed) {
                        // Mismatch - log warning but continue
                        net.nicotfpn.alientech.AlienTech.LOGGER.warn(
                                "Energy extraction mismatch at QVT {}: expected {}, got {}",
                                worldPosition, pushed, extracted);
                    }
                    remaining -= extracted;
                }
            }
        }
    }

    // ==================== Getters ====================

    public boolean isBurning() {
        return burnTime > 0;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    // === Phase 6: Pyramid Boost ===

    public float getPyramidBoostMultiplier() {
        return pyramidBoostMultiplier;
    }

    /**
     * Set the pyramid boost multiplier. Called by PyramidCoreBlockEntity.
     * Only accepts values >= current multiplier (highest-wins rule across cores).
     * Resets expiry timer to prevent stale boosts.
     * 
     * @param multiplier the boost multiplier (must be >= 1.0)
     */
    public void setPyramidBoostMultiplier(float multiplier) {
        // Validate multiplier
        if (multiplier < 1.0f) {
            multiplier = 1.0f; // Clamp to minimum
        }

        // Prevent extreme values that could cause overflow
        if (multiplier > 1000.0f) {
            multiplier = 1000.0f; // Reasonable maximum
        }

        // Highest-wins rule: only accept if >= current
        if (multiplier >= pyramidBoostMultiplier) {
            pyramidBoostMultiplier = multiplier;
            boostTicksRemaining = BOOST_EXPIRY_TICKS;
            setChanged();
        }
    }

    // ==================== Drops ====================

    public void drops() {
        if (level == null)
            return;
        for (int i = 0; i < fuelInventory.getSlots(); i++) {
            ItemStack stack = fuelInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), stack);
            }
        }
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.quantum_vacuum_turbine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        // No GUI in Phase 5
        return null;
    }

    // ==================== Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(KEY_BURN_TIME, burnTime);
        tag.putInt(KEY_MAX_BURN_TIME, maxBurnTime);
        tag.put("FuelInventory", fuelInventory.serializeNBT(provider));
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putFloat(KEY_PYRAMID_BOOST, pyramidBoostMultiplier);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        
        // Validate and load burn time with safe defaults
        burnTime = StateValidator.ensureNonNegative(SafeNBT.getInt(tag, KEY_BURN_TIME, 0));
        maxBurnTime = StateValidator.ensureNonNegative(SafeNBT.getInt(tag, KEY_MAX_BURN_TIME, 0));
        
        // Clamp burn time to max
        if (maxBurnTime > 0 && burnTime > maxBurnTime) {
            burnTime = maxBurnTime;
        }
        
        if (tag.contains("FuelInventory")) {
            try {
                fuelInventory.deserializeNBT(provider, SafeNBT.getCompound(tag, "FuelInventory"));
            } catch (Exception e) {
                AlienTech.LOGGER.error("Failed to load fuel inventory", e);
            }
        }
        
        if (tag.contains("Energy")) {
            int energy = SafeNBT.getInt(tag, "Energy", 0);
            if (energy > 0) {
                energyStorage.receiveEnergy(Math.min(energy, energyStorage.getMaxEnergyStored()), false);
            }
        }
        
        // Validate pyramid boost multiplier
        float boost = SafeNBT.getFloat(tag, KEY_PYRAMID_BOOST, 1.0f);
        pyramidBoostMultiplier = StateValidator.clampMultiplier(boost, 1.0f, 1000.0f);
        
        // Validate state after load
        validateState();
    }

    /**
     * Internal state validation method.
     * Ensures all values are within valid ranges.
     */
    public void validateState() {
        // Validate burn time
        burnTime = StateValidator.clampProgress(burnTime, maxBurnTime);
        
        // Validate pyramid boost multiplier
        pyramidBoostMultiplier = StateValidator.clampMultiplier(pyramidBoostMultiplier, 1.0f, 1000.0f);
        
        // Validate boost ticks remaining
        boostTicksRemaining = StateValidator.ensureNonNegative(boostTicksRemaining);
        
        // Energy storage validation is handled by EnergyStorage itself
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clear any cached references
        pyramidBoostMultiplier = 1.0f;
        boostTicksRemaining = 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Validate state on load
        validateState();
    }
}
