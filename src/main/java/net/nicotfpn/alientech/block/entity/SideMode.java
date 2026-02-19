package net.nicotfpn.alientech.block.entity;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the I/O mode for a side of a configurable machine.
 * Similar to Mekanism's side configuration system.
 */
public enum SideMode implements StringRepresentable {

    /** No I/O allowed on this side */
    NONE("none", 0x555555, false, false),

    /** Input only - accepts items/energy from external sources */
    INPUT("input", 0x3366CC, true, false),

    /** Output only - pushes items/energy to adjacent blocks */
    OUTPUT("output", 0xCC6633, false, true),

    /** Both input and output allowed */
    BOTH("both", 0x33CC66, true, true);

    private final String name;
    private final int color;
    private final boolean allowsInput;
    private final boolean allowsOutput;

    SideMode(String name, int color, boolean allowsInput, boolean allowsOutput) {
        this.name = name;
        this.color = color;
        this.allowsInput = allowsInput;
        this.allowsOutput = allowsOutput;
    }

    /**
     * Gets the next mode in the cycle (for clicking through modes).
     * Order: NONE -> INPUT -> OUTPUT -> BOTH -> NONE
     */
    public SideMode next() {
        return switch (this) {
            case NONE -> INPUT;
            case INPUT -> OUTPUT;
            case OUTPUT -> BOTH;
            case BOTH -> NONE;
        };
    }

    /**
     * Gets the previous mode (for right-click cycling).
     */
    public SideMode previous() {
        return switch (this) {
            case NONE -> BOTH;
            case INPUT -> NONE;
            case OUTPUT -> INPUT;
            case BOTH -> OUTPUT;
        };
    }

    /**
     * Gets the color for rendering this mode (ARGB format).
     */
    public int getColor() {
        return 0xFF000000 | color;
    }

    /**
     * Gets the color as RGB (without alpha).
     */
    public int getColorRGB() {
        return color;
    }

    /**
     * @return true if this mode allows items/energy to enter the machine
     */
    public boolean allowsInput() {
        return allowsInput;
    }

    /**
     * @return true if this mode allows items/energy to be pushed out
     */
    public boolean allowsOutput() {
        return allowsOutput;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    /**
     * Gets a SideMode from its serialized name.
     */
    public static SideMode fromName(String name) {
        for (SideMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return NONE;
    }

    /**
     * Gets a SideMode from its ordinal value (for NBT storage).
     */
    public static SideMode fromOrdinal(int ordinal) {
        if (ordinal >= 0 && ordinal < values().length) {
            return values()[ordinal];
        }
        return NONE;
    }
}
