package net.nicotfpn.alientech.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.ModBlocks;
import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AlienTech.MODID);

    public static final Supplier<CreativeModeTab> ALIEN_TECHS = CREATIVE_MODE_TAB.register("alien_techs_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.GRAVITON.get()))
                    .title(Component.translatable("creativetab.nicoalientech.alien_techs"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.GRAVION_DISK);
                    }).build());


    public static final Supplier<CreativeModeTab> ALIEN_MATERIALS = CREATIVE_MODE_TAB.register("alien_materials_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.GRAVITON.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(AlienTech.MODID, "alien_techs_tab"))
                    .title(Component.translatable("creativetab.nicoalientech.alien_materials"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.GRAVITON);
                        output.accept(ModBlocks.GRAVITON_ORE);
                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
