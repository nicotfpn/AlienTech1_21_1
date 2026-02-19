package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.base.AlienElectricBlockEntity;
import net.nicotfpn.alientech.client.IHudProvider;
import net.nicotfpn.alientech.util.EnergyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * AncientCharger - Optimized High-Throughput Energy Buffer.
 * Extends AlienElectricBlockEntity for consistent behavior.
 * Implements IHudProvider for modular HUD rendering.
 */
public class AncientChargerBlockEntity extends AlienElectricBlockEntity implements IHudProvider {

    public static final int MAX_TRANSFER = 100_000;
    private static final int CACHE_REFRESH_INTERVAL = 100; // Ticks (5 seconds)

    // Cache
    private final List<BlockPos> cachedCores = new ArrayList<>();
    private final List<BlockPos> cachedBatteries = new ArrayList<>();
    private long lastCacheUpdate = 0;

    // Inventory for "Docking"
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markInventoryDirty(); // Immediate sync for item changes
        }
    };

    public AncientChargerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ANCIENT_CHARGER_BE.get(), pos, blockState,
                Config.CHARGER_CAPACITY.get(), MAX_TRANSFER);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.alientech.ancient_charger");
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
            net.minecraft.world.entity.player.Inventory playerInventory,
            net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
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
        return new net.nicotfpn.alientech.screen.AncientChargerMenu(containerId, playerInventory, this, data);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    // ==================== HUD Provider ====================

    @Override
    public void addHudLines(List<Component> lines) {
        // Line 1: Block name (rendered separately by overlay as title)
        // Line 2: Energy
        String storedText = EnergyUtils.formatCompact(energyStorage.getEnergyStored());
        String maxText = EnergyUtils.formatCompact(energyStorage.getMaxEnergyStored());
        lines.add(Component.literal("⚡ " + storedText + " / " + maxText + " FE").withColor(0xD4AF37));

        // Line 3: Item charging status
        net.minecraft.world.item.ItemStack stack = itemHandler.getStackInSlot(0);
        if (!stack.isEmpty()) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.getMaxEnergyStored() > 0) {
                int pct = (int) (100.0f * itemEnergy.getEnergyStored() / itemEnergy.getMaxEnergyStored());
                boolean isCharging = energyStorage.getEnergyStored() > 0
                        && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored();

                int statusColor = isCharging ? 0x55FF55 : (pct >= 100 ? 0x00FFAA : 0xFF5555);
                String statusPrefix = isCharging ? "⚡ Charging: " : (pct >= 100 ? "✔ Full: " : "⏳ Waiting: ");

                lines.add(Component.literal(statusPrefix + pct + "% ")
                        .withColor(statusColor)
                        .append(stack.getHoverName().copy().withColor(0xFFFFFF)));
            } else {
                // Item has no energy capability
                lines.add(Component.literal("⚠ ").withColor(0xFFAA00)
                        .append(stack.getHoverName().copy().withColor(0xFFFFFF))
                        .append(Component.literal(" (no energy)").withColor(0xFF5555)));
            }
        }
    }

    // ==================== Ticking Logic ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, AncientChargerBlockEntity entity) {
        // 1. Maintain Cache
        if (level.getGameTime() - entity.lastCacheUpdate > CACHE_REFRESH_INTERVAL) {
            entity.refreshNeighborCache(level, pos);
        }

        // 2. Perform Operations
        entity.handleEnergyIO(level);

        // 3. Charge Item in Dock
        entity.chargeDockedItem();

        // 4. Throttled sync (from base class)
        entity.onUpdateServer();
    }

    private void chargeDockedItem() {
        if (energyStorage.getEnergyStored() <= 0)
            return;

        net.minecraft.world.item.ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty())
            return;

        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy != null && itemEnergy.canReceive()) {
            int toTransfer = Math.min(MAX_TRANSFER, energyStorage.getEnergyStored());
            int accepted = itemEnergy.receiveEnergy(toTransfer, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
                // Force the ItemStackHandler to see the NBT change
                itemHandler.setStackInSlot(0, stack);
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AncientChargerBlockEntity entity) {
        if (entity.energyStorage.getEnergyStored() > 0) {
            // Charging particles when buffer has energy but isn't full
            if (entity.energyStorage.getEnergyStored() < entity.energyStorage.getMaxEnergyStored()) {
                if (level.random.nextInt(4) == 0) {
                    double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
                    double y = pos.getY() + 0.3 + level.random.nextDouble() * 0.4;
                    double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.05, 0);
                }
            }

            // Idle energy spark
            if (level.random.nextInt(10) == 0) {
                double rx = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
                double ry = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5);
                double rz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, rx, ry, rz, 0, 0, 0);
            }
        }

        // Item charging particles
        if (!entity.itemHandler.getStackInSlot(0).isEmpty()) {
            net.minecraft.world.item.ItemStack stack = entity.itemHandler.getStackInSlot(0);
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored()) {
                if (level.random.nextInt(2) == 0) {
                    double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
                    double y = pos.getY() + 0.5;
                    double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
                    level.addParticle(ParticleTypes.WAX_ON, x, y, z, 0, 0.1, 0);
                    level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.15, 0);
                }
            }
        }
    }

    // ==================== Optimized Operations ====================

    private void refreshNeighborCache(Level level, BlockPos center) {
        this.cachedCores.clear();
        this.cachedBatteries.clear();

        BlockPos.betweenClosedStream(center.offset(-1, -1, -1), center.offset(1, 1, 1))
                .forEach(pos -> {
                    if (pos.equals(center))
                        return;
                    if (!level.isLoaded(pos))
                        return;

                    BlockEntity be = level.getBlockEntity(pos);
                    if (be == null)
                        return;

                    if (be instanceof PyramidCoreBlockEntity) {
                        this.cachedCores.add(pos.immutable());
                    } else if (be instanceof AncientBatteryBlockEntity) {
                        this.cachedBatteries.add(pos.immutable());
                    }
                });

        this.lastCacheUpdate = level.getGameTime();
    }

    private void handleEnergyIO(Level level) {
        // Pull from Cores
        if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            for (BlockPos corePos : cachedCores) {
                if (!level.isLoaded(corePos))
                    continue;

                IEnergyStorage coreCap = level.getCapability(Capabilities.EnergyStorage.BLOCK, corePos, null);
                if (coreCap != null && coreCap.canExtract()) {
                    int pullAmount = Math.min(MAX_TRANSFER,
                            energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored());
                    EnergyUtils.pullEnergy(this.energyStorage, coreCap, pullAmount);
                }
            }
        }

        // Push to Batteries
        if (energyStorage.getEnergyStored() > 0) {
            for (BlockPos batPos : cachedBatteries) {
                if (!level.isLoaded(batPos))
                    continue;

                IEnergyStorage batCap = level.getCapability(Capabilities.EnergyStorage.BLOCK, batPos, null);
                if (batCap != null && batCap.canReceive()) {
                    int pushAmount = Math.min(MAX_TRANSFER, energyStorage.getEnergyStored());
                    EnergyUtils.pushEnergy(this.energyStorage, batCap, pushAmount);
                }
            }
        }
    }

    // ==================== Persistence ====================

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
    }
}
