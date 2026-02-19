package net.nicotfpn.alientech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Generic utility for storing and releasing mob data in ItemStack NBT.
 * <p>
 * Stores a full NBT snapshot of the entity — never holds a raw entity
 * reference.
 * Uses NeoForge 1.21.1 DataComponents.CUSTOM_DATA for ItemStack tag storage.
 * <p>
 * NBT structure stored in CustomData:
 * 
 * <pre>
 * StoredEntityType: "minecraft:zombie"   (ResourceLocation string)
 * StoredHealth:     20.0f                (float)
 * StoredNBT:        { ... }             (full entity compound)
 * </pre>
 */
public final class EntityStorageUtil {

    private static final String KEY_ENTITY_TYPE = "StoredEntityType";
    private static final String KEY_HEALTH = "StoredHealth";
    private static final String KEY_NBT = "StoredNBT";

    private EntityStorageUtil() {
        // Static utility class
    }

    // ==================== Core Operations ====================

    /**
     * Store a living entity's data into an ItemStack's custom data component.
     * The entity is NOT removed from the world — the caller must handle that.
     *
     * @param stack  the item to store the mob in
     * @param entity the living entity to capture
     * @return true if stored successfully, false if entity type is invalid
     */
    public static boolean storeMob(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide())
            return false;

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null)
            return false;

        // Take full NBT snapshot
        CompoundTag entityNbt = new CompoundTag();
        entity.save(entityNbt);

        // Build custom data tag
        CompoundTag tag = getOrCreateCustomData(stack);
        tag.putString(KEY_ENTITY_TYPE, typeId.toString());
        tag.putFloat(KEY_HEALTH, entity.getHealth());
        tag.put(KEY_NBT, entityNbt);

        // Write back to item
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    /**
     * Release the stored entity into the world at the given position.
     *
     * @param stack the item containing the stored mob
     * @param level the server level to spawn in
     * @param pos   the block position to spawn at
     * @return the spawned entity, or null if release failed
     */
    @Nullable
    public static Entity releaseMob(ItemStack stack, Level level, BlockPos pos) {
        // Server-side only
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (stack == null || !hasStoredMob(stack)) {
            return null;
        }

        if (pos == null) {
            return null;
        }

        CompoundTag tag = getCustomData(stack);
        if (tag == null || !tag.contains(KEY_ENTITY_TYPE) || !tag.contains(KEY_NBT)) {
            return null;
        }

        String typeIdStr = tag.getString(KEY_ENTITY_TYPE);
        if (typeIdStr == null || typeIdStr.isEmpty()) {
            return null;
        }

        ResourceLocation typeId = ResourceLocation.tryParse(typeIdStr);
        if (typeId == null) {
            return null;
        }

        Optional<EntityType<?>> entityTypeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId);
        if (entityTypeOpt.isEmpty()) {
            return null;
        }

        EntityType<?> entityType = entityTypeOpt.get();
        CompoundTag entityNbt = tag.getCompound(KEY_NBT);
        if (entityNbt == null || entityNbt.isEmpty()) {
            return null;
        }

        // Create entity from stored NBT
        Entity entity;
        try {
            entity = entityType.create(serverLevel);
            if (entity == null) {
                return null;
            }
        } catch (Exception e) {
            net.nicotfpn.alientech.AlienTech.LOGGER.error("Failed to create entity from type {}", typeId, e);
            return null;
        }

        // Load entity NBT safely
        try {
            entity.load(entityNbt);
        } catch (Exception e) {
            net.nicotfpn.alientech.AlienTech.LOGGER.error("Failed to load entity NBT", e);
            entity.discard(); // Clean up failed entity
            return null;
        }

        // Set position safely
        entity.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);

        // Restore health if it's a living entity
        if (entity instanceof LivingEntity living) {
            float storedHealth = tag.getFloat(KEY_HEALTH);
            if (storedHealth > 0 && storedHealth <= living.getMaxHealth()) {
                living.setHealth(storedHealth);
            }
        }

        // Clear AI target to prevent immediate aggro on release
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
        }

        // Spawn entity safely
        try {
            if (level.addFreshEntity(entity)) {
                // Clear the stored data from the item only after successful spawn
                clearStoredMob(stack);
                return entity;
            } else {
                // Spawn failed - clean up
                entity.discard();
                return null;
            }
        } catch (Exception e) {
            net.nicotfpn.alientech.AlienTech.LOGGER.error("Failed to spawn entity", e);
            entity.discard();
            return null;
        }
    }

    // ==================== Query Methods ====================

    /**
     * @return true if the item currently stores a mob
     */
    public static boolean hasStoredMob(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        return tag != null && tag.contains(KEY_ENTITY_TYPE) && tag.contains(KEY_NBT);
    }

    /**
     * Get the stored entity type, or null if none stored.
     */
    @Nullable
    public static EntityType<?> getStoredEntityType(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        if (tag == null || !tag.contains(KEY_ENTITY_TYPE))
            return null;

        String typeIdStr = tag.getString(KEY_ENTITY_TYPE);
        ResourceLocation typeId = ResourceLocation.tryParse(typeIdStr);
        if (typeId == null)
            return null;

        return BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
    }

    /**
     * Get the stored entity's health, or 0 if none stored.
     */
    public static float getStoredHealth(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        if (tag == null)
            return 0f;
        return tag.getFloat(KEY_HEALTH);
    }

    /**
     * Get the stored entity's full NBT snapshot.
     */
    @Nullable
    public static CompoundTag getStoredNBT(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        if (tag == null || !tag.contains(KEY_NBT))
            return null;
        return tag.getCompound(KEY_NBT);
    }

    /**
     * Get a display name for the stored entity type (for tooltips).
     */
    @Nullable
    public static String getStoredEntityName(ItemStack stack) {
        EntityType<?> type = getStoredEntityType(stack);
        if (type == null)
            return null;
        return type.getDescription().getString();
    }

    // ==================== Mutation ====================

    /**
     * Clear all stored mob data from the item.
     */
    public static void clearStoredMob(ItemStack stack) {
        CompoundTag tag = getCustomData(stack);
        if (tag != null) {
            tag.remove(KEY_ENTITY_TYPE);
            tag.remove(KEY_HEALTH);
            tag.remove(KEY_NBT);
            // If tag is now empty, remove the component entirely
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    // ==================== Internal Helpers ====================

    /**
     * Get the custom data tag from the ItemStack (read-only copy), or null if
     * absent.
     */
    @Nullable
    private static CompoundTag getCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null)
            return null;
        return customData.copyTag();
    }

    /**
     * Get or create a mutable custom data tag for writing.
     */
    private static CompoundTag getOrCreateCustomData(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag();
    }
}
