package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.EnergyComponent;
import net.nicotfpn.alientech.machine.core.component.EntropyComponent;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.ProcessingComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;
import net.nicotfpn.alientech.machine.core.component.AutoTransferComponent;
import net.nicotfpn.alientech.pyramid.PyramidNetwork;
import net.nicotfpn.alientech.pyramid.PyramidTier;
import net.nicotfpn.alientech.recipe.ModRecipes;
import net.nicotfpn.alientech.recipe.PrimalCatalystRecipe;
import net.nicotfpn.alientech.recipe.PrimalCatalystRecipeInput;
import net.nicotfpn.alientech.screen.PrimalCatalystMenu;
import net.nicotfpn.alientech.util.SafeNBT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Primal Catalyst — A 3-input processing machine driven by Entropy.
 * <p>
 * ECS Architecture:
 * - {@link InventoryComponent}: 5 slots (3 inputs + 1 fuel[inert] + 1 output)
 * - {@link EnergyComponent}: optional FE buffer for display/compat
 * - {@link EntropyComponent}: local entropy buffer pulled from PyramidNetwork
 * - {@link ProcessingComponent}: manages recipe progress tick loop
 */
public class PrimalCatalystBlockEntity extends AlienMachineBlockEntity implements net.minecraft.world.MenuProvider {

    // ==================== Slot Constants ====================
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int INPUT_SLOT_3 = 2;
    public static final int FUEL_SLOT = 3; // deprecated/inert, kept for slot layout compat
    public static final int OUTPUT_SLOT = 4;
    private static final int SLOT_COUNT = 5;

    // ==================== Components ====================
    public final InventoryComponent inventoryComponent;
    public final EnergyComponent energyComponent;
    public final EntropyComponent entropyComponent;
    public final ProcessingComponent processingComponent;
    public final SideConfigComponent sideConfig;
    public final AutoTransferComponent autoTransfer;

    // ==================== Recipe Cache ====================
    private PrimalCatalystRecipe cachedRecipe = null;
    private boolean recipeCacheDirty = true;

    // ==================== Constructor ====================

    public PrimalCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRIMAL_CATALYST_BE.get(), pos, state);

        this.inventoryComponent = new InventoryComponent(this, SLOT_COUNT, this::isSlotValid) {
            @Override
            public void save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                super.save(tag, provider);
            }
            // onContentsChanged in ItemStackHandler already calls tile.setChanged()
            // We piggyback by checking recipeCacheDirty on each canProcess() call
        };
        registerComponent(this.inventoryComponent);

        this.energyComponent = new EnergyComponent(this,
                Config.PRIMAL_CATALYST_CAPACITY.get(), 1000, 0);
        registerComponent(this.energyComponent);

        this.entropyComponent = new EntropyComponent(this, Config.PRIMAL_CATALYST_CAPACITY.get());
        registerComponent(this.entropyComponent);

        this.processingComponent = new ProcessingComponent(this,
                () -> Config.PRIMAL_CATALYST_PROCESS_TIME.get(),
                this::onProcessComplete);
        registerComponent(this.processingComponent);

        this.sideConfig = new SideConfigComponent(this);
        registerComponent(this.sideConfig);

        this.autoTransfer = new AutoTransferComponent(this);
        this.autoTransfer.injectSideConfig(this.sideConfig);
        registerComponent(this.autoTransfer);

        initSidedWrappers();
    }

    // ==================== Slot Validation ====================

    private boolean isSlotValid(int slot, @NotNull ItemStack stack) {
        return slot != OUTPUT_SLOT && slot != FUEL_SLOT;
    }

    // ==================== Core Tick Logic ====================

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide)
            return;

        // Pull entropy from PyramidNetwork every tick if we have space
        long space = entropyComponent.getMaxEntropy() - entropyComponent.getEntropyStored();
        if (space > 0) {
            long cap = (long) Config.ENTROPY_CABLE_TRANSFER_RATE.get();
            long pulled = PyramidNetwork.get(level).extractEntropy(Math.min(space, cap), false);
            if (pulled > 0)
                entropyComponent.addEntropy(pulled);
        }

        // Determine if we can work
        boolean canWork = canProcess();
        processingComponent.setWorking(canWork);

        super.tickServer(); // Ticks ProcessingComponent (advances progress)

        setChanged();
    }

    private boolean canProcess() {
        if (level == null)
            return false;

        // Check entropy
        long entropyPerTick = Config.PRIMAL_CATALYST_ENERGY_PER_TICK.get();
        if (entropyComponent.getEntropyStored() < entropyPerTick)
            return false;

        // Check pyramid tier
        PyramidTier tier = PyramidNetwork.get(level).getTier();
        if (tier == PyramidTier.NONE)
            return false;

        if (recipeCacheDirty) {
            cachedRecipe = findRecipe();
            recipeCacheDirty = false;
        }
        if (cachedRecipe == null)
            return false;

        // Check output space
        ItemStack outputStack = inventoryComponent.getHandler().getStackInSlot(OUTPUT_SLOT);
        ItemStack recipeResult = cachedRecipe.getResult();
        if (outputStack.isEmpty())
            return true;
        if (!ItemStack.isSameItemSameComponents(outputStack, recipeResult))
            return false;
        return outputStack.getCount() + recipeResult.getCount() <= outputStack.getMaxStackSize();
    }

    private void onProcessComplete() {
        if (cachedRecipe == null)
            return;

        // Consume inputs
        inventoryComponent.getHandler().extractItem(INPUT_SLOT_1, 1, false);
        inventoryComponent.getHandler().extractItem(INPUT_SLOT_2, 1, false);
        inventoryComponent.getHandler().extractItem(INPUT_SLOT_3, 1, false);

        // Produce output
        ItemStack result = cachedRecipe.getResult();
        ItemStack current = inventoryComponent.getHandler().getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) {
            inventoryComponent.getHandler().setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            current.grow(result.getCount());
        }

        // Consume entropy for this craft
        long entropyPerTick = Config.PRIMAL_CATALYST_ENERGY_PER_TICK.get();
        entropyComponent.consumeEntropy(entropyPerTick);

        recipeCacheDirty = true;
        setChanged();
    }

    // ==================== Recipe Lookup ====================

    private PrimalCatalystRecipe findRecipe() {
        if (level == null)
            return null;
        PrimalCatalystRecipeInput input = new PrimalCatalystRecipeInput(
                inventoryComponent.getHandler().getStackInSlot(INPUT_SLOT_1),
                inventoryComponent.getHandler().getStackInSlot(INPUT_SLOT_2),
                inventoryComponent.getHandler().getStackInSlot(INPUT_SLOT_3));
        RecipeManager rm = level.getRecipeManager();
        Optional<RecipeHolder<PrimalCatalystRecipe>> found = rm.getRecipeFor(ModRecipes.PRIMAL_CATALYST_TYPE.get(),
                input, level);
        return found.map(RecipeHolder::value).orElse(null);
    }

    // ==================== Component Accessors ====================

    public net.neoforged.neoforge.items.IItemHandler getItemHandler() {
        return inventoryComponent.getHandler();
    }

    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyStorage() {
        return energyComponent.getEnergyStorage();
    }

    /** Expose entropy handler for capability registration. */
    public net.nicotfpn.alientech.entropy.IEntropyHandler getEntropyHandler() {
        return new net.nicotfpn.alientech.entropy.IEntropyHandler() {
            @Override
            public long getEntropy() {
                return entropyComponent.getEntropyStored();
            }

            @Override
            public long getMaxEntropy() {
                return entropyComponent.getMaxEntropy();
            }

            @Override
            public long insertEntropy(long a, boolean sim) {
                long space = entropyComponent.getMaxEntropy() - entropyComponent.getEntropyStored();
                long acc = Math.min(a, space);
                if (!sim && acc > 0)
                    entropyComponent.addEntropy(acc);
                return acc;
            }

            @Override
            public long extractEntropy(long a, boolean sim) {
                long ex = Math.min(a, entropyComponent.getEntropyStored());
                if (!sim && ex > 0)
                    entropyComponent.consumeEntropy(ex);
                return ex;
            }

            @Override
            public boolean canInsert() {
                return true;
            }

            @Override
            public boolean canExtract() {
                return false;
            }
        };
    }

    // ==================== Drops ====================

    @Override
    public void drops() {
        if (level != null)
            inventoryComponent.dropAll(level, worldPosition);
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.primal_catalyst");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new PrimalCatalystMenu(id, inv, this);
    }

    // ==================== Persistence ====================

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); // Saves ECS components
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); // Loads ECS components

        // === Legacy NBT Migration (AbstractMachineBlockEntity format) ===
        if (!tag.contains("Components")) {
            // Energy
            if (tag.contains("MachineEnergy")) {
                CompoundTag et = tag.getCompound("MachineEnergy");
                if (et.contains("Stored"))
                    energyComponent.getEnergyStorage().setEnergy(et.getInt("Stored"));
            }
            // Entropy
            if (tag.contains("PrimalEntropy")) {
                CompoundTag eTag = tag.getCompound("PrimalEntropy");
                if (eTag.contains("Entropy"))
                    entropyComponent.setEntropyStored(eTag.getLong("Entropy"));
            }
            // Processor progress
            if (tag.contains("MachineProcessor")) {
                CompoundTag pt = tag.getCompound("MachineProcessor");
                int p = SafeNBT.getInt(pt, "Progress", 0);
                if (p > 0)
                    processingComponent.setProgress(p);
            }
        }
    }
}
