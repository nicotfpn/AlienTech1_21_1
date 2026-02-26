package net.nicotfpn.alientech.machine.core.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

/**
 * Delegates memory representation to an IItemHandler.
 */
public class InventoryComponent extends AlienComponent {

    private final ItemStackHandler itemHandler;

    public InventoryComponent(AlienMachineBlockEntity tile, int size) {
        this(tile, size, null);
    }

    public InventoryComponent(AlienMachineBlockEntity tile, int size, BiPredicate<Integer, ItemStack> slotValidator) {
        super(tile);
        this.itemHandler = new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                tile.setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slotValidator != null)
                    return slotValidator.test(slot, stack);
                return super.isItemValid(slot, stack);
            }
        };
    }

    @Override
    public String getId() {
        return "Inventory";
    }

    @Override
    public boolean isActive() {
        return false; // Inventories don't tick natively
    }

    public ItemStackHandler getHandler() {
        return itemHandler;
    }

    /** @deprecated Use getHandler() instead */
    @Deprecated
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public void dropAll(Level level, BlockPos pos) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("Items", itemHandler.serializeNBT(provider));
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(provider, tag.getCompound("Items"));
        }
    }
}
