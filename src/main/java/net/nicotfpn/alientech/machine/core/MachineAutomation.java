package net.nicotfpn.alientech.machine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Encapsulates sided inventory access and auto-push logic for machine
 * automation.
 * Provides filtered IItemHandler wrappers per direction using SlotAccessRules.
 */
public class MachineAutomation {

    private static final int AUTO_PUSH_INTERVAL = 10;
    private final Map<Direction, IItemHandler> sidedCache = new EnumMap<>(Direction.class);
    private final Map<Direction, net.neoforged.neoforge.capabilities.BlockCapabilityCache<IItemHandler, @Nullable Direction>> autoPushCache = new EnumMap<>(
            Direction.class);
    private final Map<Direction, net.neoforged.neoforge.capabilities.BlockCapabilityCache<net.neoforged.neoforge.energy.IEnergyStorage, @Nullable Direction>> energyPushCache = new EnumMap<>(
            Direction.class);

    // ==================== Sided Handler Access ====================

    /**
     * Get an IItemHandler for the given side.
     * Returns the raw handler for null side (internal/GUI access),
     * or a filtered wrapper for directional access.
     *
     * @param side      the direction (null = internal)
     * @param inventory the machine inventory
     * @param rules     the slot access rules for this machine
     */
    public IItemHandler getHandlerForSide(@Nullable Direction side, MachineInventory inventory, SlotAccessRules rules) {
        if (side == null)
            return inventory.getHandler();
        return sidedCache.computeIfAbsent(side, s -> new SidedItemHandler(s, inventory.getHandler(), rules));
    }

    /**
     * Invalidate the sided handler cache. Call when machine configuration changes.
     */
    public void invalidateCache() {
        sidedCache.clear();
        autoPushCache.clear();
        energyPushCache.clear();
    }

    // ==================== Auto-Push Outputs ====================

    /**
     * Periodically push items from output slots to adjacent inventories.
     * Runs on a distributed offset interval using worldPosition memory.
     *
     * @param level       the server level
     * @param pos         the machine's block position
     * @param inventory   the machine inventory
     * @param outputSlots indices of output slots to push from
     * @param rules       slot access rules (used to check canExtract per direction)
     */
    public void autoPushOutputs(Level level, BlockPos pos, MachineInventory inventory,
            int[] outputSlots, SlotAccessRules rules) {

        if (level.isClientSide() || outputSlots == null || outputSlots.length == 0)
            return;

        // Distribute load across exact ticks mathematically derived from block memory
        if ((level.getGameTime() + pos.asLong()) % AUTO_PUSH_INTERVAL != 0)
            return;

        for (int i = 0; i < outputSlots.length; i++) {
            int outputSlot = outputSlots[i];
            ItemStack outputStack = inventory.getHandler().getStackInSlot(outputSlot);
            if (outputStack.isEmpty())
                continue;

            for (Direction direction : Direction.values()) {
                if (!rules.canExtract(outputSlot, direction))
                    continue;

                // Implement High-Speed Native BlockCapabilityCache (1.20.4+ spec) instead of
                // deprecated LazyOptionals
                net.neoforged.neoforge.capabilities.BlockCapabilityCache<IItemHandler, @Nullable Direction> cache = autoPushCache
                        .get(direction);
                if (cache == null) {
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLvl) {
                        cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                                Capabilities.ItemHandler.BLOCK, serverLvl, pos.relative(direction),
                                direction.getOpposite());
                        autoPushCache.put(direction, cache);
                    } else {
                        continue;
                    }
                }

                IItemHandler adjacentHandler = cache.getCapability();
                if (adjacentHandler == null)
                    continue;

                ItemStack toInsert = outputStack.copy();
                for (int slot = 0; slot < adjacentHandler.getSlots() && !toInsert.isEmpty(); slot++) {
                    toInsert = adjacentHandler.insertItem(slot, toInsert, false);
                }

                if (toInsert.getCount() < outputStack.getCount()) {
                    int pushed = outputStack.getCount() - toInsert.getCount();
                    inventory.getHandler().extractItem(outputSlot, pushed, false);
                    outputStack = inventory.getHandler().getStackInSlot(outputSlot);
                    if (outputStack.isEmpty())
                        break;
                }
            }
        }
    }

    /**
     * Periodically push energy to adjacent energy storages.
     *
     * @param level       the server level
     * @param pos         the machine's block position
     * @param energy      the machine's energy module
     * @param maxTransfer max energy to push per tick
     */
    public void autoPushEnergy(Level level, BlockPos pos, MachineEnergy energy, int maxTransfer) {
        if (level.isClientSide() || maxTransfer <= 0 || energy.getEnergyStored() <= 0)
            return;

        // Distribute load
        if ((level.getGameTime() + pos.asLong()) % AUTO_PUSH_INTERVAL != 0)
            return;

        int toPushTotal = Math.min(energy.getEnergyStored(), maxTransfer * AUTO_PUSH_INTERVAL); // Account for interval

        for (Direction direction : Direction.values()) {
            if (toPushTotal <= 0)
                break;

            net.neoforged.neoforge.capabilities.BlockCapabilityCache<net.neoforged.neoforge.energy.IEnergyStorage, @Nullable Direction> cache = energyPushCache
                    .get(direction);
            if (cache == null) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLvl) {
                    cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                            Capabilities.EnergyStorage.BLOCK, serverLvl, pos.relative(direction),
                            direction.getOpposite());
                    energyPushCache.put(direction, cache);
                } else {
                    continue;
                }
            }

            net.neoforged.neoforge.energy.IEnergyStorage adjacent = cache.getCapability();
            if (adjacent != null && adjacent.canReceive()) {
                int pushed = adjacent.receiveEnergy(toPushTotal, false);
                if (pushed > 0) {
                    energy.getEnergyStorage().extractEnergy(pushed, false);
                    toPushTotal -= pushed;
                }
            }
        }
    }

    /**
     * Periodically pull energy from adjacent energy storages.
     *
     * @param level       the server level
     * @param pos         the machine's block position
     * @param energy      the machine's energy module
     * @param maxTransfer max energy to pull per tick
     */
    public void autoPullEnergy(Level level, BlockPos pos, MachineEnergy energy, int maxTransfer) {
        if (level.isClientSide() || maxTransfer <= 0 || energy.getEnergyStored() >= energy.getCapacity())
            return;

        // Distribute load
        if ((level.getGameTime() + pos.asLong()) % AUTO_PUSH_INTERVAL != 0)
            return;

        int space = energy.getCapacity() - energy.getEnergyStored();
        int toPullTotal = Math.min(space, maxTransfer * AUTO_PUSH_INTERVAL);

        for (Direction direction : Direction.values()) {
            if (toPullTotal <= 0)
                break;

            net.neoforged.neoforge.capabilities.BlockCapabilityCache<net.neoforged.neoforge.energy.IEnergyStorage, @Nullable Direction> cache = energyPushCache
                    .get(direction);
            if (cache == null) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLvl) {
                    cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                            Capabilities.EnergyStorage.BLOCK, serverLvl, pos.relative(direction),
                            direction.getOpposite());
                    energyPushCache.put(direction, cache);
                } else {
                    continue;
                }
            }

            net.neoforged.neoforge.energy.IEnergyStorage adjacent = cache.getCapability();
            if (adjacent != null && adjacent.canExtract()) {
                int pulled = adjacent.extractEnergy(toPullTotal, false);
                if (pulled > 0) {
                    energy.getEnergyStorage().receiveEnergy(pulled, false);
                    toPullTotal -= pulled;
                }
            }
        }
    }

    // ==================== Sided Item Handler Wrapper ====================

    /**
     * IItemHandler wrapper that enforces per-slot, per-direction rules.
     * Delegates all operations to the underlying handler with access checks.
     */
    private static class SidedItemHandler implements IItemHandler {
        private final Direction side;
        private final ItemStackHandler handler;
        private final SlotAccessRules rules;

        SidedItemHandler(Direction side, ItemStackHandler handler, SlotAccessRules rules) {
            this.side = side;
            this.handler = handler;
            this.rules = rules;
        }

        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!rules.canInsert(slot, stack, side))
                return stack;
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!rules.canExtract(slot, side))
                return ItemStack.EMPTY;
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return rules.canInsert(slot, stack, side);
        }
    }
}
