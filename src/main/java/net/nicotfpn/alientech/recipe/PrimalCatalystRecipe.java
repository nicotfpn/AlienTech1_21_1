package net.nicotfpn.alientech.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Custom recipe for the Primal Catalyst machine.
 * Uses PrimalCatalystRecipeInput as the RecipeInput type (NeoForge 1.21.1
 * requirement).
 * Supports 3 shapeless ingredient inputs, 1 output, and an optional energy
 * modifier.
 *
 * The recipe matches if all 3 ingredients are present in slots 0-2 (order
 * doesn't matter).
 */
public class PrimalCatalystRecipe implements Recipe<PrimalCatalystRecipeInput> {

    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final float energyModifier;

    public PrimalCatalystRecipe(NonNullList<Ingredient> ingredients, ItemStack result, float energyModifier) {
        this.ingredients = ingredients;
        this.result = result;
        this.energyModifier = energyModifier;
    }

    /**
     * Check if the input matches this recipe.
     * Uses shapeless matching: all 3 ingredients must be present in slots 0-2,
     * but order doesn't matter.
     */
    @Override
    public boolean matches(@NotNull PrimalCatalystRecipeInput input, @NotNull Level level) {
        boolean[] used = new boolean[3];

        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < 3; i++) {
                if (!used[i] && ingredient.test(input.getItem(i))) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }

        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull PrimalCatalystRecipeInput input,
            @NotNull HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.PRIMAL_CATALYST_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.PRIMAL_CATALYST_TYPE.get();
    }

    // ==================== Accessors ====================

    public ItemStack getResult() {
        return result.copy();
    }

    public float getEnergyModifier() {
        return energyModifier;
    }
}
