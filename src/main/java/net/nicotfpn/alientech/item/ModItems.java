package net.nicotfpn.alientech.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;

import javax.swing.*;

public class ModItems {
    public static final DeferredRegister.Items  ITEMS = DeferredRegister.createItems(AlienTech.MODID);

    public static final DeferredItem<Item> GRAVITON = ITEMS.register("graviton",
        () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
