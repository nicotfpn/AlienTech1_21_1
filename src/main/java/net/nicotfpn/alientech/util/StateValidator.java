package net.nicotfpn.alientech.util;

/**
 * Internal state validation utilities.
 * <p>
 * Provides safe validation methods that clamp invalid values rather than throwing exceptions.
 * Used internally by BlockEntities and data classes to ensure state integrity.
 */
public final class StateValidator {

    private StateValidator() {
        // Static utility class
    }

    /**
     * Clamp an entropy value to valid range [0, capacity].
     * 
     * @param value the entropy value to validate
     * @param capacity the maximum capacity
     * @return clamped value in valid range
     */
    public static int clampEntropy(int value, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        if (value < 0) {
            return 0;
        }
        if (value > capacity) {
            return capacity;
        }
        return value;
    }

    /**
     * Clamp a progress value to valid range [0, maxProgress].
     * 
     * @param progress the progress value
     * @param maxProgress the maximum progress
     * @return clamped progress value
     */
    public static int clampProgress(int progress, int maxProgress) {
        if (maxProgress <= 0) {
            return 0;
        }
        if (progress < 0) {
            return 0;
        }
        if (progress > maxProgress) {
            return maxProgress;
        }
        return progress;
    }

    /**
     * Clamp a multiplier to valid range [min, max].
     * 
     * @param multiplier the multiplier value
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return clamped multiplier
     */
    public static float clampMultiplier(float multiplier, float min, float max) {
        if (multiplier < min) {
            return min;
        }
        if (multiplier > max) {
            return max;
        }
        return multiplier;
    }

    /**
     * Clamp an integer to valid range [min, max].
     * 
     * @param value the value to clamp
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return clamped value
     */
    public static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * Clamp a double to valid range [min, max].
     * 
     * @param value the value to clamp
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return clamped value
     */
    public static double clampDouble(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * Validate that a value is non-negative.
     * 
     * @param value the value to validate
     * @return 0 if negative, otherwise the value
     */
    public static int ensureNonNegative(int value) {
        return Math.max(0, value);
    }

    /**
     * Validate that a float value is non-negative.
     * 
     * @param value the value to validate
     * @return 0.0f if negative, otherwise the value
     */
    public static float ensureNonNegative(float value) {
        return Math.max(0.0f, value);
    }
}
