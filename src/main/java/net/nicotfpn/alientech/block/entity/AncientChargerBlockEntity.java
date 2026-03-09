package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.client.IHudProvider;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.EnergyComponent;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;
import net.nicotfpn.alientech.machine.core.component.AutoTransferComponent;
import net.nicotfpn.alientech.network.sideconfig.CapabilityType;
import net.nicotfpn.alientech.network.sideconfig.IOSideMode;
import net.nicotfpn.alientech.screen.AncientChargerMenu;
import net.nicotfpn.alientech.util.EnergyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * AncientCharger — High-throughput automated energy buffer and item
 * workstation.
 * ECS Architecture:
 * - {@link InventoryComponent}: 1 slot (charge item)
 * - {@link EnergyComponent}: large FE buffer
 */
public class AncientChargerBlockEntity extends AlienMachineBlockEntity
        implements net.minecraft.world.MenuProvider, IHudProvider {

    public static final int MAX_TRANSFER = 100_000;
    private static final int CACHE_REFRESH_INTERVAL = 100;
    private static final int SLOT_COUNT = 1;

    private final List<BlockPos> cachedCores = new ArrayList<>();
    private final List<BlockPos> cachedBatteries = new ArrayList<>();
    private long lastCacheUpdate = 0;

    // ==================== Components ====================
    public final InventoryComponent inventoryComponent;
    public final EnergyComponent energyComponent;
    public final SideConfigComponent sideConfig;
    public final AutoTransferComponent autoTransfer;

    // ==================== Constructor ====================

    public AncientChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_CHARGER_BE.get(), pos, state);

        this.inventoryComponent = new InventoryComponent(this, SLOT_COUNT, this::isSlotValid) {
            @Override
            public void save(CompoundTag tag, HolderLookup.Provider provider) {
                super.save(tag, provider);
            }
        };
        registerComponent(this.inventoryComponent);

        this.energyComponent = new EnergyComponent(this,
                Config.CHARGER_CAPACITY.get(), MAX_TRANSFER, MAX_TRANSFER);
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

    private boolean isSlotValid(int slot, @NotNull ItemStack stack) {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }

    // ==================== Tick Logic ====================

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide)
            return;

        if (level.getGameTime() - lastCacheUpdate > CACHE_REFRESH_INTERVAL) {
            refreshNeighborCache(level, worldPosition);
        }

        boolean didWork = handleEnergyIO(level);
        didWork |= chargeDockedItem();

        if (didWork) {
            setChanged();
        }

        super.tickServer();
    }

    private boolean chargeDockedItem() {
        ItemStack stack = inventoryComponent.getHandler().getStackInSlot(0);
        if (!stack.isEmpty() && energyComponent.getEnergyStorage().getEnergyStored() > 0) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canReceive()) {
                int accepted = itemEnergy.receiveEnergy(
                        Math.min(MAX_TRANSFER, energyComponent.getEnergyStorage().getEnergyStored()), false);
                if (accepted > 0) {
                    energyComponent.getEnergyStorage().extractEnergy(accepted, false);
                    inventoryComponent.getHandler().setStackInSlot(0, stack);
                    return true;
                }
            }
        }
        return false;
    }

    private void refreshNeighborCache(Level level, BlockPos center) {
        cachedCores.clear();
        cachedBatteries.clear();
        BlockPos.betweenClosedStream(center.offset(-1, -1, -1), center.offset(1, 1, 1)).forEach(pos -> {
            if (pos.equals(center))
                return;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PyramidCoreBlockEntity) {
                cachedCores.add(pos.immutable());
            } else if (be instanceof AncientBatteryBlockEntity) {
                cachedBatteries.add(pos.immutable());
            }
        });
        lastCacheUpdate = level.getGameTime();
    }

    private boolean handleEnergyIO(Level level) {
        boolean didWork = false;
        if (energyComponent.getEnergyStorage().getEnergyStored() < energyComponent.getEnergyStorage()
                .getMaxEnergyStored()) {
            for (BlockPos pos : cachedCores) {
                Direction dir = Direction.getNearest(pos.getX() - worldPosition.getX(),
                        pos.getY() - worldPosition.getY(), pos.getZ() - worldPosition.getZ());
                if (sideConfig.getMode(dir, CapabilityType.ENERGY) != IOSideMode.INPUT &&
                        sideConfig.getMode(dir, CapabilityType.ENERGY) != IOSideMode.PULL) {
                    continue;
                }
                IEnergyStorage coreCap = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                if (coreCap != null && coreCap.canExtract()) {
                    if (EnergyUtils.pullEnergy(energyComponent.getEnergyStorage(), coreCap, MAX_TRANSFER) > 0)
                        didWork = true;
                }
            }
        }
        if (energyComponent.getEnergyStorage().getEnergyStored() > 0) {
            for (BlockPos pos : cachedBatteries) {
                Direction dir = Direction.getNearest(pos.getX() - worldPosition.getX(),
                        pos.getY() - worldPosition.getY(), pos.getZ() - worldPosition.getZ());
                if (sideConfig.getMode(dir, CapabilityType.ENERGY) != IOSideMode.OUTPUT &&
                        sideConfig.getMode(dir, CapabilityType.ENERGY) != IOSideMode.PUSH) {
                    continue;
                }
                IEnergyStorage batCap = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                if (batCap != null && batCap.canReceive()) {
                    if (EnergyUtils.pushEnergy(energyComponent.getEnergyStorage(), batCap, MAX_TRANSFER) > 0)
                        didWork = true;
                }
            }
        }
        return didWork;
    }

    // ==================== IHudProvider ====================

    @Override
    public void addHudLines(List<Component> lines) {
        String storedText = EnergyUtils.formatCompact(energyComponent.getEnergyStorage().getEnergyStored());
        String maxText = EnergyUtils.formatCompact(energyComponent.getEnergyStorage().getMaxEnergyStored());
        lines.add(Component.literal("\u26A1 " + storedText + " / " + maxText + " FE").withColor(0xD4AF37));

        ItemStack stack = inventoryComponent.getHandler().getStackInSlot(0);
        if (!stack.isEmpty()) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.getMaxEnergyStored() > 0) {
                int pct = (int) (100.0f * itemEnergy.getEnergyStored() / itemEnergy.getMaxEnergyStored());
                boolean isCharging = energyComponent.getEnergyStorage().getEnergyStored() > 0
                        && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored();
                lines.add(Component.literal(isCharging ? "⚡ Charging: " : (pct >= 100 ? "✔ Full: " : "⏳ Waiting: "))
                        .withColor(isCharging ? 0x55FF55 : (pct >= 100 ? 0x00FFAA : 0xFF5555))
                        .append(stack.getHoverName().copy().withColor(0xFFFFFF)));
            }
        }
    }

    public void tickClient() {
        if (energyComponent.getEnergyStorage().getEnergyStored() > 0 && level != null) {
            if (energyComponent.getEnergyStorage().getEnergyStored() < energyComponent.getEnergyStorage()
                    .getMaxEnergyStored() && level.random.nextInt(4) == 0) {
                double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
                double y = worldPosition.getY() + 0.3 + level.random.nextDouble() * 0.4;
                double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.05, 0);
            }
        }
    }

    // ==================== Accessors ====================

    public IItemHandler getItemHandler() {
        return inventoryComponent.getHandler();
    }

    public IEnergyStorage getEnergyStorage() {
        return energyComponent.getEnergyStorage();
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.ancient_charger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        return new AncientChargerMenu(containerId, playerInventory, this);
    }

    // ==================== Persistence ====================

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

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }

    // ==================== Drops ====================

    @Override
    public void drops() {
        if (level != null) {
            inventoryComponent.dropAll(level, worldPosition);
        }
    }
}
