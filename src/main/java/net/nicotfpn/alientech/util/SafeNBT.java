package net.nicotfpn.alientech.util;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Safe NBT reading utilities with default values and validation.
 * <p>
 * Prevents crashes from missing or corrupted NBT data.
 */
public final class SafeNBT {

    private SafeNBT() {
        // Static utility class
    }

    /**
     * Safely get an integer from NBT with a default value.
     * 
     * @param tag the NBT tag (can be null)
     * @param key the key to read
     * @param defaultValue the default value if key is missing or invalid
     * @return the integer value or default
     */
    public static int getInt(@Nullable CompoundTag tag, @NotNull String key, int defaultValue) {
        if (tag == null || !tag.contains(key)) {
            return defaultValue;
        }
        try {
            return tag.getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely get a float from NBT with a default value.
     * 
     * @param tag the NBT tag (can be null)
     * @param key the key to read
     * @param defaultValue the default value if key is missing or invalid
     * @return the float value or default
     */
    public static float getFloat(@Nullable CompoundTag tag, @NotNull String key, float defaultValue) {
        if (tag == null || !tag.contains(key)) {
            return defaultValue;
        }
        try {
            return tag.getFloat(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely get a double from NBT with a default value.
     * 
     * @param tag the NBT tag (can be null)
     * @param key the key to read
     * @param defaultValue the default value if key is missing or invalid
     * @return the double value or default
     */
    public static double getDouble(@Nullable CompoundTag tag, @NotNull String key, double defaultValue) {
        if (tag == null || !tag.contains(key)) {
            return defaultValue;
        }
        try {
            return tag.getDouble(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely get a boolean from NBT with a default value.
     * 
     * @param tag the NBT tag (can be null)
     * @param key the key to read
     * @param defaultValue the default value if key is missing or invalid
     * @return the boolean value or default
     */
    public static boolean getBoolean(@Nullable CompoundTag tag, @NotNull String key, boolean defaultValue) {
        if (tag == null || !tag.contains(key)) {
            return defaultValue;
        }
        try {
            return tag.getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely get a CompoundTag from NBT.
     * 
     * @param tag the NBT tag (can be null)
     * @param key the key to read
     * @return the CompoundTag or null if missing
     */
    @Nullable
    public static CompoundTag getCompound(@Nullable CompoundTag tag, @NotNull String key) {
        if (tag == null || !tag.contains(key)) {
            return null;
        }
        try {
            return tag.getCompound(key);
        } catch (Exception e) {
            return null;
        }
    }
}
