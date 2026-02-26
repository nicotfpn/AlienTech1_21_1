package net.nicotfpn.alientech.machine.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.EnergyComponent;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.screen.QuantumVacuumTurbineMenu;
import net.nicotfpn.alientech.util.SafeNBT;
import net.nicotfpn.alientech.util.StateValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Quantum Vacuum Turbine — Converts Decaying Gravitons into FE energy.
 * <p>
 * ECS Architecture:
 * - {@link InventoryComponent}: single fuel slot (Decaying Graviton)
 * - {@link EnergyComponent}: FE output buffer
 * <p>
 * Entropy is extracted directly from the player's {@link AlienEnergyNetwork}
 * without local persistent storage — zero duplication risk.
 */
public class QuantumVacuumTurbineBlockEntity extends AlienMachineBlockEntity implements MenuProvider {

    // ==================== Components ====================
    public final InventoryComponent inventoryComponent;
    public final EnergyComponent energyComponent;

    // ==================== Fuel Burn State (volatile, restored from NBT)
    // ====================
    private int burnTime;
    private int maxBurnTime;

    // ==================== Owner & Boost ====================
    private UUID ownerId;
    private float pyramidBoostMultiplier = 1.0f;
    private int boostTicksRemaining = 0;
    private static final int BOOST_EXPIRY_TICKS = 250;

    // ==================== Volatile Tracking (NOT saved to NBT)
    // ====================
    /** Tracks last tick's entropy draw for UI display */
    private long lastEntropyDraw = 0;

    // ==================== Constructor ====================

    public QuantumVacuumTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_VACUUM_TURBINE_BE.get(), pos, state);

        // 1 slot: fuel (Decaying Graviton)
        this.inventoryComponent = new InventoryComponent(this, 1, this::isSlotValid);
        addComponent(this.inventoryComponent);

        // FE output buffer — receives 0, extracts at capacity rate
        this.energyComponent = new EnergyComponent(this,
                Config.QVT_ENERGY_CAPACITY.get(), 0, Config.QVT_ENERGY_CAPACITY.get());
        addComponent(this.energyComponent);
    }

    // ==================== Slot Validation ====================

    private boolean isSlotValid(int slot, @NotNull ItemStack stack) {
        return slot == 0 && stack.is(ModItems.DECAYING_GRAVITON.get());
    }

    // ==================== Owner ====================

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    public UUID getOwner() {
        return ownerId;
    }

    // ==================== Core Tick Logic ====================

    @Override
    public void tickServer() {
        super.tickServer(); // Tick ECS components

        if (level == null || level.isClientSide)
            return;

        // === Boost Expiry ===
        if (boostTicksRemaining > 0) {
            boostTicksRemaining--;
        } else if (pyramidBoostMultiplier > 1.0f) {
            pyramidBoostMultiplier = 1.0f;
            setChanged();
        }

        // === Fuel Consumption ===
        if (burnTime > 0) {
            burnTime--;
        }

        // Try to consume a new fuel item when burn expires
        if (burnTime <= 0) {
            ItemStack fuel = inventoryComponent.getHandler().getStackInSlot(0);
            if (!fuel.isEmpty() && fuel.is(ModItems.DECAYING_GRAVITON.get())) {
                // Only burn if we have space for energy
                if (energyComponent.getEnergyStorage().getEnergyStored() < energyComponent.getEnergyStorage()
                        .getMaxEnergyStored()) {
                    maxBurnTime = Config.QVT_BURN_TIME_PER_GRAVITON.get();
                    burnTime = maxBurnTime;
                    fuel.shrink(1);
                    setChanged();
                }
            }
        }

        // === Energy Generation (from fuel burn) ===
        if (burnTime > 0) {
            int baseFE = Config.QVT_FE_PER_TICK.get();
            if (baseFE > 0) {
                float boost = Math.max(1.0f, pyramidBoostMultiplier);
                int generated = (int) Math.min((long) baseFE * (long) boost, Integer.MAX_VALUE);
                energyComponent.getEnergyStorage().setEnergy(
                        Math.min(energyComponent.getEnergyStorage().getEnergyStored() + generated,
                                energyComponent.getEnergyStorage().getMaxEnergyStored()));
                setChanged();
            }
        }

        // === Energy Push to Neighbors ===
        if (energyComponent.getEnergyStorage().getEnergyStored() > 0) {
            pushEnergyToNeighbors();
        }
    }

    private void pushEnergyToNeighbors() {
        int pushRate = Config.QVT_MAX_PUSH_PER_TICK.get();
        if (pushRate <= 0)
            return;

        for (Direction dir : Direction.values()) {
            if (energyComponent.getEnergyStorage().getEnergyStored() <= 0)
                break;

            var cap = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());

            if (cap != null && cap.canReceive()) {
                int toSend = Math.min(pushRate, energyComponent.getEnergyStorage().getEnergyStored());
                int accepted = cap.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    energyComponent.getEnergyStorage().setEnergy(
                            energyComponent.getEnergyStorage().getEnergyStored() - accepted);
                }
            }
        }
    }

    // ==================== Component Accessors ====================

    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorage() {
        return energyComponent.getEnergyStorage();
    }

    public net.neoforged.neoforge.items.IItemHandler getFuelInventory() {
        return inventoryComponent.getHandler();
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    // ==================== Pyramid Boost API ====================

    public float getPyramidBoostMultiplier() {
        return pyramidBoostMultiplier;
    }

    public void setPyramidBoostMultiplier(float multiplier) {
        multiplier = StateValidator.clampMultiplier(multiplier, 1.0f, 1000.0f);
        if (multiplier >= pyramidBoostMultiplier) {
            pyramidBoostMultiplier = multiplier;
            boostTicksRemaining = BOOST_EXPIRY_TICKS;
            setChanged();
        }
    }

    // ==================== NBT Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); // Saves ECS components (Inventory + Energy)
        tag.putFloat("PyramidBoost", pyramidBoostMultiplier);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); // Loads ECS components

        float boost = SafeNBT.getFloat(tag, "PyramidBoost", 1.0f);
        pyramidBoostMultiplier = StateValidator.clampMultiplier(boost, 1.0f, 1000.0f);

        if (tag.contains("BurnTime"))
            burnTime = tag.getInt("BurnTime");
        if (tag.contains("MaxBurnTime"))
            maxBurnTime = tag.getInt("MaxBurnTime");
        if (tag.hasUUID("Owner"))
            ownerId = tag.getUUID("Owner");

        // === Legacy NBT Migration ===
        // Old AbstractMachineBlockEntity stored energy under "MachineEnergy" and
        // inventory under "Inventory"
        // The new ECS components use "Components.Energy" and "Components.Inventory"
        // AlienMachineBlockEntity.loadAdditional handles "Components" automatically.
        // If legacy keys exist but "Components" does not, migrate them.
        if (!tag.contains("Components")) {
            // Energy migration
            if (tag.contains("MachineEnergy")) {
                CompoundTag energyTag = tag.getCompound("MachineEnergy");
                if (energyTag.contains("Stored")) {
                    energyComponent.getEnergyStorage().setEnergy(energyTag.getInt("Stored"));
                }
            }
        }
    }

    // ==================== Block Removal ====================

    public void drops() {
        if (level != null) {
            inventoryComponent.dropAll(level, worldPosition);
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
        return new QuantumVacuumTurbineMenu(containerId, playerInventory, this);
    }
}
