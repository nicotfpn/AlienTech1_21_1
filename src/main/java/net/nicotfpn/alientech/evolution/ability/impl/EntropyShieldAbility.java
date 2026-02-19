package net.nicotfpn.alientech.evolution.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.nicotfpn.alientech.evolution.ability.BaseEvolutionAbility;
import net.nicotfpn.alientech.Config;

/**
 * Entropy Shield Ability - Stage 1
 * <p>
 * Consumes entropy to grant temporary damage resistance.
 * The chaotic energy of decay forms a protective barrier around the player.
 */
public class EntropyShieldAbility extends BaseEvolutionAbility {

    public static final String ID = "entropy_shield";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Entropy Shield";
    }

    @Override
    public int getRequiredStage() {
        return 1;
    }

    @Override
    public int getEntropyCost() {
        return Config.ABILITY_ENTROPY_SHIELD_COST.get();
    }

    @Override
    public int getCooldownTicks() {
        return Config.ABILITY_ENTROPY_SHIELD_COOLDOWN.get();
    }

    @Override
    public String getDescription() {
        return "Creates a protective barrier of chaotic entropy energy, granting damage resistance for a short time.";
    }

    @Override
    protected void applyEffect(ServerPlayer player) {
        // Grant Resistance II for 10 seconds (200 ticks)
        int duration = Config.ABILITY_ENTROPY_SHIELD_DURATION.get();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true, true));

        // Visual feedback
        player.sendSystemMessage(Component.literal("Entropy Shield activated!"));
    }
}
