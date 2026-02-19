package net.nicotfpn.alientech.machine.core;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Defines per-slot, per-direction insertion and extraction rules for machine
 * automation.
 * Implemented by each concrete machine to define its sided behavior.
 */
public interface SlotAccessRules {

    /**
     * Whether the given item can be inserted into the given slot from the given
     * side.
     *
     * @param slot  the inventory slot index
     * @param stack the item being inserted
     * @param side  the direction from which insertion is attempted (null =
     *              internal/GUI)
     */
    boolean canInsert(int slot, ItemStack stack, @Nullable Direction side);

    /**
     * Whether items can be extracted from the given slot from the given side.
     *
     * @param slot the inventory slot index
     * @param side the direction from which extraction is attempted (null =
     *             internal/GUI)
     */
    boolean canExtract(int slot, @Nullable Direction side);
}
