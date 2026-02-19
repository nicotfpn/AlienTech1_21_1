package net.nicotfpn.alientech.evolution.ability;

import net.nicotfpn.alientech.evolution.ability.impl.DecayVisionAbility;
import net.nicotfpn.alientech.evolution.ability.impl.EntropyBurstAbility;
import net.nicotfpn.alientech.evolution.ability.impl.EntropyShieldAbility;
import net.nicotfpn.alientech.evolution.ability.impl.GravitationalPullAbility;

/**
 * Registers all evolution abilities.
 * <p>
 * Call {@link #register()} during mod initialization to register all abilities.
 */
public final class ModAbilities {

    private ModAbilities() {
        // Static registration class
    }

    /**
     * Register all evolution abilities.
     * <p>
     * Should be called during mod initialization (common setup phase).
     */
    public static void register() {
        // Stage 1 abilities
        AbilityRegistry.register(new EntropyShieldAbility());
        AbilityRegistry.register(new DecayVisionAbility());

        // Stage 2 abilities
        AbilityRegistry.register(new GravitationalPullAbility());

        // Stage 3 abilities
        AbilityRegistry.register(new EntropyBurstAbility());
    }
}
