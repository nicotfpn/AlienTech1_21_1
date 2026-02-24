package net.nicotfpn.alientech.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.nicotfpn.alientech.AlienTech;

/**
 * Centralized mod tag definitions.
 */
public class ModTags {

    public static class EntityTypes {
        /**
         * Entities tagged as DECAY_IMMUNE will not lose HP in the Decay Chamber.
         * Instead, they generate passive entropy at a boosted rate.
         */
        public static final TagKey<EntityType<?>> DECAY_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "decay_immune"));
    }

    private ModTags() {
    }
}
