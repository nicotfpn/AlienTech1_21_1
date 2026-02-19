package net.nicotfpn.alientech.evolution.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.nicotfpn.alientech.evolution.ability.BaseEvolutionAbility;
import net.nicotfpn.alientech.Config;

/**
 * Decay Vision Ability - Stage 1
 * <p>
 * Allows the player to see through the veil of decay, revealing hidden entities and structures.
 * Grants night vision and glowing effect to nearby entities.
 */
public class DecayVisionAbility extends BaseEvolutionAbility {

    public static final String ID = "decay_vision";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Decay Vision";
    }

    @Override
    public int getRequiredStage() {
        return 1;
    }

    @Override
    public int getEntropyCost() {
        return Config.ABILITY_DECAY_VISION_COST.get();
    }

    @Override
    public int getCooldownTicks() {
        return Config.ABILITY_DECAY_VISION_COOLDOWN.get();
    }

    @Override
    public String getDescription() {
        return "Peer through the chaos of decay to see hidden entities and structures. Grants night vision and reveals nearby entities.";
    }

    @Override
    protected void applyEffect(ServerPlayer player) {
        // Grant Night Vision for 30 seconds (600 ticks)
        int duration = Config.ABILITY_DECAY_VISION_DURATION.get();
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, true));

        // Apply glowing to nearby entities (reveals them)
        double range = Config.ABILITY_DECAY_VISION_RANGE.get();
        player.level().getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity != player && entity.isAlive()
        ).forEach(entity -> {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration / 2, 0, false, false, true));
            }
        });

        player.sendSystemMessage(Component.literal("Decay Vision activated!"));
    }
}
