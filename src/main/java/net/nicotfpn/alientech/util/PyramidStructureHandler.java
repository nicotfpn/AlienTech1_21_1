package net.nicotfpn.alientech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * PyramidStructureHandler - Logic for Pyramid Multiblock validation.
 * 
 * Separates the "What is a pyramid" logic from the "What the pyramid does"
 * logic.
 * This allows easier expansion or modification of the structure rules without
 * touching the BE logic.
 */
public final class PyramidStructureHandler {

    private PyramidStructureHandler() {
    }

    /**
     * Checks if a valid pyramid structure exists below the core.
     * 
     * Structure Definition:
     * - Level -1 (relative to core): 3x3 of Neutrion Blocks, centered on core.
     * - Core must be on top of the center block.
     * 
     * @param level   The level.
     * @param corePos The position of the Pyramid Core.
     * @return true if structure is valid.
     */
    public static boolean isValidPyramid(Level level, BlockPos corePos) {
        if (level == null)
            return false;

        // Layer -1: 3x3 Gold Blocks
        if (!checkLayer(level, corePos, -1, 1, net.minecraft.world.level.block.Blocks.GOLD_BLOCK)) {
            return false;
        }

        // Layer -2: 5x5 Sandstone
        if (!checkLayer(level, corePos, -2, 2, net.minecraft.world.level.block.Blocks.SANDSTONE)) {
            return false;
        }

        // Layer -3: 7x7 Sandstone
        if (!checkLayer(level, corePos, -3, 3, net.minecraft.world.level.block.Blocks.SANDSTONE)) {
            return false;
        }

        // Layer -4: 9x9 Sandstone
        if (!checkLayer(level, corePos, -4, 4, net.minecraft.world.level.block.Blocks.SANDSTONE)) {
            return false;
        }

        return true;
    }

    private static boolean checkLayer(Level level, BlockPos center, int yOffset, int radius,
            net.minecraft.world.level.block.Block block) {
        BlockPos layerCenter = center.offset(0, yOffset, 0);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = layerCenter.offset(x, 0, z);
                // Allow variants for sandstone
                if (block == net.minecraft.world.level.block.Blocks.SANDSTONE) {
                    if (!isSandstone(level, pos))
                        return false;
                } else {
                    if (!level.getBlockState(pos).is(block))
                        return false;
                }
            }
        }
        return true;
    }

    private static boolean isSandstone(Level level, BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        return state.is(net.minecraft.world.level.block.Blocks.SANDSTONE) ||
                state.is(net.minecraft.world.level.block.Blocks.CHISELED_SANDSTONE) ||
                state.is(net.minecraft.world.level.block.Blocks.CUT_SANDSTONE) ||
                state.is(net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE);
    }
}
