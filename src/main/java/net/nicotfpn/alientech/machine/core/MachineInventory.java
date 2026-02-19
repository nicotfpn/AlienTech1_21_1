package net.nicotfpn.alientech.machine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * Encapsulates all inventory logic for a machine.
 * Wraps an ItemStackHandler with slot validation, NBT persistence, and item
 * dropping.
 */
public class MachineInventory {

    private final ItemStackHandler handler;
    private @Nullable BiPredicate<Integer, ItemStack> slotValidator;

    /**
     * @param slotCount number of inventory slots
     * @param onChanged callback invoked when any slot contents change (for dirty
     *                  marking)
     */
    public MachineInventory(int slotCount, Runnable onChanged) {
        this.handler = new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slotValidator == null)
                    return true;
                return slotValidator.test(slot, stack);
            }
        };
    }

    // ==================== Configuration ====================

    /**
     * Set the slot validation predicate. Called once after construction.
     * The predicate receives (slotIndex, itemStack) and returns true if the item is
     * valid.
     */
    public void setSlotValidator(BiPredicate<Integer, ItemStack> validator) {
        this.slotValidator = validator;
    }

    // ==================== Accessors ====================

    public ItemStackHandler getHandler() {
        return handler;
    }

    public int getSlots() {
        return handler.getSlots();
    }

    // ==================== NBT Persistence ====================

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Inventory", handler.serializeNBT(registries));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    // ==================== Block Removal ====================

    /**
     * Drop all inventory contents into the world when the block is broken.
     */
    public void dropAll(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            container.setItem(i, handler.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, container);
    }
}
