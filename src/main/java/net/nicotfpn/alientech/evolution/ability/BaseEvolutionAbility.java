package net.nicotfpn.alientech.evolution.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.nicotfpn.alientech.evolution.PlayerEvolutionData;
import net.nicotfpn.alientech.evolution.PlayerEvolutionHelper;
import net.nicotfpn.alientech.util.CapabilityUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base implementation of {@link IEvolutionAbility} with common functionality.
 * <p>
 * Handles cooldown tracking, entropy consumption, and prerequisite checks.
 * Subclasses should override {@link #applyEffect(ServerPlayer)} to implement ability-specific logic.
 * <p>
 * Thread-safe cooldown tracking with automatic cleanup to prevent memory leaks.
 */
public abstract class BaseEvolutionAbility implements IEvolutionAbility {

    // Thread-safe map for multiplayer safety
    // UUID -> last activation game time
    private static final Map<UUID, Long> COOLDOWN_MAP = new ConcurrentHashMap<>();
    
    // Cleanup threshold: remove entries older than 1 hour of game time (72000 ticks)
    private static final long COOLDOWN_CLEANUP_THRESHOLD = 72000L;

    @Override
    public boolean canActivate(Player player) {
        // Server-side only validation
        if (player == null || player.level() == null || player.level().isClientSide) {
            return false;
        }

        PlayerEvolutionData data = CapabilityUtils.safeGetEvolutionData(player);
        if (data == null) {
            return false;
        }

        // Check evolution stage
        if (data.getEvolutionStage() < getRequiredStage()) {
            return false;
        }

        // Check entropy availability
        int cost = getEntropyCost();
        if (cost <= 0) {
            return false; // Invalid cost
        }
        if (data.getStoredEntropy() < cost) {
            return false;
        }

        // Check cooldown
        if (getCooldownTicks() > 0) {
            long currentTime = player.level().getGameTime();
            Long lastUsed = COOLDOWN_MAP.get(player.getUUID());
            if (lastUsed != null) {
                long elapsed = currentTime - lastUsed;
                if (elapsed < 0) {
                    // Time went backwards (shouldn't happen, but handle gracefully)
                    COOLDOWN_MAP.remove(player.getUUID());
                } else if (elapsed < getCooldownTicks()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        // Server-side only - validate player is actually a ServerPlayer
        if (player == null || !(player instanceof ServerPlayer)) {
            return false;
        }

        if (player.level() == null || player.level().isClientSide) {
            return false;
        }

        // Re-validate prerequisites (defense against packet spam)
        if (!canActivate(player)) {
            return false;
        }

        PlayerEvolutionData data = CapabilityUtils.safeGetEvolutionData(player);
        if (data == null) {
            return false;
        }

        int cost = getEntropyCost();
        if (cost <= 0) {
            return false; // Invalid cost
        }

        // Consume entropy atomically
        int extracted = data.extractEntropy(cost);
        if (extracted < cost) {
            // Failed to extract full cost - abort
            return false;
        }

        // Set cooldown atomically
        if (getCooldownTicks() > 0) {
            long currentTime = player.level().getGameTime();
            COOLDOWN_MAP.put(player.getUUID(), currentTime);
            
            // Periodic cleanup to prevent memory leaks
            cleanupOldCooldowns(currentTime);
        }

        AlienTechDebug.ABILITY.log("Ability '{}' activated by player {} (cost: {})", 
                getId(), player.getName().getString(), cost);

        // Apply ability effect (after entropy consumed and cooldown set)
        try {
            applyEffect(player);
        } catch (Exception e) {
            // Effect application failed - entropy already consumed, but that's acceptable
            // Log error but don't fail the activation
            AlienTech.LOGGER.error("Error applying ability effect for " + getId(), e);
        }

        return true;
    }

    /**
     * Clean up old cooldown entries to prevent memory leaks.
     * Called periodically during activation.
     */
    private static void cleanupOldCooldowns(long currentTime) {
        // Only cleanup occasionally (every 1000 activations approximately)
        if (COOLDOWN_MAP.size() < 100) {
            return; // Small map, no cleanup needed
        }

        // Remove entries older than threshold
        COOLDOWN_MAP.entrySet().removeIf(entry -> {
            long elapsed = currentTime - entry.getValue();
            return elapsed > COOLDOWN_CLEANUP_THRESHOLD;
        });
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
        if (player == null || player.level() == null) {
            return 0;
        }

        if (getCooldownTicks() == 0) {
            return 0;
        }

        long currentTime = player.level().getGameTime();
        Long lastUsed = COOLDOWN_MAP.get(player.getUUID());
        if (lastUsed == null) {
            return 0;
        }

        long elapsed = currentTime - lastUsed;
        if (elapsed < 0) {
            // Time went backwards - clear invalid entry
            COOLDOWN_MAP.remove(player.getUUID());
            return 0;
        }

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
