package net.nicotfpn.alientech.pyramid;

/**
 * Represents the validation tier of a Pyramid multiblock structure.
 * Higher tiers require larger structures and provide greater multipliers.
 * <p>
 * Structure:
 *
 * <pre>
 * Tier 3 (full): 9×9 + 7×7 + 5×5 Casing, 3×3 Gold, Core
 * Tier 2:        7×7 + 5×5 Casing, 3×3 Gold, Core
 * Tier 1:        5×5 Casing, 3×3 Gold, Core
 * NONE:          Structure incomplete
 * </pre>
 */
public enum PyramidTier {
    NONE(1.0f, 0),
    TIER_1(4.0f, 32),
    TIER_2(8.0f, 48),
    TIER_3(16.0f, 64);

    private final float boostMultiplier;
    private final int scanRange;

    PyramidTier(float boostMultiplier, int scanRange) {
        this.boostMultiplier = boostMultiplier;
        this.scanRange = scanRange;
    }

    public float getMultiplier() {
        return boostMultiplier;
    }

    public int getScanRange() {
        return scanRange;
    }
}
