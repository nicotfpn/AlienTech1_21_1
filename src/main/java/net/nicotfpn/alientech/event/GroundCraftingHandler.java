package net.nicotfpn.alientech.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.item.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles special ground crafting recipes.
 * Pharaoh Sword: Inertial Stability Alloy + Netherite Sword + Nether Star +
 * Ancient Ankh = Pharaoh Sword
 * 
 * PERFORMANCE: Only checks items near players, not entire world.
 */
@EventBusSubscriber(modid = AlienTech.MOD_ID)
public class GroundCraftingHandler {

    private static final int CHECK_INTERVAL = 20; // Check every second
    private static final double CRAFT_RADIUS = 1.5;
    private static final double PLAYER_SEARCH_RADIUS = 16.0; // Only check items within 16 blocks of players
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide())
            return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL)
            return;
        tickCounter = 0;

        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel))
            return;

        // PERFORMANCE FIX: Only search for items near players, not entire world!
        List<ItemEntity> nearbyItems = new ArrayList<>();
        for (var player : serverLevel.players()) {
            AABB searchBox = player.getBoundingBox().inflate(PLAYER_SEARCH_RADIUS);
            nearbyItems.addAll(serverLevel.getEntitiesOfClass(ItemEntity.class, searchBox));
        }

        // Deduplicate (player search areas may overlap)
        List<ItemEntity> uniqueItems = new ArrayList<>(new java.util.LinkedHashSet<>(nearbyItems));

        // Check for Pharaoh Sword recipe combinations
        checkPharaohSwordRecipe(serverLevel, uniqueItems);
    }

    private static void checkPharaohSwordRecipe(ServerLevel level, List<ItemEntity> allItems) {
        // Find Inertial Stability Alloys as anchor points
        for (ItemEntity alloyEntity : allItems) {
            if (!alloyEntity.isAlive())
                continue;
            if (!alloyEntity.getItem().is(ModItems.INERTIAL_STABILITY_ALLOY.get()))
                continue;

            Vec3 center = alloyEntity.position();
            AABB searchArea = new AABB(
                    center.x - CRAFT_RADIUS, center.y - CRAFT_RADIUS, center.z - CRAFT_RADIUS,
                    center.x + CRAFT_RADIUS, center.y + CRAFT_RADIUS, center.z + CRAFT_RADIUS);

            // Find required items nearby
            ItemEntity netheriteEntity = null;
            ItemEntity netherStarEntity = null;
            ItemEntity ankhEntity = null;

            for (ItemEntity candidate : allItems) {
                if (!candidate.isAlive())
                    continue;
                if (candidate == alloyEntity)
                    continue;
                if (!searchArea.contains(candidate.position()))
                    continue;

                ItemStack stack = candidate.getItem();
                if (stack.is(Items.NETHERITE_SWORD) && netheriteEntity == null) {
                    netheriteEntity = candidate;
                } else if (stack.is(Items.NETHER_STAR) && netherStarEntity == null) {
                    netherStarEntity = candidate;
                } else if (stack.is(ModItems.ANCIENT_ANKH.get()) && ankhEntity == null) {
                    ankhEntity = candidate;
                }
            }

            // Check if all 4 ingredients are present
            if (netheriteEntity != null && netherStarEntity != null && ankhEntity != null) {
                // Perform the craft!
                performPharaohSwordCraft(level, alloyEntity, netheriteEntity, netherStarEntity, ankhEntity);
                return; // Only craft one at a time
            }
        }
    }

    private static void performPharaohSwordCraft(ServerLevel level, ItemEntity alloyEntity,
            ItemEntity netheriteEntity, ItemEntity netherStarEntity, ItemEntity ankhEntity) {
        Vec3 craftPos = alloyEntity.position();
        BlockPos blockPos = BlockPos.containing(craftPos);

        // Consume one of each item
        consumeOne(alloyEntity);
        consumeOne(netheriteEntity);
        consumeOne(netherStarEntity);
        consumeOne(ankhEntity);

        // Spawn the Pharaoh Sword
        ItemStack result = new ItemStack(ModItems.PHARAOH_SWORD.get());
        ItemEntity resultEntity = new ItemEntity(level, craftPos.x, craftPos.y + 0.5, craftPos.z, result);
        resultEntity.setPickUpDelay(20); // 1 second delay before pickup
        resultEntity.setDeltaMovement(0, 0.2, 0); // Small upward motion
        level.addFreshEntity(resultEntity);

        // Visual and audio effects
        level.playSound(null, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5,
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.5f);

        level.playSound(null, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5,
                SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5f, 2.0f);

        // Particles
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                craftPos.x, craftPos.y + 0.5, craftPos.z,
                50, 0.5, 0.5, 0.5, 0.2);

        level.sendParticles(ParticleTypes.END_ROD,
                craftPos.x, craftPos.y + 0.5, craftPos.z,
                30, 0.3, 0.3, 0.3, 0.1);

        level.sendParticles(ParticleTypes.FLAME,
                craftPos.x, craftPos.y + 0.5, craftPos.z,
                20, 0.4, 0.4, 0.4, 0.05);
    }

    private static void consumeOne(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (stack.getCount() > 1) {
            stack.shrink(1);
            itemEntity.setItem(stack);
        } else {
            itemEntity.discard();
        }
    }
}
