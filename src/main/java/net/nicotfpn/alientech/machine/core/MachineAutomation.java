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
    private int autoPushTimer = 0;
    private final Map<Direction, IItemHandler> sidedCache = new EnumMap<>(Direction.class);

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
    }

    // ==================== Auto-Push Outputs ====================

    /**
     * Periodically push items from output slots to adjacent inventories.
     * Runs every AUTO_PUSH_INTERVAL ticks (0.5 seconds).
     *
     * @param level       the server level
     * @param pos         the machine's block position
     * @param inventory   the machine inventory
     * @param outputSlots indices of output slots to push from
     * @param rules       slot access rules (used to check canExtract per direction)
     */
    public void autoPushOutputs(Level level, BlockPos pos, MachineInventory inventory,
            int[] outputSlots, SlotAccessRules rules) {
        autoPushTimer++;
        if (autoPushTimer < AUTO_PUSH_INTERVAL)
            return;
        autoPushTimer = 0;

        if (outputSlots == null || outputSlots.length == 0)
            return;

        for (int outputSlot : outputSlots) {
            ItemStack outputStack = inventory.getHandler().getStackInSlot(outputSlot);
            if (outputStack.isEmpty())
                continue;

            for (Direction direction : Direction.values()) {
                if (!rules.canExtract(outputSlot, direction))
                    continue;

                BlockPos adjacentPos = pos.relative(direction);
                if (!level.isLoaded(adjacentPos))
                    continue;

                IItemHandler adjacentHandler = level.getCapability(
                        Capabilities.ItemHandler.BLOCK, adjacentPos, direction.getOpposite());
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
