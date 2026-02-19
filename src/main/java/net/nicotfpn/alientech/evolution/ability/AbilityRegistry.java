package net.nicotfpn.alientech.evolution.ability;

import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.AlienTech;

import java.util.*;

/**
 * Registry for all evolution abilities.
 * <p>
 * Abilities are registered by their ID and can be looked up by ID or retrieved as collections.
 */
public final class AbilityRegistry {

    private AbilityRegistry() {
        // Static registry class
    }

    private static final Map<String, IEvolutionAbility> ABILITIES = new HashMap<>();
    private static final Map<ResourceLocation, IEvolutionAbility> ABILITIES_BY_RL = new HashMap<>();

    /**
     * Register an ability.
     * 
     * @param ability the ability to register
     * @throws IllegalArgumentException if an ability with the same ID is already registered
     */
    public static void register(IEvolutionAbility ability) {
        String id = ability.getId();
        if (ABILITIES.containsKey(id)) {
            throw new IllegalArgumentException("Ability with ID '" + id + "' is already registered!");
        }

        ABILITIES.put(id, ability);
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, id);
        ABILITIES_BY_RL.put(rl, ability);
    }

    /**
     * Get an ability by its ID.
     * 
     * @param id the ability ID
     * @return the ability, or null if not found
     */
    public static IEvolutionAbility get(String id) {
        return ABILITIES.get(id);
    }

    /**
     * Get an ability by its ResourceLocation.
     * 
     * @param rl the ability ResourceLocation
     * @return the ability, or null if not found
     */
    public static IEvolutionAbility get(ResourceLocation rl) {
        return ABILITIES_BY_RL.get(rl);
    }

    /**
     * Get all registered abilities.
     * 
     * @return unmodifiable collection of all abilities
     */
    public static Collection<IEvolutionAbility> getAll() {
        return Collections.unmodifiableCollection(ABILITIES.values());
    }

    /**
     * Get all abilities available to a player based on their evolution stage.
     * 
     * @param evolutionStage the player's evolution stage
     * @return list of abilities the player can use
     */
    public static List<IEvolutionAbility> getAvailableAbilities(int evolutionStage) {
        List<IEvolutionAbility> available = new ArrayList<>();
        for (IEvolutionAbility ability : ABILITIES.values()) {
            if (evolutionStage >= ability.getRequiredStage()) {
                available.add(ability);
            }
        }
        return available;
    }

    /**
     * Clear all registered abilities (useful for testing or reloading).
     */
    public static void clear() {
        ABILITIES.clear();
        ABILITIES_BY_RL.clear();
    }
}
