package net.nicotfpn.alientech.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nicotfpn.alientech.block.ModBlocks;
import net.nicotfpn.alientech.block.entity.PyramidCoreBlockEntity;

public class PyramidCoreMenu extends AbstractContainerMenu {
    public final PyramidCoreBlockEntity blockEntity;
    private final ContainerData data;

    public PyramidCoreMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(4));
    }

    public PyramidCoreMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PYRAMID_CORE_MENU.get(), pContainerId);
        blockEntity = ((PyramidCoreBlockEntity) entity);
        this.data = data;

        // Eye of Horus slot
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 80, 35));

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Registra os data slots para sincronização servidor->cliente
        addDataSlots(data);
    }

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 1; // Inventory starts after TE slot
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_SLOT_COUNT = 1;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem())
            return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                pPlayer, ModBlocks.PYRAMID_CORE.get());
    }

    // Energia é dividida em 2 ints (ContainerData usa shorts internamente)
    // data[0] = energy low bits, data[1] = energy high bits
    // data[2] = isActive (0 ou 1), data[3] = alloy count
    public int getEnergyStored() {
        return data.get(0) | (data.get(1) << 16);
    }

    public int getMaxEnergy() {
        return blockEntity.getMaxEnergy();
    }

    public boolean isActive() {
        return data.get(2) != 0;
    }

    public int getAlloyCount() {
        return data.get(3);
    }

    private void addPlayerInventory(Inventory pPlayerInventory) {
        for (int i = 0; i < PLAYER_INVENTORY_ROW_COUNT; i++) {
            for (int l = 0; l < PLAYER_INVENTORY_COLUMN_COUNT; l++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(pPlayerInventory,
                        l + i * PLAYER_INVENTORY_COLUMN_COUNT + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory pPlayerInventory) {
        for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(pPlayerInventory, i, 8 + i * 18, 142));
        }
    }
}
