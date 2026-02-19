package net.nicotfpn.alientech.pyramid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.nicotfpn.alientech.block.ModBlocks;

/**
 * Validates the Pyramid multiblock structure below a Pyramid Core.
 * <p>
 * Structure (centered on core, layers below):
 * 
 * <pre>
 * Layer -1: 3×3 Gold Blocks
 * Layer -2: 5×5 Alien Pyramid Casing  (Tier 1 minimum)
 * Layer -3: 7×7 Alien Pyramid Casing  (Tier 2 minimum)
 * Layer -4: 9×9 Alien Pyramid Casing  (Tier 3 minimum)
 * </pre>
 * <p>
 * Performance guarantees:
 * - Fail-fast on first mismatch
 * - Never loads chunks (checks isLoaded before scanning)
 * - Maximum blocks scanned: 3×3 + 5×5 + 7×7 + 9×9 = 164 blocks
 * - Called only on structure change events, NEVER every tick
 */
public final class PyramidStructureValidator {

    private PyramidStructureValidator() {
        // Static utility
    }

    /**
     * Validate the pyramid structure and return the highest achieved tier.
     * Checks tiers bottom-up: gold base first, then expanding casing layers.
     * Returns the highest complete tier (NONE if gold base fails).
     *
     * @param level   the world
     * @param corePos position of the Pyramid Core block
     * @return the highest validated PyramidTier
     */
    public static PyramidTier validate(Level level, BlockPos corePos) {
        if (level == null)
            return PyramidTier.NONE;

        // Gold base is always required (layer -1: 3×3)
        if (!checkLayer(level, corePos, -1, 1, Blocks.GOLD_BLOCK)) {
            return PyramidTier.NONE;
        }

        // Tier 1: 5×5 casing at layer -2
        Block casing = ModBlocks.ALIEN_PYRAMID_CASING.get();
        if (!checkLayer(level, corePos, -2, 2, casing)) {
            return PyramidTier.NONE;
        }

        // Tier 2: 7×7 casing at layer -3
        if (!checkLayer(level, corePos, -3, 3, casing)) {
            return PyramidTier.TIER_1;
        }

        // Tier 3: 9×9 casing at layer -4
        if (!checkLayer(level, corePos, -4, 4, casing)) {
            return PyramidTier.TIER_2;
        }

        return PyramidTier.TIER_3;
    }

    /**
     * Check a square layer of blocks below the core.
     *
     * @param level   the world
     * @param center  the core position
     * @param yOffset vertical offset from core (negative = below)
     * @param radius  half-width of the square (1 = 3×3, 2 = 5×5, etc.)
     * @param block   the required block type
     * @return true if every block in the layer matches
     */
    private static boolean checkLayer(Level level, BlockPos center, int yOffset, int radius, Block block) {
        BlockPos layerCenter = center.offset(0, yOffset, 0);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = layerCenter.offset(x, 0, z);
                // Safety: never load chunks for validation
                if (!level.isLoaded(pos))
                    return false;
                if (!level.getBlockState(pos).is(block))
                    return false;
            }
        }
        return true;
    }
}
