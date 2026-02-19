package net.nicotfpn.alientech.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * Pharaoh Sword - A legendary energy-powered sword with ancient Egyptian
 * abilities.
 * 
 * Features:
 * - Energy-based system (1,000,000 FE capacity)
 * - Solar beam ability (right-click)
 * - Desert curse: applies fire and wither to enemies
 * - Bonus damage against undead mobs
 * - Golden particle effects
 */
public class PharaohSwordItem extends SwordItem {
    public static final int ENERGY_PER_HIT = 500;
    public static final int MAX_TRANSFER = 10_000;

    private static final int FIRE_DURATION_SECONDS = 5;
    private static final int WITHER_DURATION_SECONDS = 3;
    private static final int WITHER_AMPLIFIER = 1;

    // Custom tier for Pharaoh Sword
    private static final Tier PHARAOH_TIER = Tiers.NETHERITE;

    public PharaohSwordItem(Properties properties) {
        super(PHARAOH_TIER, properties.attributes(
                SwordItem.createAttributes(PHARAOH_TIER, 9, -2.4f) // +10 attack damage total
        ));
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel serverLevel) {
            ItemEnergyStorage energyStorage = new ItemEnergyStorage(stack,
                    net.nicotfpn.alientech.Config.PHARAOH_SWORD_CAPACITY.get(), MAX_TRANSFER, MAX_TRANSFER);

            // Check if enough energy
            if (energyStorage.getEnergyStored() >= ENERGY_PER_HIT) {
                // Consume energy
                energyStorage.extractEnergy(ENERGY_PER_HIT, false);

                // Apply Desert Curse effects
                applyDesertCurse(target, serverLevel);

                // Bonus damage against undead
                if (target.isInvertedHealAndHarm()) {
                    target.hurt(serverLevel.damageSources().magic(), 5.0F);
                }

                // Spawn golden particles at hit location
                spawnGoldenParticles(serverLevel, target.position(), 10);

                return true;
            } else {
                // No energy - sword doesn't work
                return false;
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 72000; // Continuous use
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW; // Animation when holding
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack stack,
            int count) {
        if (livingEntity instanceof Player player && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Fire ray continuously (every 5 ticks = 0.25s)
            if (count % 5 == 0) {
                ItemEnergyStorage energyStorage = new ItemEnergyStorage(stack,
                        net.nicotfpn.alientech.Config.PHARAOH_SWORD_CAPACITY.get(), MAX_TRANSFER, MAX_TRANSFER);

                int energyCost = net.nicotfpn.alientech.Config.PHARAOH_SWORD_COST.get();
                // Check energy
                if (energyStorage.getEnergyStored() >= energyCost) {
                    // Consume energy
                    energyStorage.extractEnergy(energyCost, false);

                    // Fire Voldemort Ray
                    fireVoldemortRay(serverLevel, player);
                } else {
                    player.stopUsingItem();
                    player.getCooldowns().addCooldown(this, 100); // Longer cooldown (5s) if you run out of energy
                }
            }
        }
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId,
            boolean isSelected) {
        if (isSelected && entity instanceof Player player && !level.isClientSide) {
            // Spawn subtle golden particles when held
            if (level.random.nextFloat() < 0.1f && level instanceof ServerLevel serverLevel) {
                Vec3 pos = player.position().add(0, 1.0, 0);
                spawnGoldenParticles(serverLevel, pos, 1);
            }
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true; // Always show energy bar
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        int maxEnergy = net.nicotfpn.alientech.Config.PHARAOH_SWORD_CAPACITY.get();
        ItemEnergyStorage energyStorage = new ItemEnergyStorage(stack, maxEnergy, MAX_TRANSFER, MAX_TRANSFER);
        return Math.round(13.0F * energyStorage.getEnergyStored() / (float) maxEnergy);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        int maxEnergy = net.nicotfpn.alientech.Config.PHARAOH_SWORD_CAPACITY.get();
        ItemEnergyStorage energyStorage = new ItemEnergyStorage(stack, maxEnergy, MAX_TRANSFER, MAX_TRANSFER);
        float ratio = energyStorage.getEnergyStored() / (float) maxEnergy;

        // Golden color gradient based on energy
        if (ratio > 0.5f) {
            return 0xFFD700; // Gold
        } else if (ratio > 0.25f) {
            return 0xFFA500; // Orange
        } else {
            return 0xFF4500; // Red-Orange
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
            @NotNull java.util.List<net.minecraft.network.chat.Component> tooltipComponents,
            @NotNull net.minecraft.world.item.TooltipFlag tooltipFlag) {

        int maxEnergy = net.nicotfpn.alientech.Config.PHARAOH_SWORD_CAPACITY.get();
        ItemEnergyStorage energyStorage = new ItemEnergyStorage(stack, maxEnergy, MAX_TRANSFER, MAX_TRANSFER);

        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.US);
        String energyText = formatter.format(energyStorage.getEnergyStored()) + " / " + formatter.format(maxEnergy);

        tooltipComponents.add(net.minecraft.network.chat.Component.literal("§7Stored Energy: §b" + energyText + " FE"));
        tooltipComponents.add(net.minecraft.network.chat.Component.empty()); // Spacer
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.alientech.pharaoh_sword.desc")
                .withStyle(net.minecraft.ChatFormatting.GOLD));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * Apply Desert Curse effects: fire and wither
     *
     */
    private void applyDesertCurse(LivingEntity target, ServerLevel level) {
        // Set on fire for 5 seconds
        target.igniteForSeconds(FIRE_DURATION_SECONDS);

        // Apply Wither effect
        target.addEffect(new MobEffectInstance(
                MobEffects.WITHER,
                WITHER_DURATION_SECONDS * 20,
                WITHER_AMPLIFIER));

        // Spawn dark particles to indicate curse
        spawnDarkParticles(level, target.position(), 15);
    }

    /**
     * Fire a solar beam that damages entities in a line
     */
    /**
     * Spawns a lightning bolt at the player's looking position
     */
    /**
     * Fires a continuous beam of energy ("Voldemort Style")
     * - Dense particle stream
     * - Penetrates entities
     * - Continuous damage tick
     */
    /**
     * Fires a continuous ray of energy (Voldemort Ray).
     * Performs raycasting to detect entities, spawns particles along the path,
     * and applies damage and effects to hit entities.
     *
     * @param level  The server level.
     * @param player The player using the item.
     */
    // ... (existing code)

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (entity instanceof Player player) {
            // Add cooldown of 3 seconds (60 ticks)
            player.getCooldowns().addCooldown(this, 60);
        }
        super.releaseUsing(stack, level, entity, timeCharged);
    }

    private void fireVoldemortRay(ServerLevel level, Player player) {
        double range = 30.0;
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(range));

        // 1. Raycast for block collision
        HitResult blockHit = level.clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        // 2. Spawn dense particle beam (RED)
        Vec3 dir = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        // Red Dust Options: Color (1, 0, 0), Size 1.5
        net.minecraft.core.particles.DustParticleOptions redDust = new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(1.0f, 0.0f, 0.0f), 1.5f);

        for (double i = 0; i < distance; i += 0.5) {
            Vec3 p = start.add(dir.scale(i));
            // Red central beam
            level.sendParticles(redDust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            // Dynamic sparks (Flame/Lava for "Red/Desert" feel)
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
        }

        // 3. Damage entities along the path
        Vec3 finalEnd = end;
        level.getEntities(player, player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0))
                .stream()
                .filter(e -> e instanceof LivingEntity && e != player)
                .map(e -> (LivingEntity) e)
                .filter(e -> e.getBoundingBox().inflate(0.5).clip(start, finalEnd).isPresent())
                .forEach(target -> {
                    target.hurt(level.damageSources().magic(), 6.0F);
                    applyDesertCurse(target, level);
                    target.setDeltaMovement(target.getDeltaMovement().add(look.scale(0.2)));
                });

        // 4. Sound Effect
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.BEACON_AMBIENT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    /**
     * Spawn golden particles at a position
     */
    private void spawnGoldenParticles(ServerLevel level, Vec3 pos, int count) {
        level.sendParticles(
                ParticleTypes.WAX_ON,
                pos.x, pos.y + 1, pos.z,
                count, 0.3, 0.5, 0.3, 0.1);
    }

    /**
     * Spawn dark/wither particles for curse effect
     */
    private void spawnDarkParticles(ServerLevel level, Vec3 pos, int count) {
        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.x, pos.y + 1, pos.z,
                count, 0.3, 0.5, 0.3, 0.05);
    }

    // Capability registration is done in ModCapabilities or AlienTech.java
}
