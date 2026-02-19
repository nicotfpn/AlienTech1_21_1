package net.nicotfpn.alientech.block.entity;

/**
 * Redstone control modes for AlienTech machines.
 * Allows players to control when machines operate based on redstone signals.
 */
public enum RedstoneMode {
    /**
     * Machine ignores redstone signal and always operates (default).
     */
    IGNORED("tooltip.alientech.redstone.ignored", 0x808080),

    /**
     * Machine only operates when receiving a redstone signal (power > 0).
     */
    HIGH("tooltip.alientech.redstone.high", 0xFF0000),

    /**
     * Machine only operates when NOT receiving a redstone signal (power = 0).
     */
    LOW("tooltip.alientech.redstone.low", 0x00FF00),

    /**
     * Machine runs once per rising edge (0 -> power > 0 transition).
     */
    PULSE("tooltip.alientech.redstone.pulse", 0xFFFF00);

    private final String translationKey;
    private final int color;

    RedstoneMode(String translationKey, int color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    /**
     * Gets the translation key for this mode's tooltip.
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Gets the display color for this mode (ARGB format without alpha).
     */
    public int getColor() {
        return color;
    }

    /**
     * Cycles to the next mode in order.
     */
    public RedstoneMode next() {
        RedstoneMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Cycles to the previous mode in order.
     */
    public RedstoneMode previous() {
        RedstoneMode[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }

    /**
     * Checks if the machine should operate given the current redstone power level.
     * 
     * @param power  The redstone power level (0-15)
     * @param wasLow Whether the previous tick had no power (for pulse detection)
     * @return true if the machine should operate
     */
    public boolean shouldOperate(int power, boolean wasLow) {
        return switch (this) {
            case IGNORED -> true;
            case HIGH -> power > 0;
            case LOW -> power == 0;
            case PULSE -> power > 0 && wasLow;
        };
    }

    /**
     * Gets a RedstoneMode from its ordinal value, with IGNORED as default.
     */
    public static RedstoneMode fromOrdinal(int ordinal) {
        RedstoneMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IGNORED;
    }
}
