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
import net.nicotfpn.alientech.block.entity.base.AbstractMachineBlockEntity;
import net.nicotfpn.alientech.client.IHudProvider;
import net.nicotfpn.alientech.machine.core.IMachineProcess;
import net.nicotfpn.alientech.machine.core.SlotAccessRules;
import net.nicotfpn.alientech.screen.AncientChargerMenu;
import net.nicotfpn.alientech.util.EnergyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * AncientCharger — High-throughput automated energy buffer and item
 * workstation.
 * Ported to AbstractMachineBlockEntity framework.
 */
public class AncientChargerBlockEntity extends AbstractMachineBlockEntity
        implements IMachineProcess, SlotAccessRules, IHudProvider {

    public static final int MAX_TRANSFER = 100_000;
    private static final int CACHE_REFRESH_INTERVAL = 100;
    private static final int SLOT_COUNT = 1;

    private final List<BlockPos> cachedCores = new ArrayList<>();
    private final List<BlockPos> cachedBatteries = new ArrayList<>();
    private long lastCacheUpdate = 0;

    public AncientChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_CHARGER_BE.get(), pos, state,
                Config.CHARGER_CAPACITY.get(), MAX_TRANSFER, MAX_TRANSFER, SLOT_COUNT);
    }

    @Override
    protected void onUpdateServer() {
        if (level == null)
            return;
        if (level.getGameTime() - lastCacheUpdate > CACHE_REFRESH_INTERVAL) {
            refreshNeighborCache(level, worldPosition);
        }
        handleEnergyIO(level);
        super.onUpdateServer();
        chargeDockedItem();
    }

    private void chargeDockedItem() {
        ItemStack stack = inventory.getHandler().getStackInSlot(0);
        if (!stack.isEmpty() && energy.getEnergyStored() > 0) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canReceive()) {
                int accepted = itemEnergy.receiveEnergy(Math.min(MAX_TRANSFER, energy.getEnergyStored()), false);
                if (accepted > 0) {
                    energy.getEnergyStorage().extractEnergy(accepted, false);
                    inventory.getHandler().setStackInSlot(0, stack);
                    setChanged();
                }
            }
        }
    }

    private void refreshNeighborCache(Level level, BlockPos center) {
        cachedCores.clear();
        cachedBatteries.clear();
        BlockPos.betweenClosedStream(center.offset(-1, -1, -1), center.offset(1, 1, 1)).forEach(pos -> {
            if (pos.equals(center))
                return;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PyramidCoreBlockEntity)
                cachedCores.add(pos.immutable());
            else if (be instanceof AncientBatteryBlockEntity)
                cachedBatteries.add(pos.immutable());
        });
        lastCacheUpdate = level.getGameTime();
    }

    private void handleEnergyIO(Level level) {
        if (energy.getEnergyStored() < energy.getCapacity()) {
            for (BlockPos pos : cachedCores) {
                IEnergyStorage coreCap = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                if (coreCap != null && coreCap.canExtract()) {
                    EnergyUtils.pullEnergy(energy.getStorage(), coreCap, MAX_TRANSFER);
                }
            }
        }
        if (energy.getEnergyStored() > 0) {
            for (BlockPos pos : cachedBatteries) {
                IEnergyStorage batCap = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                if (batCap != null && batCap.canReceive()) {
                    EnergyUtils.pushEnergy(energy.getStorage(), batCap, MAX_TRANSFER);
                }
            }
        }
    }

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
        return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }

    @Override
    public boolean canInsert(int slot, @NotNull ItemStack stack, @Nullable Direction side) {
        return isSlotValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, @Nullable Direction side) {
        return true;
    }

    @Override
    public void addHudLines(List<Component> lines) {
        String storedText = EnergyUtils.formatCompact(energy.getEnergyStored());
        String maxText = EnergyUtils.formatCompact(energy.getCapacity());
        lines.add(Component.literal("\u26A1 " + storedText + " / " + maxText + " FE").withColor(0xD4AF37));
        ItemStack stack = inventory.getHandler().getStackInSlot(0);
        if (!stack.isEmpty()) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.getMaxEnergyStored() > 0) {
                int pct = (int) (100.0f * itemEnergy.getEnergyStored() / itemEnergy.getMaxEnergyStored());
                boolean isCharging = energy.getEnergyStored() > 0
                        && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored();
                lines.add(Component.literal(isCharging ? "⚡ Charging: " : (pct >= 100 ? "✔ Full: " : "⏳ Waiting: "))
                        .withColor(isCharging ? 0x55FF55 : (pct >= 100 ? 0x00FFAA : 0xFF5555))
                        .append(stack.getHoverName().copy().withColor(0xFFFFFF)));
            }
        }
    }

    @Override
    protected void onUpdateClient() {
        if (energy.getEnergyStored() > 0 && level != null) {
            if (energy.getEnergyStored() < energy.getCapacity() && level.random.nextInt(4) == 0) {
                double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
                double y = worldPosition.getY() + 0.3 + level.random.nextDouble() * 0.4;
                double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.05, 0);
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.ancient_charger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        return new AncientChargerMenu(containerId, playerInventory, this, this.data);
    }

    public IItemHandler getItemHandler() {
        return inventory.getHandler();
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }
}
