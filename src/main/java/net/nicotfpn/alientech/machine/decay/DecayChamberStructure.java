package net.nicotfpn.alientech.machine.decay;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Validates the Decay Chamber multiblock structure around a controller.
 * <p>
 * Structure requirement: controller must have at least 1 adjacent
 * DecayChamberBlock
 * forming a 2-block-high chamber (one on top of another, or adjacent).
 * <p>
 * Performance: only validates immediate neighbors (6 directions), never scans
 * world.
 * Called only on activation or block neighbor change — NOT every tick.
 */
public final class DecayChamberStructure {

    private DecayChamberStructure() {
        // Static utility class
    }

    /**
     * Validate that the controller position has a valid chamber structure.
     *
     * @param level         the world
     * @param controllerPos the controller block position
     * @param chamberBlock  the expected chamber block type
     * @return true if at least one valid 2-high chamber column exists adjacent to
     *         the controller
     */
    public static boolean isValid(Level level, BlockPos controllerPos, Block chamberBlock) {
        // Check all 4 horizontal directions for a 2-high chamber column
        BlockPos[] horizontalOffsets = {
                controllerPos.north(),
                controllerPos.south(),
                controllerPos.east(),
                controllerPos.west()
        };

        for (BlockPos base : horizontalOffsets) {
            if (isChamberColumn(level, base, chamberBlock)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a position has a 2-block-high column of chamber blocks.
     */
    private static boolean isChamberColumn(Level level, BlockPos base, Block chamberBlock) {
        return level.getBlockState(base).is(chamberBlock)
                && level.getBlockState(base.above()).is(chamberBlock);
    }

    /**
     * Count the number of valid chamber columns adjacent to the controller.
     * More chambers = faster decay rate (future expansion).
     */
    public static int countChambers(Level level, BlockPos controllerPos, Block chamberBlock) {
        int count = 0;
        BlockPos[] horizontalOffsets = {
                controllerPos.north(),
                controllerPos.south(),
                controllerPos.east(),
                controllerPos.west()
        };

        for (BlockPos base : horizontalOffsets) {
            if (isChamberColumn(level, base, chamberBlock)) {
                count++;
            }
        }
        return count;
    }
}
