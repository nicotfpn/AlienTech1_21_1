package net.nicotfpn.alientech.network.sideconfig;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.nicotfpn.alientech.machine.core.component.InventoryComponent;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;

/**
 * Proxy de IItemHandler que filtra operações baseado no SideConfigComponent.
 *
 * Instanciado UMA VEZ por direção no construtor da BlockEntity.
 * Não cria objetos intermediários no tick — todas as operações são O(1)
 * delegações.
 */
public class SidedItemHandlerWrapper implements IItemHandler {

    private final InventoryComponent inventory;
    private final SideConfigComponent sideConfig;
    private final Direction face;

    public SidedItemHandlerWrapper(InventoryComponent inventory,
            SideConfigComponent sideConfig,
            Direction face) {
        this.inventory = inventory;
        this.sideConfig = sideConfig;
        this.face = face;
    }

    @Override
    public int getSlots() {
        return inventory.getHandler().getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getHandler().getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        IOSideMode mode = sideConfig.getMode(face, CapabilityType.ITEM);
        if (!mode.allowsInsertion()) {
            return stack; // Face bloqueada para inserção — retorna o item sem consumir
        }
        return inventory.getHandler().insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        IOSideMode mode = sideConfig.getMode(face, CapabilityType.ITEM);
        if (!mode.allowsExtraction()) {
            return ItemStack.EMPTY; // Face bloqueada para extração
        }
        return inventory.getHandler().extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getHandler().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inventory.getHandler().isItemValid(slot, stack);
    }
}
