package net.nicotfpn.alientech.evolution.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.nicotfpn.alientech.evolution.ability.BaseEvolutionAbility;
import net.nicotfpn.alientech.Config;

/**
 * Gravitational Pull Ability - Stage 2
 * <p>
 * Uses decaying gravitons to pull nearby entities toward the player.
 * Higher evolution stages increase pull strength and range.
 */
public class GravitationalPullAbility extends BaseEvolutionAbility {

    public static final String ID = "gravitational_pull";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Gravitational Pull";
    }

    @Override
    public int getRequiredStage() {
        return 2;
    }

    @Override
    public int getEntropyCost() {
        return Config.ABILITY_GRAVITATIONAL_PULL_COST.get();
    }

    @Override
    public int getCooldownTicks() {
        return Config.ABILITY_GRAVITATIONAL_PULL_COOLDOWN.get();
    }

    @Override
    public String getDescription() {
        return "Manipulate decaying gravitons to pull nearby entities toward you. Strength increases with evolution stage.";
    }

    @Override
    protected void applyEffect(ServerPlayer player) {
        if (player == null || player.level() == null || !net.nicotfpn.alientech.util.CapabilityUtils.isValidServerLevel(player.level())) {
            return;
        }

        double range = Config.ABILITY_GRAVITATIONAL_PULL_RANGE.get();
        if (range <= 0 || range > 64.0) {
            return; // Invalid config
        }

        double strength = Config.ABILITY_GRAVITATIONAL_PULL_STRENGTH.get();
        if (strength <= 0 || strength > 5.0) {
            return; // Invalid config
        }
        
        // Scale strength with evolution stage (capped)
        net.nicotfpn.alientech.evolution.PlayerEvolutionData data = 
            net.nicotfpn.alientech.evolution.PlayerEvolutionHelper.get(player);
        if (data != null) {
            double stageMultiplier = 1.0 + Math.min(data.getEvolutionStage(), 10) * 0.2; // Cap at 10 stages
            strength *= stageMultiplier;
        }

        // Clamp strength to reasonable maximum
        strength = Math.min(strength, 10.0);

        Vec3 playerPos = player.position();
        
        try {
            player.level().getEntitiesOfClass(
                    Entity.class,
                    player.getBoundingBox().inflate(range),
                    entity -> entity != player && entity.isAlive() && !entity.isSpectator()
            ).forEach(entity -> {
                try {
                    Vec3 entityPos = entity.position();
                    Vec3 direction = playerPos.subtract(entityPos);
                    double distance = direction.length();
                    
                    if (distance < 0.1) {
                        return; // Too close, skip to avoid division by zero
                    }
                    
                    direction = direction.normalize();
                    
                    // Apply pull force
                    Vec3 velocity = entity.getDeltaMovement();
                    Vec3 pull = direction.scale(strength);
                    Vec3 newVelocity = velocity.add(pull);
                    
                    // Clamp velocity to prevent extreme speeds
                    double maxSpeed = 2.0; // Reasonable maximum
                    if (newVelocity.length() > maxSpeed) {
                        newVelocity = newVelocity.normalize().scale(maxSpeed);
                    }
                    
                    entity.setDeltaMovement(newVelocity);
                    
                    // Reset fall distance to prevent fall damage from pull
                    if (entity instanceof LivingEntity living) {
                        living.fallDistance = 0;
                    }
                    
                    // Mark entity as moved
                    entity.hurtMarked = true;
                } catch (Exception e) {
                    // Ignore individual entity pull failures
                }
            });
        } catch (Exception e) {
            net.nicotfpn.alientech.AlienTech.LOGGER.error("Failed to apply gravitational pull", e);
        }

        player.sendSystemMessage(Component.literal("Gravitational Pull activated!"));
    }
}
