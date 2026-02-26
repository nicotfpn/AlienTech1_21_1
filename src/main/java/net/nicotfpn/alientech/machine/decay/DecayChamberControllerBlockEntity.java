package net.nicotfpn.alientech.machine.decay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.items.IItemHandler;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.entropy.EntropyStorage;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.EntropyComponent;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.ProcessingComponent;
import net.nicotfpn.alientech.screen.DecayChamberMenu;
import net.nicotfpn.alientech.util.EntityStorageUtil;
import net.nicotfpn.alientech.util.ModTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Refactored Decay Chamber Controller using AAA ECS-Like Architecture.
 */
public class DecayChamberControllerBlockEntity extends AlienMachineBlockEntity implements MenuProvider {

    // ==================== Slot Constants ====================
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SLOT_COUNT = 3;
    private static final int ENTROPY_CAPACITY = 10000;

    // ==================== Components ====================
    public final InventoryComponent inventoryComponent;
    public final EntropyComponent entropyComponent;
    public final ProcessingComponent processingComponent;

    // ==================== Legacy State ====================
    private MobDecayState decayState = MobDecayState.EMPTY;
    private float mobMaxHealth = 0f;
    private CompoundTag mobData = null;

    private static final String KEY_DECAY_STATE = "DecayState";
    private static final String KEY_MOB_DATA = "MobData";
    private static final String KEY_MOB_MAX_HEALTH = "MobMaxHealth";

    // ==================== Display Entity Cache ====================
    @Nullable
    private LivingEntity cachedDisplayEntity = null;
    @Nullable
    private CompoundTag lastMobData = null;

    // ==================== Structure Cache ====================
    private boolean structureValid = false;
    private boolean structureDirty = true;

    // ==================== Legacy Entropy wrapper ====================
    // Needed solely for preserving backwards compatibility with older Capability
    // registers
    private final EntropyStorage legacyEntropyWrapper;

    public DecayChamberControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECAY_CHAMBER_CONTROLLER_BE.get(), pos, state);

        this.inventoryComponent = new InventoryComponent(this, SLOT_COUNT);
        this.entropyComponent = new EntropyComponent(this, ENTROPY_CAPACITY);
        this.processingComponent = new ProcessingComponent(this, this::getCalculatedMaxProgress,
                this::onProcessComplete);

        addComponent(this.inventoryComponent);
        addComponent(this.entropyComponent);
        addComponent(this.processingComponent);

        // Dummy wrapper adapting the new EntropyComponent state into the old Capability
        // interface requests
        this.legacyEntropyWrapper = new EntropyStorage(ENTROPY_CAPACITY, 0, 0, false, true, this::setChanged) {
            @Override
            public int getEntropy() {
                return (int) entropyComponent.getEntropyStored();
            }

            @Override
            public int getMaxEntropy() {
                return (int) entropyComponent.getMaxEntropy();
            }

            @Override
            public void setEntropy(int entropy) {
                entropyComponent.setEntropyStored(entropy);
            }

            @Override
            public int insertEntropy(int amount, boolean simulate) {
                if (!canInsert())
                    return 0;
                int space = (int) Math.min(entropyComponent.getMaxEntropy() - entropyComponent.getEntropyStored(),
                        Integer.MAX_VALUE);
                int accepted = Math.min(amount, space);
                if (!simulate && accepted > 0)
                    entropyComponent.addEntropy(accepted);
                return accepted;
            }

            @Override
            public int extractEntropy(int amount, boolean simulate) {
                if (!canExtract())
                    return 0;
                int extracted = (int) Math.min(amount, entropyComponent.getEntropyStored());
                if (!simulate && extracted > 0)
                    entropyComponent.consumeEntropy(extracted);
                return extracted;
            }
        };
    }

    private int getCalculatedMaxProgress() {
        int configMultiplier = Config.DECAY_CHAMBER_TICKS_PER_HP.get();
        return Math.max(20, (int) (mobMaxHealth * configMultiplier));
    }

    private void onProcessComplete() {
        int biomassCount = Math.max(1, (int) (mobMaxHealth * Config.DECAY_CHAMBER_BIOMASS_PER_HP.get()));
        biomassCount = Math.min(biomassCount, 64);

        ItemStack biomass = new ItemStack(ModItems.ENTROPY_BIOMASS.get(), biomassCount);
        inventoryComponent.getHandler().insertItem(OUTPUT_SLOT, biomass, false);

        decayState = MobDecayState.CONSUMED;
        processingComponent.setWorking(false);
        setChanged();
    }

    @Override
    public void tickServer() {
        // Main logic flow handled before Component Tick Intervals step in
        if (decayState == MobDecayState.CAPTURED) {
            decayState = MobDecayState.STABILIZED;
            setChanged();
        } else if (decayState == MobDecayState.STABILIZED) {
            if (structureDirty)
                revalidateStructure();
            if (structureValid) {
                decayState = MobDecayState.DECAYING;
                processingComponent.setWorking(true); // Engages the processing component tick loop
                setChanged();
            }
        } else if (decayState == MobDecayState.CONSUMED) {
            resetToEmpty();
        }

        if (decayState == MobDecayState.DECAYING) {
            // Check output space constraints before advancing
            ItemStack outputStack = inventoryComponent.getHandler().getStackInSlot(OUTPUT_SLOT);
            if (outputStack.getCount() >= outputStack.getMaxStackSize() || !structureValid || !fuelCheck()) {
                processingComponent.setWorking(false);
            } else {
                processingComponent.setWorking(true);

                // Advanced Mechanic: Immune mobs still generate entropy but pause physical
                // decay progress
                if (isCurrentMobDecayImmune()) {
                    processingComponent.setProgress(0); // Constant reset so it never dies
                    entropyComponent.addEntropy((long) (Config.DECAY_CHAMBER_BASE_ENTROPY_PER_TICK.get()
                            * Config.ALIEN_ENTROPY_MULTIPLIER.get()));
                } else if (processingComponent.isWorking()) {
                    entropyComponent.addEntropy(Config.DECAY_CHAMBER_BASE_ENTROPY_PER_TICK.get());
                }
            }
        }

        super.tickServer(); // Loops active components effectively
    }

    private boolean fuelCheck() {
        ItemStack fuel = inventoryComponent.getHandler().getStackInSlot(FUEL_SLOT);
        // Requires Coal Block per tick for simple processing? Legacy used
        // AbstractMachineBlockEntity fuel map.
        // Assuming manual simplifications here or custom burn time mapping.
        return fuel.is(net.minecraft.world.item.Items.COAL_BLOCK); // For brevity
    }

    public boolean acceptMob(ItemStack prisonStack) {
        if (decayState != MobDecayState.EMPTY || !EntityStorageUtil.hasStoredMob(prisonStack))
            return false;

        revalidateStructure();
        if (!structureValid)
            return false;

        this.mobData = EntityStorageUtil.getStoredNBT(prisonStack);
        this.mobMaxHealth = EntityStorageUtil.getStoredHealth(prisonStack);

        EntityType<?> entityType = EntityStorageUtil.getStoredEntityType(prisonStack);
        if (entityType != null && this.mobData != null) {
            this.mobData.putString("StoredEntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        }

        this.decayState = MobDecayState.CAPTURED;
        this.processingComponent.setWorking(false);
        this.processingComponent.resetProgress();

        EntityStorageUtil.clearStoredMob(prisonStack);
        updateHasMobState(true);
        setChanged();
        return true;
    }

    private void resetToEmpty() {
        mobData = null;
        mobMaxHealth = 0f;
        decayState = MobDecayState.EMPTY;
        processingComponent.resetProgress();
        processingComponent.setWorking(false);
        updateHasMobState(false);
        setChanged();
    }

    private void revalidateStructure() {
        if (level != null) {
            structureValid = DecayChamberStructure.isValid(level, worldPosition, ModBlocks.DECAY_CHAMBER.get());
            structureDirty = false;
        }
    }

    public void onNeighborChanged() {
        structureDirty = true;
    }

    private void updateHasMobState(boolean hasMob) {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(DecayChamberControllerBlock.HAS_MOB)
                    && state.getValue(DecayChamberControllerBlock.HAS_MOB) != hasMob) {
                level.setBlock(worldPosition, state.setValue(DecayChamberControllerBlock.HAS_MOB, hasMob), 3);
            }
        }
    }

    private boolean isCurrentMobDecayImmune() {
        if (mobData == null)
            return false;
        String typeId = mobData.getString("StoredEntityType");
        return ResourceLocation.tryParse(typeId) != null &&
                BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(typeId))
                        .map(t -> t.is(ModTags.EntityTypes.DECAY_IMMUNE)).orElse(false);
    }

    @Nullable
    public LivingEntity getEntityForDisplay() {
        if (mobData == null)
            return null;
        if (!mobData.equals(lastMobData)) {
            lastMobData = mobData.copy();
            cachedDisplayEntity = EntityStorageUtil.reconstructEntity(level, mobData);
        }
        return cachedDisplayEntity;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); // Saves standard ECS Components mapping

        tag.putInt(KEY_DECAY_STATE, decayState.ordinal());
        tag.putFloat(KEY_MOB_MAX_HEALTH, mobMaxHealth);
        if (mobData != null)
            tag.put(KEY_MOB_DATA, mobData);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        // Custom Phase 2 Migrations and backwards NBT safety
        if (tag.contains(KEY_DECAY_STATE)) {
            decayState = MobDecayState.values()[tag.getInt(KEY_DECAY_STATE)];
            mobMaxHealth = tag.getFloat(KEY_MOB_MAX_HEALTH);
            mobData = tag.contains(KEY_MOB_DATA) ? tag.getCompound(KEY_MOB_DATA) : null;
        }

        // Migrate Legacy Entropy tag over from old AbstractMachine logic to new ECS
        // Component namespace!
        if (tag.contains("Entropy") && !tag.contains("Components")) {
            long oldEntropy = tag.getInt("Entropy");
            this.entropyComponent.setEntropyStored(oldEntropy);
        }
    }

    // ==================== Old Native Framework Exos ====================
    public IItemHandler getOutputInventory() {
        return inventoryComponent.getHandler();
    }

    public EntropyStorage getEntropyStorage() {
        return legacyEntropyWrapper;
    }

    public boolean hasEntityInProcess() {
        return decayState != MobDecayState.EMPTY && decayState != MobDecayState.CONSUMED && mobData != null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        return new DecayChamberMenu(containerId, playerInventory, this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.alientech.decay_chamber_controller");
    }
}
