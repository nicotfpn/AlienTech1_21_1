package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.base.AlienElectricBlockEntity;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.machine.turbine.QuantumVacuumTurbineBlockEntity;
import net.nicotfpn.alientech.pyramid.PyramidStructureValidator;
import net.nicotfpn.alientech.pyramid.PyramidTier;
import net.nicotfpn.alientech.util.StateValidator;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.AlienTechDebug;
import net.nicotfpn.alientech.screen.PyramidCoreMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pyramid Core — energy amplification structure (boost-only, NOT a generator).
 * <p>
 * The Pyramid Core validates its multiblock structure and broadcasts a
 * boost multiplier to nearby Quantum Vacuum Turbines. It does NOT generate
 * or transfer FE energy itself.
 * <p>
 * Architecture:
 * - Validates structure via PyramidStructureValidator (tiered)
 * - Also validates via legacy PyramidStructureHandler (for Ankh activation)
 * - Scans for nearby QVTs and applies highest-multiplier boost
 * - Scan/validation is throttled (configurable interval, default 200 ticks)
 * - Keeps fuel slot + GUI for backward compatibility
 */
public class PyramidCoreBlockEntity extends AlienElectricBlockEntity {

    private static final int FUEL_SLOT = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markInventoryDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.GRAVITON.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    private boolean isActive = false;
    private boolean structureValid = false;
    private long lastStructureCheck = 0;

    // === Pyramid Boost ===
    private PyramidTier pyramidTier = PyramidTier.NONE;
    private float boostMultiplier = 1.0f;
    private int structureCheckCooldown = 0;

    protected final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> isActive ? 1 : 0;
                case 3 -> itemHandler.getStackInSlot(FUEL_SLOT).getCount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PyramidCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PYRAMID_CORE_BE.get(), pos, blockState,
                Config.PYRAMID_CORE_CAPACITY.get(),
                Config.PYRAMID_CORE_CAPACITY.get(),
                Config.PYRAMID_CORE_CAPACITY.get());
    }

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(Level level, BlockPos pos,
            BlockState blockState, T blockEntity) {
        if (level.isClientSide)
            return;
        if (blockEntity instanceof PyramidCoreBlockEntity core) {
            core.onUpdateServer();
        }
    }

    // ==================== Tick Logic (Boost-Only) ====================

    @Override
    protected void onUpdateServer() {
        // Validate level is valid
        if (!net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        super.onUpdateServer(); // Throttled sync

        // Countdown-based scan (more efficient than gameTime comparison)
        structureCheckCooldown--;
        if (structureCheckCooldown <= 0) {
            int scanInterval = Config.PYRAMID_SCAN_INTERVAL.get();
            structureCheckCooldown = Math.max(1, scanInterval); // Prevent zero or negative

            // Tiered structure validation updates everything
            updateStructure();

            // Structure is valid if we have ANY tier
            boolean isValid = (pyramidTier != PyramidTier.NONE);

            if (isValid != structureValid) {
                structureValid = isValid;
                setChanged();
                markForSync();
            }

            // Deactivate if structure invalid
            if (!structureValid && isActive) {
                setActive(false);
            }

            // Broadcast boost to nearby turbines
            broadcastBoostToTurbines();
        }
    }

    // ==================== Activation (Ankh) ====================

    public void setActive(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            setChanged();
            markForSync();
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean activatePyramid() {
        // Validation now depends on having a valid tier
        updateStructure();
        if (pyramidTier != PyramidTier.NONE) {
            setActive(true);
            return true;
        }
        return false;
    }

    public void deactivatePyramid() {
        setActive(false);
    }

    // ==================== Pyramid Boost Logic ====================

    /**
     * Re-validate the pyramid structure tier using the new tiered validator.
     */
    public void updateStructure() {
        PyramidTier newTier = PyramidStructureValidator.validate(level, worldPosition);
        if (newTier != pyramidTier) {
            pyramidTier = newTier;
            boostMultiplier = newTier.getMultiplier();
            setChanged();
            markForSync();
        }
    }

    /**
     * Find all QVTs within the tier's scan range and apply the boost multiplier.
     * Highest-wins rule: turbines only accept >= current multiplier.
     * <p>
     * Deterministic: processes positions in order, validates all inputs.
     */
    private void broadcastBoostToTurbines() {
        if (!net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        if (pyramidTier == PyramidTier.NONE) {
            return;
        }

        int range = pyramidTier.getScanRange();
        if (range <= 0 || range > 128) {
            return; // Invalid range (prevent excessive scanning)
        }

        // Validate multiplier
        if (boostMultiplier < 1.0f || boostMultiplier > 1000.0f) {
            return; // Invalid multiplier
        }

        BlockPos min = worldPosition.offset(-range, -range, -range);
        BlockPos max = worldPosition.offset(range, range, range);

        // Process positions deterministically
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!net.nicotfpn.alientech.util.CapabilityUtils.isPositionLoaded(level, pos)) {
                continue;
            }

            // Safe block entity access
            try {
                if (level.getBlockEntity(pos) instanceof QuantumVacuumTurbineBlockEntity turbine) {
                    turbine.setPyramidBoostMultiplier(boostMultiplier);
                }
            } catch (Exception e) {
                // Block entity access can fail in edge cases - continue scanning
                net.nicotfpn.alientech.AlienTech.LOGGER.debug("Failed to access block entity at {}", pos, e);
            }
        }
    }

    public PyramidTier getPyramidTier() {
        return pyramidTier;
    }

    public float getBoostMultiplier() {
        return boostMultiplier;
    }

    // ==================== Getters ====================

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public int getMaxEnergy() {
        return energyStorage.getMaxEnergyStored();
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.alientech.pyramid_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory pInv, Player pPlayer) {
        return new PyramidCoreMenu(id, pInv, this, containerData);
    }

    // ==================== Drops ====================

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // ==================== Persistence ====================

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        
        // Load inventory safely
        CompoundTag invTag = SafeNBT.getCompound(tag, "ItemHandler");
        if (invTag != null) {
            try {
                itemHandler.deserializeNBT(provider, invTag);
            } catch (Exception e) {
                AlienTech.LOGGER.error("Failed to load item handler", e);
            }
        }
        
        isActive = SafeNBT.getBoolean(tag, "IsActive", false);
        structureValid = SafeNBT.getBoolean(tag, "StructureValid", false);
        lastStructureCheck = SafeNBT.getInt(tag, "LastStructureCheck", 0);
        
        // Pyramid Boost - load with safe defaults
        int tierOrd = SafeNBT.getInt(tag, "PyramidTier", PyramidTier.NONE.ordinal());
        if (tierOrd >= 0 && tierOrd < PyramidTier.values().length) {
            pyramidTier = PyramidTier.values()[tierOrd];
        } else {
            pyramidTier = PyramidTier.NONE;
        }
        
        float boost = SafeNBT.getFloat(tag, "BoostMultiplier", 1.0f);
        boostMultiplier = StateValidator.clampMultiplier(boost, 1.0f, 1000.0f);
        
        // Validate state after load
        validateState();
    }

    /**
     * Internal state validation method.
     * Ensures all values are within valid ranges.
     */
    public void validateState() {
        // Validate boost multiplier
        boostMultiplier = StateValidator.clampMultiplier(boostMultiplier, 1.0f, 1000.0f);
        
        // Validate structure check cooldown
        structureCheckCooldown = StateValidator.ensureNonNegative(structureCheckCooldown);
        
        // Validate pyramid tier consistency
        if (pyramidTier == PyramidTier.NONE) {
            // No tier - ensure boost is reset
            if (boostMultiplier > 1.0f) {
                AlienTechDebug.MACHINE.log("PyramidCore: Resetting boost multiplier (no tier)");
                boostMultiplier = 1.0f;
            }
            structureValid = false;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clear cached state
        pyramidTier = PyramidTier.NONE;
        boostMultiplier = 1.0f;
        structureValid = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Validate state on load
        validateState();
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("ItemHandler", itemHandler.serializeNBT(provider));
        tag.putBoolean("IsActive", isActive);
        tag.putBoolean("StructureValid", structureValid);
        tag.putLong("LastStructureCheck", lastStructureCheck);
        // Pyramid Boost
        tag.putInt("PyramidTier", pyramidTier.ordinal());
        tag.putFloat("BoostMultiplier", boostMultiplier);
    }
}
