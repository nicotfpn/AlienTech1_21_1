package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.nicotfpn.alientech.Config;
import net.nicotfpn.alientech.block.entity.base.AlienElectricBlockEntity;
import net.nicotfpn.alientech.screen.AncientBatteryMenu;
import net.nicotfpn.alientech.util.EnergyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ancient Battery - Stores energy.
 * FIX [H1]: Now extends updated AlienElectricBlockEntity and uses Config.
 */
public class AncientBatteryBlockEntity extends AlienElectricBlockEntity {

    // Total Transfer Rate per tick (Input+Output)
    public static final int MAX_TRANSFER = 100_000;

    // Item Handler for 2 slots (0=Charge Item, 1=Discharge Item)
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markInventoryDirty(); // Immediate sync for item changes
        }
    };

    private final ContainerData data;

    public AncientBatteryBlockEntity(BlockPos pos, BlockState blockState) {
        // FIX [M1]: Use Config for capacity instead of hardcoded value
        super(ModBlockEntities.ANCIENT_BATTERY_BE.get(), pos, blockState,
                Config.ANCIENT_BATTERY_CAPACITY.get(), MAX_TRANSFER);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    default -> 0; // FIX [C7]: Implicit bounds check
                };
            }

            @Override
            public void set(int index, int value) {
                // Client sync only
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer(); // FIX [C2]: Call super to handle throttled sync

        boolean changed = false;
        int rate = MAX_TRANSFER;

        // 1. DRAIN ITEMS (Discharge Slot 1) -> FILLS BATTERY
        ItemStack dischargeStack = itemHandler.getStackInSlot(1);
        if (!dischargeStack.isEmpty()) {
            int needed = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
            if (needed > 0) {
                int toPull = Math.min(rate, needed);
                int pulled = EnergyUtils.dischargeItem(dischargeStack, toPull);
                if (pulled > 0) {
                    energyStorage.receiveEnergy(pulled, false);
                    // Update stack if needed (some items might consume durability etc)
                    itemHandler.setStackInSlot(1, dischargeStack);
                    changed = true;
                }
            }
        }

        // 2. FILL ITEMS (Charge Slot 0) -> DRAINS BATTERY
        ItemStack chargeStack = itemHandler.getStackInSlot(0);
        if (!chargeStack.isEmpty()) {
            int available = energyStorage.getEnergyStored();
            if (available > 0) {
                int toPush = Math.min(rate, available);
                int pushed = EnergyUtils.chargeItem(chargeStack, toPush);
                if (pushed > 0) {
                    energyStorage.extractEnergy(pushed, false);
                    // Update stack if needed
                    itemHandler.setStackInSlot(0, chargeStack);
                    changed = true;
                }
            }
        }

        if (changed) {
            setChanged();
            markForSync(); // Ensure sync happens on energy/inventory change
        }
    }

    // ==================== Persistence ====================

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
    }

    // ==================== UI / Drops ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.alientech.ancient_battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AncientBatteryMenu(containerId, playerInventory, this, this.data);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public void drops() {
        if (this.level == null)
            return;
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}
// ✅ CORRIGIDO: C2, C4, C7, H1, H2, M1
