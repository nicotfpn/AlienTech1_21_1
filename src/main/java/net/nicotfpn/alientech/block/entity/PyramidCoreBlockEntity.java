package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.EnergyComponent;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;
import net.nicotfpn.alientech.machine.core.component.AutoTransferComponent;
import net.nicotfpn.alientech.machine.turbine.QuantumVacuumTurbineBlockEntity;
import net.nicotfpn.alientech.pyramid.PyramidNetwork;
import net.nicotfpn.alientech.pyramid.PyramidStructureValidator;
import net.nicotfpn.alientech.pyramid.PyramidTier;
import net.nicotfpn.alientech.screen.PyramidCoreMenu;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.StateValidator;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Pyramid Core — The central brain of a pyramid structure.
 * Handles structure validation, entropy generation, and turbine boosting.
 * <p>
 * ECS Architecture:
 * - {@link InventoryComponent}: 2 slots (ISA + Biomass)
 * - {@link EnergyComponent}: FE buffer (for comparator output)
 * <p>
 * Mechanics:
 * - Requires Entropy Biomass to run (consumed over time)
 * - Inertial Stability Alloy enables infinite production until turned off
 * - Tracks owner who activated the pyramid
 */
public class PyramidCoreBlockEntity extends AlienMachineBlockEntity implements net.minecraft.world.MenuProvider {

    // ==================== Slot Constants ====================
    public static final int ALLOY_SLOT = 0;
    public static final int BIOMASS_SLOT = 1;
    private static final int SLOT_COUNT = 2;

    // ==================== Components ====================
    public final InventoryComponent inventoryComponent;
    public final EnergyComponent energyComponent;
    public final SideConfigComponent sideConfig;
    public final AutoTransferComponent autoTransfer;

    // ==================== State ====================
    private boolean isActive = false;
    private boolean structureValid = false;
    private int structureCheckCooldown = 0;
    private PyramidTier pyramidTier = PyramidTier.NONE;
    private float boostMultiplier = 1.0f;

    // Owner tracking
    private UUID ownerUUID = null;
    private String ownerName = null;

    // Biomass consumption tracker
    private int biomassTicks = 0;
    private static final int BIOMASS_CONSUME_INTERVAL = 200; // ~10 seconds

    // ==================== Constructor ====================

    public PyramidCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYRAMID_CORE_BE.get(), pos, state);

        // 2 slots: Alloy (ISA) + Biomass
        this.inventoryComponent = new InventoryComponent(this, SLOT_COUNT, this::isSlotValid);
        registerComponent(this.inventoryComponent);

        // FE buffer for comparator output
        this.energyComponent = new EnergyComponent(this,
                Config.PYRAMID_CORE_CAPACITY.get(),
                Config.PYRAMID_CORE_CAPACITY.get(),
                Config.PYRAMID_CORE_CAPACITY.get());
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
        if (slot == ALLOY_SLOT)
            return stack.is(ModItems.INERTIAL_STABILITY_ALLOY.get());
        if (slot == BIOMASS_SLOT)
            return stack.is(ModItems.ENTROPY_BIOMASS.get());
        return false;
    }

    // ==================== Core Tick Logic ====================

    @Override
    public void tickServer() {
        super.tickServer(); // Ticks ECS components

        if (level == null || level.isClientSide)
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
                    ItemStack alloyStack = inventoryComponent.getHandler().getStackInSlot(ALLOY_SLOT);
                    boolean hasISA = !alloyStack.isEmpty() && alloyStack.is(ModItems.INERTIAL_STABILITY_ALLOY.get());

                    ItemStack biomassStack = inventoryComponent.getHandler().getStackInSlot(BIOMASS_SLOT);
                    boolean hasBiomass = !biomassStack.isEmpty() && biomassStack.is(ModItems.ENTROPY_BIOMASS.get());

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
                        setActive(false);
                    }
                }
            } else {
                if (isActive)
                    setActive(false);
                structureValid = false;
            }
        }
    }

    private void updateStructure() {
        PyramidTier newTier = PyramidStructureValidator.validate(level, worldPosition);
        if (newTier != pyramidTier) {
            pyramidTier = newTier;
            boostMultiplier = newTier.getMultiplier();
            structureValid = (newTier != PyramidTier.NONE);
            setChanged();
            syncToClients();
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
            syncToClients();
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean activatePyramid(Player player) {
        updateStructure();
        if (pyramidTier != PyramidTier.NONE) {
            this.ownerUUID = player.getUUID();
            this.ownerName = player.getName().getString();

            ItemStack alloyStack = inventoryComponent.getHandler().getStackInSlot(ALLOY_SLOT);
            if (!alloyStack.isEmpty() && alloyStack.is(ModItems.INERTIAL_STABILITY_ALLOY.get())) {
                alloyStack.shrink(1);
                setActive(true);
                return true;
            }
            ItemStack biomassStack = inventoryComponent.getHandler().getStackInSlot(BIOMASS_SLOT);
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
            this.ownerUUID = player.getUUID();
            this.ownerName = player.getName().getString();

            ItemStack biomassStack = inventoryComponent.getHandler().getStackInSlot(BIOMASS_SLOT);
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
        if (ownerUUID == null)
            return true;
        return player.getUUID().equals(ownerUUID);
    }

    // ==================== Component Accessors ====================

    public IItemHandler getItemHandler() {
        return inventoryComponent.getHandler();
    }

    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorage() {
        return energyComponent.getEnergyStorage();
    }

    public PyramidTier getPyramidTier() {
        return pyramidTier;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    // ==================== Drops ====================

    public void drops() {
        if (level != null) {
            inventoryComponent.dropAll(level, worldPosition);
        }
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.pyramid_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory pInv, @NotNull Player pPlayer) {
        return new PyramidCoreMenu(id, pInv, this);
    }

    // ==================== Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); // Saves ECS components
        tag.putBoolean("IsActive", isActive);
        tag.putBoolean("StructureValid", structureValid);
        tag.putInt("PyramidTier", pyramidTier.ordinal());
        tag.putFloat("BoostMultiplier", boostMultiplier);
        if (ownerUUID != null)
            tag.putUUID("OwnerUUID", ownerUUID);
        if (ownerName != null)
            tag.putString("OwnerName", ownerName);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); // Loads ECS components

        isActive = SafeNBT.getBoolean(tag, "IsActive", false);
        structureValid = SafeNBT.getBoolean(tag, "StructureValid", false);
        int tierOrd = SafeNBT.getInt(tag, "PyramidTier", PyramidTier.NONE.ordinal());
        pyramidTier = (tierOrd >= 0 && tierOrd < PyramidTier.values().length) ? PyramidTier.values()[tierOrd]
                : PyramidTier.NONE;
        boostMultiplier = StateValidator.clampMultiplier(SafeNBT.getFloat(tag, "BoostMultiplier", 1.0f), 1.0f, 1000.0f);

        if (tag.hasUUID("OwnerUUID"))
            ownerUUID = tag.getUUID("OwnerUUID");
        ownerName = SafeNBT.getString(tag, "OwnerName", null);

        // === Legacy NBT Migration ===
        if (!tag.contains("Components")) {
            if (tag.contains("MachineEnergy")) {
                CompoundTag energyTag = tag.getCompound("MachineEnergy");
                if (energyTag.contains("Stored")) {
                    energyComponent.getEnergyStorage().setEnergy(energyTag.getInt("Stored"));
                }
            }
        }
    }

    private void syncToClients() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
