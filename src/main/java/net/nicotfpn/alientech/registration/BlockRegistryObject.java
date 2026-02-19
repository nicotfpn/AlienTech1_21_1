package net.nicotfpn.alientech.registration;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class BlockRegistryObject<BLOCK extends Block, ITEM extends Item> implements Supplier<BLOCK>, ItemLike {

    private final DeferredHolder<Block, BLOCK> blockHolder;
    private final DeferredHolder<Item, ITEM> itemHolder;

    public BlockRegistryObject(DeferredHolder<Block, BLOCK> blockHolder, DeferredHolder<Item, ITEM> itemHolder) {
        this.blockHolder = blockHolder;
        this.itemHolder = itemHolder;
    }

    @Override
    public BLOCK get() {
        return blockHolder.get();
    }

    public @NotNull BLOCK value() {
        return blockHolder.value();
    }

    @Override
    public @NotNull ITEM asItem() {
        return itemHolder.get();
    }

    public DeferredHolder<Block, BLOCK> getBlockHolder() {
        return blockHolder;
    }

    public DeferredHolder<Item, ITEM> getItemHolder() {
        return itemHolder;
    }

    public Component getTextComponent() {
        return value().getName();
    }
}
