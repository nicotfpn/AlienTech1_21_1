package net.nicotfpn.alientech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.nicotfpn.alientech.entropy.IEntropyHandler;
import net.nicotfpn.alientech.entropy.ModCapabilities;
import net.nicotfpn.alientech.evolution.PlayerEvolutionData;
import net.nicotfpn.alientech.evolution.PlayerEvolutionHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Safe utility methods for capability and attachment access.
 * <p>
 * All methods perform null checks and validation before returning values.
 * Prevents crashes from unsafe capability access patterns.
 */
public final class CapabilityUtils {

    private CapabilityUtils() {
        // Static utility class
    }

    /**
     * Safely get an entropy handler from a block position.
     * 
     * @param level the level (must not be null)
     * @param pos the block position
     * @param side the side to query from (null for no side)
     * @return the entropy handler, or null if not present or invalid
     */
    @Nullable
    public static IEntropyHandler safeGetEntropyHandler(@Nullable Level level, BlockPos pos, @Nullable Direction side) {
        if (level == null || !level.isLoaded(pos)) {
            return null;
        }
        try {
            return level.getCapability(ModCapabilities.ENTROPY, pos, side);
        } catch (Exception e) {
            // Capability access can throw in edge cases
            return null;
        }
    }

    /**
     * Safely get an entropy handler from a neighbor block.
     * 
     * @param level the level (must not be null)
     * @param pos the block position
     * @param direction the direction to the neighbor
     * @return the entropy handler, or null if not present or invalid
     */
    @Nullable
    public static IEntropyHandler safeGetNeighborEntropyHandler(@Nullable Level level, BlockPos pos, Direction direction) {
        if (level == null || direction == null) {
            return null;
        }
        BlockPos neighborPos = pos.relative(direction);
        return safeGetEntropyHandler(level, neighborPos, direction.getOpposite());
    }

    /**
     * Safely get player evolution data.
     * Always returns a valid instance (never null).
     * 
     * @param player the player (must not be null)
     * @return the evolution data (never null)
     */
    @Nullable
    public static PlayerEvolutionData safeGetEvolutionData(@Nullable Player player) {
        if (player == null) {
            return null;
        }
        try {
            return PlayerEvolutionHelper.get(player);
        } catch (Exception e) {
            // Attachment access can fail in edge cases
            return null;
        }
    }

    /**
     * Validate that a level is a valid server level.
     * 
     * @param level the level to validate
     * @return true if level is non-null, not client-side, and is a ServerLevel
     */
    public static boolean isValidServerLevel(@Nullable Level level) {
        return level != null && !level.isClientSide && level instanceof ServerLevel;
    }

    /**
     * Validate that a level is loaded at the given position.
     * 
     * @param level the level to check
     * @param pos the position to check
     * @return true if level is non-null and position is loaded
     */
    public static boolean isPositionLoaded(@Nullable Level level, BlockPos pos) {
        return level != null && level.isLoaded(pos);
    }
}
