package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.Direction;

/**
 * Interface for block entities that support side configuration.
 * Implement this interface to enable Mekanism-style I/O configuration.
 */
public interface ISideConfigurable {

    /**
     * Gets the side configuration for this block entity.
     */
    SideConfiguration getSideConfiguration();

    /**
     * Gets the facing direction of this block (used for relative side calculation).
     */
    Direction getBlockFacing();

    /**
     * Called when the side configuration changes.
     * Use this to sync to client or update capabilities.
     */
    void onSideConfigChanged();

    /**
     * Checks if input is allowed from the given direction.
     */
    default boolean allowsInputFrom(Direction direction) {
        return getSideConfiguration().allowsInputFrom(direction, getBlockFacing());
    }

    /**
     * Checks if output is allowed to the given direction.
     */
    default boolean allowsOutputTo(Direction direction) {
        return getSideConfiguration().allowsOutputTo(direction, getBlockFacing());
    }

    /**
     * Gets the current mode for an absolute direction.
     */
    default SideMode getModeForDirection(Direction direction) {
        return getSideConfiguration().getModeForDirection(direction, getBlockFacing());
    }

    /**
     * Cycles the mode for a relative side (left-click).
     */
    default void cycleMode(SideConfiguration.RelativeSide side) {
        getSideConfiguration().cycleMode(side);
        onSideConfigChanged();
    }

    /**
     * Cycles the mode backwards for a relative side (right-click).
     */
    default void cycleModeReverse(SideConfiguration.RelativeSide side) {
        getSideConfiguration().cycleModeReverse(side);
        onSideConfigChanged();
    }
}
