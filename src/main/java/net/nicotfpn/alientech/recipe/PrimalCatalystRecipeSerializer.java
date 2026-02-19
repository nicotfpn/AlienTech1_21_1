package net.nicotfpn.alientech.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Serializer for PrimalCatalystRecipe using MapCodec (NeoForge 1.21.1 pattern).
 *
 * JSON format:
 * {
 * "type": "alientech:primal_catalyst",
 * "ingredients": [
 * { "item": "alientech:concentrated_substrate" },
 * { "item": "alientech:neutrion_ingot" },
 * { "item": "alientech:graviton" }
 * ],
 * "result": { "id": "alientech:inertial_stability_alloy", "count": 1 },
 * "energy_modifier": 1.0
 * }
 */
public class PrimalCatalystRecipeSerializer implements RecipeSerializer<PrimalCatalystRecipe> {

    private static final MapCodec<PrimalCatalystRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                    .forGetter(r -> r.getIngredients().stream().toList()),
            ItemStack.CODEC.fieldOf("result").forGetter(PrimalCatalystRecipe::getResult),
            Codec.FLOAT.optionalFieldOf("energy_modifier", 1.0f).forGetter(PrimalCatalystRecipe::getEnergyModifier))
            .apply(inst, (ingredients, result, energyMod) -> {
                NonNullList<Ingredient> list = NonNullList.create();
                list.addAll(ingredients);
                return new PrimalCatalystRecipe(list, result, energyMod);
            }));

    private static final StreamCodec<RegistryFriendlyByteBuf, PrimalCatalystRecipe> STREAM_CODEC = StreamCodec
            .of(PrimalCatalystRecipeSerializer::toNetwork, PrimalCatalystRecipeSerializer::fromNetwork);

    @Override
    public @NotNull MapCodec<PrimalCatalystRecipe> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, PrimalCatalystRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static PrimalCatalystRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int i = 0; i < size; i++) {
            ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
        }
        ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
        float energyMod = buf.readFloat();
        return new PrimalCatalystRecipe(ingredients, result, energyMod);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, PrimalCatalystRecipe recipe) {
        buf.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
        }
        ItemStack.STREAM_CODEC.encode(buf, recipe.getResult());
        buf.writeFloat(recipe.getEnergyModifier());
    }
}
