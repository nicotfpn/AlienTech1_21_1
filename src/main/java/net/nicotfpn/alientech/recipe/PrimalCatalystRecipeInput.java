package net.nicotfpn.alientech.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

/**
 * Simple RecipeInput wrapper for the Primal Catalyst's 3 input slots.
 * Implements RecipeInput as required by NeoForge 1.21.1's Recipe system.
 */
public class PrimalCatalystRecipeInput implements RecipeInput {

    private final ItemStack input1;
    private final ItemStack input2;
    private final ItemStack input3;

    public PrimalCatalystRecipeInput(ItemStack input1, ItemStack input2, ItemStack input3) {
        this.input1 = input1;
        this.input2 = input2;
        this.input3 = input3;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> input1;
            case 1 -> input2;
            case 2 -> input3;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
