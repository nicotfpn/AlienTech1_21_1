package net.nicotfpn.alientech.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;

/**
 * Registry for all custom recipe types and serializers in AlienTech.
 */
public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
            .create(BuiltInRegistries.RECIPE_SERIALIZER, AlienTech.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE,
            AlienTech.MOD_ID);

    // ==================== Primal Catalyst ====================

    public static final DeferredHolder<RecipeType<?>, RecipeType<PrimalCatalystRecipe>> PRIMAL_CATALYST_TYPE = TYPES
            .register("primal_catalyst", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "primal_catalyst")));

    public static final DeferredHolder<RecipeSerializer<?>, PrimalCatalystRecipeSerializer> PRIMAL_CATALYST_SERIALIZER = SERIALIZERS
            .register("primal_catalyst", PrimalCatalystRecipeSerializer::new);

    // ==================== Registration ====================

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
        SERIALIZERS.register(eventBus);
    }
}
