package net.nicotfpn.alientech.evolution.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for player evolution abilities.
 * <p>
 * Abilities are unlocked based on evolution stage and consume entropy when used.
 * Each ability defines its own requirements, costs, and effects.
 */
public interface IEvolutionAbility {

    /**
     * @return Unique identifier for this ability (e.g., "entropy_shield")
     */
    String getId();

    /**
     * @return Display name for this ability
     */
    String getDisplayName();

    /**
     * @return Minimum evolution stage required to unlock this ability (0 = baseline)
     */
    int getRequiredStage();

    /**
     * @return Entropy cost to activate this ability
     */
    int getEntropyCost();

    /**
     * @return Cooldown in ticks before this ability can be used again (0 = no cooldown)
     */
    int getCooldownTicks();

    /**
     * Check if the player can use this ability right now.
     * 
     * @param player the player attempting to use the ability
     * @return true if the ability can be activated
     */
    boolean canActivate(Player player);

    /**
     * Activate this ability on the server side.
     * <p>
     * This method should:
     * - Check prerequisites (stage, entropy, cooldown)
     * - Consume entropy
     * - Apply the ability's effect
     * - Set cooldown if applicable
     * 
     * @param player the player activating the ability
     * @return true if the ability was successfully activated
     */
    boolean activate(ServerPlayer player);

    /**
     * Get a description of what this ability does.
     * 
     * @return description text
     */
    String getDescription();
}
