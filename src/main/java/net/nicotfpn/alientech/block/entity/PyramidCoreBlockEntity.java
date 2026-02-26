package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.base.AbstractMachineBlockEntity;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.machine.core.IMachineProcess;
import net.nicotfpn.alientech.machine.core.SlotAccessRules;
import net.nicotfpn.alientech.machine.turbine.QuantumVacuumTurbineBlockEntity;
import net.nicotfpn.alientech.pyramid.PyramidNetwork;
import net.nicotfpn.alientech.pyramid.PyramidStructureValidator;
import net.nicotfpn.alientech.pyramid.PyramidTier;
import net.nicotfpn.alientech.screen.PyramidCoreMenu;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.StateValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Pyramid Core — The central brain of a pyramid structure.
 * Handles structure validation, entropy generation, and turbine boosting.
 * Ported to AbstractMachineBlockEntity framework.
 * 
 * NEW MECHANICS v2.0:
 * - Requires Entropy Biomass to run (consumed over time)
 * - Inertial Stability Alloy enables infinite production until turned off
 * - Tracks owner who activated the pyramid
 */
public class PyramidCoreBlockEntity extends AbstractMachineBlockEntity implements IMachineProcess, SlotAccessRules {

    public static final int ALLOY_SLOT = 0;
    public static final int BIOMASS_SLOT = 1;
    private static final int SLOT_COUNT = 2;

    // State
    private boolean isActive = false;
    private boolean structureValid = false;
    private int structureCheckCooldown = 0;
    private PyramidTier pyramidTier = PyramidTier.NONE;
    private float boostMultiplier = 1.0f;
    
    // Owner tracking - the player who activated the pyramid
    private UUID ownerUUID = null;
    private String ownerName = null;
    
    // Biomass consumption tracker
    private int biomassTicks = 0;
    private static final int BIOMASS_CONSUME_INTERVAL = 200; // ~10 seconds

    // ==================== Constructor ====================

    public PyramidCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYRAMID_CORE_BE.get(), pos, state,
                Config.PYRAMID_CORE_CAPACITY.get(),
                Config.PYRAMID_CORE_CAPACITY.get(),
                Config.PYRAMID_CORE_CAPACITY.get(),
                SLOT_COUNT);
    }

    // ==================== Tick Logic Overrides ====================

    @Override
    protected void onUpdateServer() {
        if (level == null)
            return;

        // 1. Throttled Structure Validation & Boost Broadcast
        structureCheckCooldown--;
        if (structureCheckCooldown <= 0) {
            structureCheckCooldown = Math.max(20, Config.PYRAMID_SCAN_INTERVAL.get());
            updateStructure();

            if (pyramidTier != PyramidTier.NONE) {
                broadcastBoostToTurbines();

                // Entropy Generation Logic
                if (isActive) {
                    // Check if we have ISA for infinite production
                    ItemStack alloyStack = inventory.getHandler().getStackInSlot(ALLOY_SLOT);
                    boolean hasISA = !alloyStack.isEmpty() && alloyStack.is(ModItems.INERTIAL_STABILITY_ALLOY.get());
                    
                    // Check for biomass
                    ItemStack biomassStack = inventory.getHandler().getStackInSlot(BIOMASS_SLOT);
                    boolean hasBiomass = !biomassStack.isEmpty() && biomassStack.is(ModItems.ENTROPY_BIOMASS.get());
                    
                    // Either ISA (infinite) OR biomass (consumed) is needed to produce
                    if (hasISA || hasBiomass) {
                        int generation = Config.PYRAMID_CORE_GENERATION.get();
                        if (generation > 0) {
                            PyramidNetwork.get(level).insertEntropy(generation, false);
                        }
                        
                        // Consume biomass if not using ISA
                        if (!hasISA && hasBiomass) {
                            biomassTicks++;
                            if (biomassTicks >= BIOMASS_CONSUME_INTERVAL) {
                                biomassTicks = 0;
                                biomassStack.shrink(1);
                            }
                        }
                    } else {
                        // No fuel - deactivate
                        setActive(false);
                    }
                }
            } else {
                if (isActive)
                    setActive(false);
                structureValid = false;
            }
        }

        // 2. Framework Tick
        super.onUpdateServer();
    }

    private void updateStructure() {
        PyramidTier newTier = PyramidStructureValidator.validate(level, worldPosition);
        if (newTier != pyramidTier) {
            pyramidTier = newTier;
            boostMultiplier = newTier.getMultiplier();
            structureValid = (newTier != PyramidTier.NONE);
            setChanged();
            markForSync();
        }
    }

    private void broadcastBoostToTurbines() {
        int range = pyramidTier.getScanRange();
        BlockPos.betweenClosed(worldPosition.offset(-range, -range, -range), worldPosition.offset(range, range, range))
                .forEach(pos -> {
                    if (level.isLoaded(pos)
                            && level.getBlockEntity(pos) instanceof QuantumVacuumTurbineBlockEntity turbine) {
                        turbine.setPyramidBoostMultiplier(boostMultiplier);
                    }
                });
    }

    // ==================== Activation API ====================

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

    /**
     * Activate pyramid with Inertial Stability Alloy (infinite production until off)
     */
    public boolean activatePyramid(Player player) {
        updateStructure();
        if (pyramidTier != PyramidTier.NONE) {
            // Set owner
            this.ownerUUID = player.getUUID();
            this.ownerName = player.getName().getString();
            
            // Consume ISA if present
            ItemStack alloyStack = inventory.getHandler().getStackInSlot(ALLOY_SLOT);
            if (!alloyStack.isEmpty() && alloyStack.is(ModItems.INERTIAL_STABILITY_ALLOY.get())) {
                alloyStack.shrink(1);
                setActive(true);
                return true;
            }
            // If no ISA, check for biomass
            ItemStack biomassStack = inventory.getHandler().getStackInSlot(BIOMASS_SLOT);
            if (!biomassStack.isEmpty() && biomassStack.is(ModItems.ENTROPY_BIOMASS.get())) {
                setActive(true);
                return true;
            }
        }
        return false;
    }

    public boolean activatePyramidWithAnkh(Player player) {
        updateStructure();
        if (pyramidTier != PyramidTier.NONE) {
            // Set owner
            this.ownerUUID = player.getUUID();
            this.ownerName = player.getName().getString();
            
            // Check for biomass
            ItemStack biomassStack = inventory.getHandler().getStackInSlot(BIOMASS_SLOT);
            if (!biomassStack.isEmpty() && biomassStack.is(ModItems.ENTROPY_BIOMASS.get())) {
                setActive(true);
                return true;
            }
        }
        return false;
    }

    public void deactivatePyramid() {
        setActive(false);
    }
    
    // ==================== Owner Getters ====================
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public boolean isOwner(Player player) {
        if (ownerUUID == null) return true; // No owner = anyone can use
        return player.getUUID().equals(ownerUUID);
    }

    // ==================== IMachineProcess Implementation ====================

    @Override
    public boolean canProcess() {
        return false;
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
    protected boolean isSlotValid(int slot, @NotNull ItemStack stack) {
        if (slot == ALLOY_SLOT) {
            return stack.is(ModItems.INERTIAL_STABILITY_ALLOY.get());
        } else if (slot == BIOMASS_SLOT) {
            return stack.is(ModItems.ENTROPY_BIOMASS.get());
        }
        return false;
    }

    @Override
    protected int getContainerDataValue(int index) {
        return switch (index) {
            case 8 -> net.nicotfpn.alientech.util.EnergyUtils
                    .lowBits(level != null ? PyramidNetwork.get(level).getEntropyAvailable() : 0);
            case 9 -> net.nicotfpn.alientech.util.EnergyUtils
                    .highBits(level != null ? PyramidNetwork.get(level).getEntropyAvailable() : 0);
            case 10 -> net.nicotfpn.alientech.util.EnergyUtils
                    .lowBits(level != null ? PyramidNetwork.get(level).getNetworkCapacity() : 0);
            case 11 -> net.nicotfpn.alientech.util.EnergyUtils
                    .highBits(level != null ? PyramidNetwork.get(level).getNetworkCapacity() : 0);
            case 12 -> isActive ? 1 : 0;
            case 13 -> inventory.getHandler().getStackInSlot(ALLOY_SLOT).getCount();
            case 14 -> inventory.getHandler().getStackInSlot(BIOMASS_SLOT).getCount();
            default -> super.getContainerDataValue(index);
        };
    }

    @Override
    protected int getContainerDataCount() {
        return 15;
    }

    // ==================== SlotAccessRules Implementation ====================

    @Override
    public boolean canInsert(int slot, @NotNull ItemStack stack, @Nullable Direction side) {
        return isSlotValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, @Nullable Direction side) {
        return false;
    }

    // ==================== Getters ====================

    public IItemHandler getItemHandler() {
        return inventory.getHandler();
    }

    public PyramidTier getPyramidTier() {
        return pyramidTier;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    // ==================== Menu & Drops ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.pyramid_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory pInv, Player pPlayer) {
        return new PyramidCoreMenu(id, pInv, this, this.data);
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getHandler().getSlots());
        for (int i = 0; i < inventory.getHandler().getSlots(); i++) {
            container.setItem(i, inventory.getHandler().getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    // ==================== Persistence ====================

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        isActive = SafeNBT.getBoolean(tag, "IsActive", false);
        structureValid = SafeNBT.getBoolean(tag, "StructureValid", false);
        int tierOrd = SafeNBT.getInt(tag, "PyramidTier", PyramidTier.NONE.ordinal());
        pyramidTier = (tierOrd >= 0 && tierOrd < PyramidTier.values().length) ? PyramidTier.values()[tierOrd]
                : PyramidTier.NONE;
        boostMultiplier = StateValidator.clampMultiplier(SafeNBT.getFloat(tag, "BoostMultiplier", 1.0f), 1.0f, 1000.0f);
        
        // Load owner
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        ownerName = SafeNBT.getString(tag, "OwnerName", null);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("IsActive", isActive);
        tag.putBoolean("StructureValid", structureValid);
        tag.putInt("PyramidTier", pyramidTier.ordinal());
        tag.putFloat("BoostMultiplier", boostMultiplier);
        
        // Save owner
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        if (ownerName != null) {
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            PyramidNetwork.get(level).registerCore(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            PyramidNetwork.get(level).unregisterCore(worldPosition);
        }
        super.setRemoved();
    }
}
