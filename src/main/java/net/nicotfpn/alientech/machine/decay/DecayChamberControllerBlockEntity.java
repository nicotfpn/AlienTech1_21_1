package net.nicotfpn.alientech.machine.decay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.block.entity.base.AlienBlockEntity;
import net.nicotfpn.alientech.entropy.EntropyStorage;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.util.EntityStorageUtil;
import net.nicotfpn.alientech.util.StateValidator;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.AlienTechDebug;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decay Chamber Controller — processes captured mobs into entropy biomass.
 * <p>
 * Pipeline: Mob (from Pocket Dimensional Prison) → decay processing → entropy
 * biomass items.
 * <p>
 * Architecture:
 * - Extends AlienBlockEntity (tick hooks, sync, MenuProvider)
 * - Contains EntropyStorage (output buffer)
 * - Contains ItemStackHandler (1 output slot for biomass)
 * - Stores mob snapshot in NBT (never raw entity reference)
 * - Progress tracked with simple tick counter
 * <p>
 * Structure validation: only on mob insertion (not every tick).
 * Mob scanning: never — mobs are delivered via item interaction.
 */
public class DecayChamberControllerBlockEntity extends AlienBlockEntity {

    // ==================== Constants ====================

    private static final int OUTPUT_SLOT = 0;
    private static final int SLOT_COUNT = 1;
    private static final int MAX_STACK_OUTPUT = 64;

    // NBT keys
    private static final String KEY_DECAY_STATE = "DecayState";
    private static final String KEY_DECAY_PROGRESS = "DecayProgress";
    private static final String KEY_DECAY_MAX = "DecayMax";
    private static final String KEY_MOB_DATA = "MobData";
    private static final String KEY_MOB_MAX_HEALTH = "MobMaxHealth";

    // ==================== State ====================

    private MobDecayState decayState = MobDecayState.EMPTY;
    private int decayProgress = 0;
    private int decayMaxProgress = 0;
    private float mobMaxHealth = 0f;

    // Mob snapshot (stored as raw NBT, not entity reference)
    private CompoundTag mobData = null;

    // ==================== Components ====================

    private final ItemStackHandler outputInventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final EntropyStorage entropyStorage;

    // ==================== Structure Cache ====================

    private boolean structureValid = false;
    private boolean structureDirty = true; // Revalidate on first tick

    // ==================== Constructor ====================

    public DecayChamberControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECAY_CHAMBER_CONTROLLER_BE.get(), pos, state);
        this.entropyStorage = new EntropyStorage(
                10000, // capacity
                0, // no insert limit
                0, // no extract limit
                false, // cannot receive entropy (it produces)
                true, // can output entropy
                this::setChanged);
    }

    // ==================== Mob Transfer ====================

    /**
     * Accept a mob from a Pocket Dimensional Prison item.
     * Called from DecayChamberControllerBlock.useItemOn().
     *
     * @param prisonStack the prison item stack with a stored mob
     * @return true if mob was accepted
     */
    public boolean acceptMob(ItemStack prisonStack) {
        if (decayState != MobDecayState.EMPTY)
            return false;
        if (!EntityStorageUtil.hasStoredMob(prisonStack))
            return false;

        // Validate structure before accepting
        revalidateStructure();
        if (!structureValid)
            return false;

        // Extract mob data from the prison item
        this.mobData = EntityStorageUtil.getStoredNBT(prisonStack);
        this.mobMaxHealth = EntityStorageUtil.getStoredHealth(prisonStack);

        // Calculate processing time based on mob max health
        int configMultiplier = Config.DECAY_CHAMBER_TICKS_PER_HP.get();
        this.decayMaxProgress = Math.max(20, (int) (mobMaxHealth * configMultiplier));
        this.decayProgress = 0;
        this.decayState = MobDecayState.CAPTURED;

        // Clear the prison item
        EntityStorageUtil.clearStoredMob(prisonStack);

        setChanged();
        return true;
    }

    // ==================== Tick Logic ====================

    @Override
    protected void onUpdateServer() {
        // Validate level is valid
        if (!net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(level)) {
            return;
        }

        if (decayState == MobDecayState.EMPTY) {
            return;
        }

        // State machine progression (deterministic)
        switch (decayState) {
            case CAPTURED -> {
                // Immediately stabilize
                decayState = MobDecayState.STABILIZED;
                setChanged();
            }
            case STABILIZED -> {
                // Only start decaying if structure is valid
                if (structureDirty) {
                    revalidateStructure();
                }
                if (structureValid) {
                    decayState = MobDecayState.DECAYING;
                    setChanged();
                }
            }
            case DECAYING -> {
                // Validate progress bounds
                if (decayMaxProgress <= 0) {
                    // Invalid state - reset
                    decayState = MobDecayState.EMPTY;
                    decayProgress = 0;
                    decayMaxProgress = 0;
                    mobData = null;
                    mobMaxHealth = 0f;
                    setChanged();
                    return;
                }

                // Progress the decay
                decayProgress++;
                if (decayProgress >= decayMaxProgress) {
                    onDecayComplete();
                } else if (decayProgress < 0) {
                    // Overflow protection
                    decayProgress = decayMaxProgress;
                    onDecayComplete();
                }
                setChanged();
            }
            case CONSUMED -> {
                // Reset to empty
                mobData = null;
                mobMaxHealth = 0f;
                decayProgress = 0;
                decayMaxProgress = 0;
                decayState = MobDecayState.EMPTY;
                setChanged();
            }
            default -> {
                // Invalid state - reset to empty
                decayState = MobDecayState.EMPTY;
                decayProgress = 0;
                decayMaxProgress = 0;
                mobData = null;
                mobMaxHealth = 0f;
                setChanged();
            }
        }
    }

    /**
     * Called when decay processing reaches 100%.
     * Generates entropy biomass items proportional to mob max health.
     */
    private void onDecayComplete() {
        // Calculate output biomass count: 1 per configured HP units
        int biomassCount = Math.max(1, (int) (mobMaxHealth * Config.DECAY_CHAMBER_BIOMASS_PER_HP.get()));
        biomassCount = Math.min(biomassCount, MAX_STACK_OUTPUT);

        ItemStack biomass = new ItemStack(ModItems.ENTROPY_BIOMASS.get(), biomassCount);
        ItemStack existing = outputInventory.getStackInSlot(OUTPUT_SLOT);

        // Try to merge with existing output
        if (existing.isEmpty()) {
            outputInventory.setStackInSlot(OUTPUT_SLOT, biomass);
        } else if (ItemStack.isSameItemSameComponents(existing, biomass)) {
            int newCount = Math.min(existing.getCount() + biomassCount, existing.getMaxStackSize());
            existing.setCount(newCount);
        }
        // If output is full, biomass is lost (player should clear output)

        decayState = MobDecayState.CONSUMED;
    }

    // ==================== Structure Validation ====================

    /**
     * Mark structure as needing revalidation (called on neighbor block change).
     */
    public void onNeighborChanged() {
        structureDirty = true;
    }

    private void revalidateStructure() {
        if (!net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(level)) {
            structureValid = false;
            structureDirty = false;
            return;
        }
        
        try {
            structureValid = DecayChamberStructure.isValid(level, worldPosition,
                    ModBlocks.DECAY_CHAMBER.get());
        } catch (Exception e) {
            // Structure validation can fail in edge cases
            net.nicotfpn.alientech.AlienTech.LOGGER.debug("Structure validation failed", e);
            structureValid = false;
        }
        structureDirty = false;
    }

    // ==================== Getters ====================

    public MobDecayState getDecayState() {
        return decayState;
    }

    public int getDecayProgress() {
        return decayProgress;
    }

    public int getDecayMaxProgress() {
        return decayMaxProgress;
    }

    public float getMobMaxHealth() {
        return mobMaxHealth;
    }

    public EntropyStorage getEntropyStorage() {
        return entropyStorage;
    }

    public ItemStackHandler getOutputInventory() {
        return outputInventory;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    // ==================== Drops ====================

    public void drops() {
        if (level == null)
            return;
        for (int i = 0; i < outputInventory.getSlots(); i++) {
            ItemStack stack = outputInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), stack);
            }
        }
    }

    // ==================== MenuProvider (no GUI yet) ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.decay_chamber_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        // No GUI in Phase 3 — return null (block won't open a screen)
        return null;
    }

    // ==================== Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(KEY_DECAY_STATE, decayState.ordinal());
        tag.putInt(KEY_DECAY_PROGRESS, decayProgress);
        tag.putInt(KEY_DECAY_MAX, decayMaxProgress);
        tag.putFloat(KEY_MOB_MAX_HEALTH, mobMaxHealth);
        if (mobData != null) {
            tag.put(KEY_MOB_DATA, mobData.copy());
        }
        tag.put("OutputInventory", outputInventory.serializeNBT(provider));
        entropyStorage.save(tag);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        
        // Validate and load state with safe defaults
        int stateOrdinal = SafeNBT.getInt(tag, KEY_DECAY_STATE, MobDecayState.EMPTY.ordinal());
        if (stateOrdinal >= 0 && stateOrdinal < MobDecayState.values().length) {
            decayState = MobDecayState.values()[stateOrdinal];
        } else {
            decayState = MobDecayState.EMPTY;
        }
        
        // Validate and load progress
        decayMaxProgress = StateValidator.ensureNonNegative(SafeNBT.getInt(tag, KEY_DECAY_MAX, 0));
        decayProgress = StateValidator.clampProgress(
                SafeNBT.getInt(tag, KEY_DECAY_PROGRESS, 0), 
                decayMaxProgress);
        
        // Validate and load mob health
        mobMaxHealth = StateValidator.ensureNonNegative(SafeNBT.getFloat(tag, KEY_MOB_MAX_HEALTH, 0f));
        
        // Load mob data safely
        CompoundTag mobDataTag = SafeNBT.getCompound(tag, KEY_MOB_DATA);
        if (mobDataTag != null && !mobDataTag.isEmpty()) {
            try {
                mobData = mobDataTag.copy();
            } catch (Exception e) {
                AlienTech.LOGGER.error("Failed to load mob data", e);
                mobData = null;
            }
        } else {
            mobData = null;
        }
        
        // Load inventory safely
        CompoundTag invTag = SafeNBT.getCompound(tag, "OutputInventory");
        if (invTag != null) {
            try {
                outputInventory.deserializeNBT(provider, invTag);
            } catch (Exception e) {
                AlienTech.LOGGER.error("Failed to load output inventory", e);
            }
        }
        
        // Load entropy storage
        try {
            entropyStorage.load(tag);
        } catch (Exception e) {
            AlienTech.LOGGER.error("Failed to load entropy storage", e);
        }
        
        // Validate state after load
        validateState();
    }

    /**
     * Internal state validation method.
     * Ensures all values are within valid ranges.
     */
    public void validateState() {
        // Validate decay progress
        decayProgress = StateValidator.clampProgress(decayProgress, decayMaxProgress);
        
        // Validate mob health
        mobMaxHealth = StateValidator.ensureNonNegative(mobMaxHealth);
        
        // Validate decay state consistency
        if (decayState == MobDecayState.EMPTY) {
            // Empty state - clear all data
            if (mobData != null || mobMaxHealth > 0 || decayProgress > 0) {
                AlienTechDebug.MACHINE.log("DecayChamber: Clearing invalid state data");
                mobData = null;
                mobMaxHealth = 0f;
                decayProgress = 0;
                decayMaxProgress = 0;
            }
        }
        
        // Validate entropy storage
        if (entropyStorage instanceof net.nicotfpn.alientech.entropy.EntropyStorage storage) {
            storage.validateState();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clear mob data reference
        mobData = null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Validate state on load
        validateState();
    }
}
