package net.nicotfpn.alientech.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class AlienDeferredRegister {

    private final DeferredRegister<Block> blockRegister;
    private final DeferredRegister<Item> itemRegister;

    public AlienDeferredRegister(String modid) {
        this.blockRegister = DeferredRegister.create(Registries.BLOCK, modid);
        this.itemRegister = DeferredRegister.create(Registries.ITEM, modid);
    }

    public <BLOCK extends Block> BlockRegistryObject<BLOCK, BlockItem> register(String name,
            Supplier<BLOCK> blockSupplier) {
        DeferredHolder<Block, BLOCK> block = blockRegister.register(name, blockSupplier);
        DeferredHolder<Item, BlockItem> item = itemRegister.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()));
        return new BlockRegistryObject<>(block, item);
    }

    public <BLOCK extends Block> BlockRegistryObject<BLOCK, BlockItem> register(String name,
            Supplier<BLOCK> blockSupplier, Function<BLOCK, BlockItem> itemCreator) {
        DeferredHolder<Block, BLOCK> block = blockRegister.register(name, blockSupplier);
        DeferredHolder<Item, BlockItem> item = itemRegister.register(name, () -> itemCreator.apply(block.get()));
        return new BlockRegistryObject<>(block, item);
    }

    public void register(IEventBus bus) {
        blockRegister.register(bus);
        itemRegister.register(bus);
    }
}
