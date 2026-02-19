package net.nicotfpn.alientech.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.ModBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, AlienTech.MOD_ID);

    // Aba de Materiais Alienígenas
    public static final Supplier<CreativeModeTab> ALIEN_MATERIALS_TAB = CREATIVE_MODE_TABS.register("alien_materials",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.alientech.alien_materials"))
                    .icon(() -> new ItemStack(ModItems.NEUTRION_INGOT.get()))
                    .displayItems((parameters, output) -> {
                        // Materiais brutos e processados
                        output.accept(ModItems.RAW_NEUTRION.get());
                        output.accept(ModItems.NEUTRION_INGOT.get());
                        output.accept(ModItems.GRAVITON.get());
                        output.accept(ModItems.GRAVION_DISK.get());
                        output.accept(ModItems.GRAVITON_CONTAINED.get());
                        output.accept(ModItems.CONCENTRATED_SUBSTRATE.get());
                        output.accept(ModItems.EXOTIC_SUBSTRATE.get());
                        output.accept(ModItems.INERTIAL_STABILITY_ALLOY.get());
                        output.accept(ModItems.RAINBOW_CAPTURED.get());

                        // Blocos (se tiver)
                        output.accept(ModBlocks.NEUTRION_ORE.get());
                        output.accept(ModBlocks.NEUTRION_BLOCK.get());
                        output.accept(ModBlocks.PRIMAL_CATALYST.get());
                        output.accept(ModBlocks.PYRAMID_CORE.get());
                        output.accept(ModBlocks.ANCIENT_CHARGER.get());
                        output.accept(net.nicotfpn.alientech.registration.AlienBlocks.ANCIENT_BATTERY.get());

                        // Phase 2-3: Entropy Ecosystem
                        output.accept(ModItems.POCKET_DIMENSIONAL_PRISON.get());
                        output.accept(ModItems.ENTROPY_BIOMASS.get());
                        output.accept(ModBlocks.DECAY_CHAMBER_CONTROLLER.get());
                        output.accept(ModBlocks.DECAY_CHAMBER.get());

                        // Phase 4: Entropy Reservoir
                        output.accept(ModItems.DECAYING_GRAVITON.get());
                        output.accept(ModBlocks.ENTROPY_RESERVOIR.get());

                        // Phase 5: Quantum Vacuum Turbine
                        output.accept(ModBlocks.QUANTUM_VACUUM_TURBINE.get());

                        // Phase 6: Pyramid Structure
                        output.accept(ModBlocks.ALIEN_PYRAMID_CASING.get());

                        // Phase 7: Entropy Transport
                        output.accept(ModBlocks.ENTROPY_CABLE.get());
                        output.accept(ModBlocks.EVOLUTION_CHAMBER.get());

                    })
                    .build());

    // Aba de Tecnologias Alienígenas
    public static final Supplier<CreativeModeTab> ALIEN_TECHS_TAB = CREATIVE_MODE_TABS.register("alien_techs",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.alientech.alien_techs"))
                    .icon(() -> new ItemStack(ModItems.ANCIENT_ANKH.get()))
                    .displayItems((parameters, output) -> {
                        // Artefatos e itens especiais
                        output.accept(ModItems.ANCIENT_ANKH.get());
                        output.accept(ModItems.HORUS_EYE.get());
                        output.accept(ModItems.HORUS_EYE_ACTIVATED.get());
                        output.accept(ModItems.PHARAOH_SWORD.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}