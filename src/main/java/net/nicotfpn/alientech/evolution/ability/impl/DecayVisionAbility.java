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
        if (player == null || player.level() == null || !net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(player.level())) {
            return;
        }

        // Grant Night Vision for configured duration
        int duration = Config.ABILITY_DECAY_VISION_DURATION.get();
        if (duration <= 0) {
            return; // Invalid config
        }

        // Clamp duration to reasonable maximum (5 minutes)
        duration = Math.min(duration, 6000);

        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, true));

        // Apply glowing to nearby entities (reveals them)
        double range = Config.ABILITY_DECAY_VISION_RANGE.get();
        if (range > 0 && range <= 128.0) { // Validate range
            try {
                player.level().getEntitiesOfClass(
                        net.minecraft.world.entity.Entity.class,
                        player.getBoundingBox().inflate(range),
                        entity -> entity != player && entity.isAlive()
                ).forEach(entity -> {
                    if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                        try {
                            int glowDuration = Math.max(1, duration / 2);
                            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowDuration, 0, false, false, true));
                        } catch (Exception e) {
                            // Ignore individual entity effect failures
                        }
                    }
                });
            } catch (Exception e) {
                net.nicotfpn.alientech.AlienTech.LOGGER.error("Failed to apply decay vision effects", e);
            }
        }

        player.sendSystemMessage(Component.literal("Decay Vision activated!"));
    }
}
