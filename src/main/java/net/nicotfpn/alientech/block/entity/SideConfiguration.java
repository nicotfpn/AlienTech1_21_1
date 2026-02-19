package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stores the I/O configuration for all 6 sides of a machine block.
 * Handles relative directions (FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM)
 * based on the block's facing direction.
 * 
 * Similar to Mekanism's side configuration system.
 */
public class SideConfiguration {

    /** The actual configuration stored by relative side */
    private final Map<RelativeSide, SideMode> configuration;

    /** Default mode for new configurations */
    private final SideMode defaultMode;

    /**
     * Represents sides relative to the block's facing direction.
     */
    public enum RelativeSide {
        FRONT("front"),
        BACK("back"),
        LEFT("left"),
        RIGHT("right"),
        TOP("top"),
        BOTTOM("bottom");

        private final String name;

        RelativeSide(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static RelativeSide fromName(String name) {
            for (RelativeSide side : values()) {
                if (side.name.equals(name)) {
                    return side;
                }
            }
            return FRONT;
        }
    }

    /**
     * Creates a new SideConfiguration with all sides set to NONE.
     */
    public SideConfiguration() {
        this(SideMode.NONE);
    }

    /**
     * Creates a new SideConfiguration with all sides set to the default mode.
     */
    public SideConfiguration(SideMode defaultMode) {
        this.defaultMode = defaultMode;
        this.configuration = new EnumMap<>(RelativeSide.class);
        reset();
    }

    /**
     * Resets all sides to the default mode.
     */
    public void reset() {
        for (RelativeSide side : RelativeSide.values()) {
            configuration.put(side, defaultMode);
        }
    }

    /**
     * Gets the mode for a relative side.
     */
    public SideMode getMode(RelativeSide side) {
        return configuration.getOrDefault(side, defaultMode);
    }

    /**
     * Sets the mode for a relative side.
     */
    public void setMode(RelativeSide side, SideMode mode) {
        configuration.put(side, mode);
    }

    /**
     * Cycles to the next mode for a relative side.
     */
    public void cycleMode(RelativeSide side) {
        setMode(side, getMode(side).next());
    }

    /**
     * Cycles to the previous mode for a relative side (right-click).
     */
    public void cycleModeReverse(RelativeSide side) {
        setMode(side, getMode(side).previous());
    }

    /**
     * Gets the mode for an absolute direction, considering the block's facing.
     * 
     * @param absoluteDirection The world direction (NORTH, SOUTH, etc.)
     * @param blockFacing       The direction the block is facing
     * @return The SideMode for that direction
     */
    public SideMode getModeForDirection(Direction absoluteDirection, Direction blockFacing) {
        RelativeSide relativeSide = getRelativeSide(absoluteDirection, blockFacing);
        return getMode(relativeSide);
    }

    /**
     * Converts an absolute direction to a relative side based on block facing.
     * 
     * @param absoluteDirection The world direction
     * @param blockFacing       The direction the block is facing (its "front")
     * @return The relative side
     */
    public static RelativeSide getRelativeSide(Direction absoluteDirection, Direction blockFacing) {
        // Handle vertical directions first
        if (absoluteDirection == Direction.UP) {
            return RelativeSide.TOP;
        }
        if (absoluteDirection == Direction.DOWN) {
            return RelativeSide.BOTTOM;
        }

        // For horizontal directions, calculate relative to block facing
        // blockFacing is the "front" of the block
        if (absoluteDirection == blockFacing) {
            return RelativeSide.FRONT;
        }
        if (absoluteDirection == blockFacing.getOpposite()) {
            return RelativeSide.BACK;
        }
        if (absoluteDirection == blockFacing.getClockWise()) {
            return RelativeSide.RIGHT;
        }
        if (absoluteDirection == blockFacing.getCounterClockWise()) {
            return RelativeSide.LEFT;
        }

        // Fallback
        return RelativeSide.FRONT;
    }

    /**
     * Converts a relative side to an absolute direction based on block facing.
     */
    public static Direction getAbsoluteDirection(RelativeSide relativeSide, Direction blockFacing) {
        return switch (relativeSide) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case FRONT -> blockFacing;
            case BACK -> blockFacing.getOpposite();
            case RIGHT -> blockFacing.getClockWise();
            case LEFT -> blockFacing.getCounterClockWise();
        };
    }

    // ==================== NBT Serialization ====================

    private static final String NBT_KEY = "SideConfig";

    /**
     * Saves the configuration to NBT.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (RelativeSide side : RelativeSide.values()) {
            tag.putInt(side.getName(), getMode(side).ordinal());
        }
        return tag;
    }

    /**
     * Loads the configuration from NBT.
     */
    public void load(CompoundTag tag) {
        for (RelativeSide side : RelativeSide.values()) {
            if (tag.contains(side.getName())) {
                int ordinal = tag.getInt(side.getName());
                setMode(side, SideMode.fromOrdinal(ordinal));
            }
        }
    }

    /**
     * Saves this configuration to a parent NBT tag.
     */
    public void saveToTag(CompoundTag parentTag) {
        parentTag.put(NBT_KEY, save());
    }

    /**
     * Loads this configuration from a parent NBT tag.
     */
    public void loadFromTag(CompoundTag parentTag) {
        if (parentTag.contains(NBT_KEY)) {
            load(parentTag.getCompound(NBT_KEY));
        }
    }

    // ==================== Convenience Methods ====================

    /**
     * Checks if input is allowed from a given absolute direction.
     */
    public boolean allowsInputFrom(Direction direction, Direction blockFacing) {
        return getModeForDirection(direction, blockFacing).allowsInput();
    }

    /**
     * Checks if output is allowed to a given absolute direction.
     */
    public boolean allowsOutputTo(Direction direction, Direction blockFacing) {
        return getModeForDirection(direction, blockFacing).allowsOutput();
    }

    /**
     * Creates a copy of this configuration.
     */
    public SideConfiguration copy() {
        SideConfiguration copy = new SideConfiguration(defaultMode);
        for (RelativeSide side : RelativeSide.values()) {
            copy.setMode(side, getMode(side));
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SideConfiguration{");
        for (RelativeSide side : RelativeSide.values()) {
            sb.append(side.getName()).append("=").append(getMode(side).getSerializedName());
            if (side.ordinal() < RelativeSide.values().length - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
