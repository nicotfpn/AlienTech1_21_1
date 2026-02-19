package net.nicotfpn.alientech.evolution;

import net.minecraft.world.entity.player.Player;

/**
 * Utility class for accessing player evolution data.
 * <p>
 * Usage:
 * 
 * <pre>
 * PlayerEvolutionData data = PlayerEvolutionHelper.get(player);
 * int stage = data.getEvolutionStage();
 * data.insertEntropy(100);
 * </pre>
 */
public final class PlayerEvolutionHelper {

    private PlayerEvolutionHelper() {
        // Static utility class
    }

    /**
     * Get the player's evolution data via attachment.
     * If no data exists yet, a default instance is created automatically.
     *
     * @param player the player to query
     * @return the player's evolution data (never null)
     */
    public static PlayerEvolutionData get(Player player) {
        return player.getData(ModAttachments.PLAYER_EVOLUTION);
    }
}
