package net.nicotfpn.alientech.machine.entropy;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.block.entity.base.AbstractMachineBlockEntity;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.machine.core.IMachineProcess;
import net.nicotfpn.alientech.machine.core.SlotAccessRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Entropy Reservoir — converts entropy biomass into decaying gravitons.
 * <p>
 * Slot layout:
 * - Slot 0: Input 1 (entropy biomass)
 * - Slot 1: Input 2 (entropy biomass)
 * - Slot 2: Output (decaying graviton)
 * <p>
 * No fuel slot — this machine runs purely on FE energy.
 * Uses the component-based {@link AbstractMachineBlockEntity} framework.
 */
public class EntropyReservoirBlockEntity extends AbstractMachineBlockEntity
        implements IMachineProcess, SlotAccessRules {

    // ==================== Slot Layout ====================
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    // ==================== Constructor ====================

    public EntropyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPY_RESERVOIR_BE.get(), pos, state,
                Config.ENTROPY_RESERVOIR_CAPACITY.get(), // energyCapacity
                Config.ENTROPY_RESERVOIR_CAPACITY.get(), // maxReceive (same)
                0, // maxExtract (no output)
                SLOT_COUNT);
    }

    // ==================== Local Entropy Buffer (present for architectural consistency)
    private final net.nicotfpn.alientech.entropy.EntropyStorage entropyStorage = new net.nicotfpn.alientech.entropy.EntropyStorage(
            Config.ENTROPY_RESERVOIR_CAPACITY.get(), () -> {
                setChanged();
                markForSync();
            });

    // ==================== IMachineProcess ====================

    @Override
    public boolean canProcess() {
        ItemStack input1 = inventory.getHandler().getStackInSlot(INPUT_SLOT_1);
        ItemStack input2 = inventory.getHandler().getStackInSlot(INPUT_SLOT_2);
        ItemStack output = inventory.getHandler().getStackInSlot(OUTPUT_SLOT);

        // Need at least 1 entropy biomass in each input slot
        if (input1.isEmpty() || !input1.is(ModItems.ENTROPY_BIOMASS.get()))
            return false;
        if (input2.isEmpty() || !input2.is(ModItems.ENTROPY_BIOMASS.get()))
            return false;

        // Check output has space
        ItemStack result = new ItemStack(ModItems.DECAYING_GRAVITON.get(), 1);
        if (output.isEmpty())
            return true;
        if (!ItemStack.isSameItemSameComponents(output, result))
            return false;
        return output.getCount() + 1 <= output.getMaxStackSize();
    }

    @Override
    public void onProcessComplete() {
        ItemStack input1 = inventory.getHandler().getStackInSlot(INPUT_SLOT_1);
        ItemStack input2 = inventory.getHandler().getStackInSlot(INPUT_SLOT_2);
        ItemStack output = inventory.getHandler().getStackInSlot(OUTPUT_SLOT);

        // Validate inputs before consuming
        if (input1.isEmpty() || input2.isEmpty()) {
            return; // Invalid state - should not happen if canProcess() passed
        }

        // Consume 1 from each input (atomic operation)
        input1.shrink(1);
        input2.shrink(1);

        // Produce 1 decaying graviton
        ItemStack result = new ItemStack(ModItems.DECAYING_GRAVITON.get(), 1);
        if (output.isEmpty()) {
            inventory.getHandler().setStackInSlot(OUTPUT_SLOT, result);
        } else {
            // Validate output can accept more
            if (ItemStack.isSameItemSameComponents(output, result)) {
                int newCount = output.getCount() + 1;
                if (newCount <= output.getMaxStackSize()) {
                    output.grow(1);
                }
                // If output is full, item is lost (player should clear output)
            }
        }
        
        // processing complete
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Validate state on load (handled by AbstractMachineBlockEntity)
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clean up resources if needed
    }

    @Override
    public int getProcessTime() {
        return Config.ENTROPY_RESERVOIR_PROCESS_TIME.get();
    }

    @Override
    public int getEnergyCost() {
        return Config.ENTROPY_RESERVOIR_ENERGY_PER_TICK.get();
    }

    @Override
    protected net.nicotfpn.alientech.entropy.EntropyStorage getEntropyStorage() {
        return entropyStorage;
    }

    /** Public entropy handler accessor for capability registration. */
    public net.nicotfpn.alientech.entropy.IEntropyHandler getEntropyHandler() {
        return entropyStorage;
    }

    @Override
    protected int getEntropyPerTick() {
        return 0;
    }

    @Override
    protected int getFEPerTick() {
        return getEnergyCost();
    }

    @Override
    public void saveAdditional(@org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag, @org.jetbrains.annotations.NotNull net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        entropyStorage.save(t);
        tag.put("EntropyReservoir_Entropy", t);
    }

    @Override
    public void loadAdditional(@org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag, @org.jetbrains.annotations.NotNull net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        net.minecraft.nbt.CompoundTag eTag = net.nicotfpn.alientech.util.SafeNBT.getCompound(tag, "EntropyReservoir_Entropy");
        if (eTag != null) {
            try {
                entropyStorage.load(eTag);
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== SlotAccessRules ====================

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        // Only input slots accept items from automation
        if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
            return stack.is(ModItems.ENTROPY_BIOMASS.get());
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, @Nullable Direction side) {
        // Only output slot allows extraction
        return slot == OUTPUT_SLOT;
    }

    // ==================== Abstract Hooks ====================

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
        return switch (slot) {
            case INPUT_SLOT_1, INPUT_SLOT_2 -> stack.is(ModItems.ENTROPY_BIOMASS.get());
            case OUTPUT_SLOT -> false; // No manual insertion into output
            default -> false;
        };
    }

    @Override
    protected int getFuelSlot() {
        return -1; // No fuel slot — FE powered only
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[] { OUTPUT_SLOT };
    }

    @Override
    protected Predicate<ItemStack> getFuelValidator() {
        return stack -> false; // No fuel
    }

    @Override
    protected ToIntFunction<ItemStack> getBurnTimeFunction() {
        return stack -> 0; // No fuel
    }

    // ==================== MenuProvider ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.alientech.entropy_reservoir");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory,
            @NotNull Player player) {
        // No GUI in Phase 4 — return null
        return null;
    }
}
