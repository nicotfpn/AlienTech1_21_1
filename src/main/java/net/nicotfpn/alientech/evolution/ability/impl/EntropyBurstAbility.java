package net.nicotfpn.alientech.evolution.ability.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.nicotfpn.alientech.evolution.ability.BaseEvolutionAbility;
import net.nicotfpn.alientech.Config;

/**
 * Entropy Burst Ability - Stage 3
 * <p>
 * Releases a massive burst of chaotic entropy energy, damaging and knocking back nearby entities.
 * The decay energy explodes outward from the player.
 */
public class EntropyBurstAbility extends BaseEvolutionAbility {

    public static final String ID = "entropy_burst";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Entropy Burst";
    }

    @Override
    public int getRequiredStage() {
        return 3;
    }

    @Override
    public int getEntropyCost() {
        return Config.ABILITY_ENTROPY_BURST_COST.get();
    }

    @Override
    public int getCooldownTicks() {
        return Config.ABILITY_ENTROPY_BURST_COOLDOWN.get();
    }

    @Override
    public String getDescription() {
        return "Release a devastating burst of entropy energy, damaging and knocking back all nearby entities.";
    }

    @Override
    protected void applyEffect(ServerPlayer player) {
        float power = Config.ABILITY_ENTROPY_BURST_POWER.get().floatValue();
        
        // Scale power with evolution stage
        net.nicotfpn.alientech.evolution.PlayerEvolutionData data = 
            net.nicotfpn.alientech.evolution.PlayerEvolutionHelper.get(player);
        power *= (1.0f + data.getEvolutionStage() * 0.15f);

        // Create explosion that doesn't break blocks but damages entities
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.NONE;
        player.level().explode(
                player,
                player.getX(),
                player.getY() + player.getEyeHeight() / 2.0,
                player.getZ(),
                power,
                mode
        );

        // Additional damage to nearby entities
        double range = Config.ABILITY_ENTROPY_BURST_RANGE.get();
        float damage = Config.ABILITY_ENTROPY_BURST_DAMAGE.get().floatValue();
        
        player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
        ).forEach(entity -> {
            entity.hurt(player.damageSources().magic(), damage);
        });

        player.sendSystemMessage(Component.literal("Entropy Burst unleashed!"));
    }
}
