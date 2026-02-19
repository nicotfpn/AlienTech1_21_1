package net.nicotfpn.alientech.evolution.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.nicotfpn.alientech.evolution.PlayerEvolutionData;
import net.nicotfpn.alientech.evolution.PlayerEvolutionHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base implementation of {@link IEvolutionAbility} with common functionality.
 * <p>
 * Handles cooldown tracking, entropy consumption, and prerequisite checks.
 * Subclasses should override {@link #applyEffect(ServerPlayer)} to implement ability-specific logic.
 */
public abstract class BaseEvolutionAbility implements IEvolutionAbility {

    private static final Map<UUID, Long> COOLDOWN_MAP = new HashMap<>();

    @Override
    public boolean canActivate(Player player) {
        PlayerEvolutionData data = PlayerEvolutionHelper.get(player);

        // Check evolution stage
        if (data.getEvolutionStage() < getRequiredStage()) {
            return false;
        }

        // Check entropy availability
        if (data.getStoredEntropy() < getEntropyCost()) {
            return false;
        }

        // Check cooldown
        if (getCooldownTicks() > 0) {
            long currentTime = player.level().getGameTime();
            Long lastUsed = COOLDOWN_MAP.get(player.getUUID());
            if (lastUsed != null && currentTime - lastUsed < getCooldownTicks()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!canActivate(player)) {
            return false;
        }

        PlayerEvolutionData data = PlayerEvolutionHelper.get(player);

        // Consume entropy
        int extracted = data.extractEntropy(getEntropyCost());
        if (extracted < getEntropyCost()) {
            return false; // Shouldn't happen if canActivate passed, but safety check
        }

        // Set cooldown
        if (getCooldownTicks() > 0) {
            COOLDOWN_MAP.put(player.getUUID(), player.level().getGameTime());
        }

        // Apply ability effect
        applyEffect(player);

        return true;
    }

    /**
     * Apply the ability's specific effect.
     * <p>
     * This is called after entropy is consumed and cooldown is set.
     * Override this method to implement ability-specific logic.
     * 
     * @param player the player activating the ability
     */
    protected abstract void applyEffect(ServerPlayer player);

    /**
     * Get remaining cooldown ticks for a player.
     * 
     * @param player the player to check
     * @return remaining cooldown ticks, or 0 if not on cooldown
     */
    public int getRemainingCooldown(Player player) {
        if (getCooldownTicks() == 0) {
            return 0;
        }

        long currentTime = player.level().getGameTime();
        Long lastUsed = COOLDOWN_MAP.get(player.getUUID());
        if (lastUsed == null) {
            return 0;
        }

        long elapsed = currentTime - lastUsed;
        return Math.max(0, (int) (getCooldownTicks() - elapsed));
    }

    /**
     * Clear cooldown for a player (useful for admin commands or testing).
     * 
     * @param player the player to clear cooldown for
     */
    public void clearCooldown(Player player) {
        COOLDOWN_MAP.remove(player.getUUID());
    }
}
