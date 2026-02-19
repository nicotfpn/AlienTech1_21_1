package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.base.AbstractMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.IMachineProcess;
import net.nicotfpn.alientech.machine.core.SlotAccessRules;
import net.nicotfpn.alientech.recipe.ModRecipes;
import net.nicotfpn.alientech.recipe.PrimalCatalystRecipe;
import net.nicotfpn.alientech.recipe.PrimalCatalystRecipeInput;
import net.nicotfpn.alientech.screen.PrimalCatalystMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Primal Catalyst — A 3-input processing machine with hybrid energy/fuel
 * support.
 *
 * Built on the component-based AbstractMachineBlockEntity framework.
 * Implements only: slot layout, fuel/output config, sided rules, and recipe
 * logic.
 *
 * Slot layout:
 * 0, 1, 2 = Input slots
 * 3 = Fuel slot (coal_block only)
 * 4 = Output slot
 *
 * Sided automation:
 * TOP → insert into input slots (0, 1, 2)
 * SIDES → insert into fuel slot (3)
 * BOTTOM → extract from output slot (4)
 */
public class PrimalCatalystBlockEntity extends AbstractMachineBlockEntity implements IMachineProcess, SlotAccessRules {

    // ==================== Slot Constants (PRESERVED) ====================
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int INPUT_SLOT_3 = 2;
    public static final int FUEL_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    private static final int SLOT_COUNT = 5;

    // ==================== Energy Constants ====================
    private static final int DEFAULT_CAPACITY = 100_000;
    private static final int MAX_RECEIVE = 1_000;
    private static final int MAX_EXTRACT = 0;

    // ==================== Fuel Constants ====================
    private static final int COAL_BLOCK_BURN_TIME = 16_000;

    // ==================== Cached Recipe ====================
    private PrimalCatalystRecipe cachedRecipe = null;
    private boolean recipeCacheDirty = true;

    // ==================== Constructor ====================

    public PrimalCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRIMAL_CATALYST_BE.get(), pos, state,
                DEFAULT_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, SLOT_COUNT);
    }

    // ==================== IMachineProcess Implementation ====================

    @Override
    public boolean canProcess() {
        if (level == null)
            return false;

        // Invalidate cache when inventory changes
        if (recipeCacheDirty) {
            cachedRecipe = findRecipe();
            recipeCacheDirty = false;
        }

        if (cachedRecipe == null)
            return false;

        // Check output slot has space
        ItemStack outputStack = inventory.getHandler().getStackInSlot(OUTPUT_SLOT);
        ItemStack recipeResult = cachedRecipe.getResult();

        if (outputStack.isEmpty())
            return true;
        if (!ItemStack.isSameItemSameComponents(outputStack, recipeResult))
            return false;
        return outputStack.getCount() + recipeResult.getCount() <= outputStack.getMaxStackSize();
    }

    @Override
    public void onProcessComplete() {
        if (cachedRecipe == null)
            return;

        // Consume one of each input
        inventory.getHandler().extractItem(INPUT_SLOT_1, 1, false);
        inventory.getHandler().extractItem(INPUT_SLOT_2, 1, false);
        inventory.getHandler().extractItem(INPUT_SLOT_3, 1, false);

        // Produce output
        ItemStack result = cachedRecipe.getResult();
        ItemStack currentOutput = inventory.getHandler().getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) {
            inventory.getHandler().setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            currentOutput.grow(result.getCount());
        }

        // Invalidate recipe cache after crafting
        recipeCacheDirty = true;
    }

    @Override
    public int getProcessTime() {
        return Config.PRIMAL_CATALYST_PROCESS_TIME.get();
    }

    @Override
    public int getEnergyCost() {
        int baseCost = Config.PRIMAL_CATALYST_ENERGY_PER_TICK.get();
        if (cachedRecipe != null) {
            return Math.round(baseCost * cachedRecipe.getEnergyModifier());
        }
        return baseCost;
    }

    // ==================== SlotAccessRules Implementation ====================

    @Override
    public boolean canInsert(int slot, @NotNull ItemStack stack, @Nullable Direction side) {
        if (side == null) {
            // Internal access (GUI): allow inputs and fuel, deny output
            return slot != OUTPUT_SLOT;
        }
        return switch (side) {
            case UP -> slot >= INPUT_SLOT_1 && slot <= INPUT_SLOT_3;
            case DOWN -> false;
            default -> slot == FUEL_SLOT && stack.is(Items.COAL_BLOCK);
        };
    }

    @Override
    public boolean canExtract(int slot, @Nullable Direction side) {
        if (side == null)
            return true;
        return side == Direction.DOWN && slot == OUTPUT_SLOT;
    }

    // ==================== Framework Hooks ====================

    @Override
    protected IMachineProcess getProcess() {
        return this;
    }

    @Override
    public SlotAccessRules getSlotAccessRules() {
        return this;
    }

    @Override
    protected boolean isSlotValid(int slot, @NotNull ItemStack stack) {
        if (slot == OUTPUT_SLOT)
            return false;
        if (slot == FUEL_SLOT)
            return stack.is(Items.COAL_BLOCK);
        return true;
    }

    @Override
    protected int getFuelSlot() {
        return FUEL_SLOT;
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[] { OUTPUT_SLOT };
    }

    @Override
    protected Predicate<ItemStack> getFuelValidator() {
        return stack -> stack.is(Items.COAL_BLOCK);
    }

    @Override
    protected ToIntFunction<ItemStack> getBurnTimeFunction() {
        return stack -> stack.is(Items.COAL_BLOCK) ? COAL_BLOCK_BURN_TIME : 0;
    }

    // ==================== Recipe Cache Invalidation ====================

    @Override
    protected void markInventoryDirty() {
        super.markInventoryDirty();
        recipeCacheDirty = true;
    }

    // ==================== Recipe Lookup ====================

    private PrimalCatalystRecipe findRecipe() {
        if (level == null)
            return null;

        PrimalCatalystRecipeInput input = new PrimalCatalystRecipeInput(
                inventory.getHandler().getStackInSlot(INPUT_SLOT_1),
                inventory.getHandler().getStackInSlot(INPUT_SLOT_2),
                inventory.getHandler().getStackInSlot(INPUT_SLOT_3));

        RecipeManager recipeManager = level.getRecipeManager();
        Optional<RecipeHolder<PrimalCatalystRecipe>> recipe = recipeManager
                .getRecipeFor(ModRecipes.PRIMAL_CATALYST_TYPE.get(), input, level);

        return recipe.map(RecipeHolder::value).orElse(null);
    }

    // ==================== Menu Provider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.primal_catalyst");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        return new PrimalCatalystMenu(containerId, playerInventory, this, this.data);
    }
}
