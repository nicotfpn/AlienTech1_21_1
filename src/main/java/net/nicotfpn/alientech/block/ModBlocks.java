package net.nicotfpn.alientech.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.custom.AncientChargerBlock;
import net.nicotfpn.alientech.block.custom.PrimalCatalystBlock;
import net.nicotfpn.alientech.block.custom.PyramidCoreBlock;
import net.nicotfpn.alientech.item.ModItems;
import java.util.function.Supplier;

public class ModBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AlienTech.MOD_ID);

        public static final DeferredBlock<Block> NEUTRION_ORE = registerBlock("neutrion_ore",
                        () -> new DropExperienceBlock(UniformInt.of(2, 4),
                                        BlockBehaviour.Properties.of().strength(3f)
                                                        .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

        public static final DeferredBlock<Block> NEUTRION_BLOCK = registerBlock("neutrion_block",
                        () -> new Block(BlockBehaviour.Properties.of()
                                        .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

        public static final DeferredBlock<Block> PRIMAL_CATALYST = registerBlock("primal_catalyst",
                        () -> new PrimalCatalystBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_PURPLE)
                                        .strength(2.5f)
                                        .sound(SoundType.NETHERITE_BLOCK)
                                        .lightLevel(state -> 7)
                                        .requiresCorrectToolForDrops()));

        public static final DeferredBlock<Block> PYRAMID_CORE = registerBlockCustomItem("pyramid_core",
                        () -> new PyramidCoreBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_YELLOW)
                                        .strength(3f)
                                        .sound(SoundType.STONE)
                                        .lightLevel(state -> 8)
                                        .requiresCorrectToolForDrops()),
                        block -> new net.nicotfpn.alientech.item.custom.PyramidCoreBlockItem(block,
                                        new Item.Properties()));

        public static final DeferredBlock<Block> ANCIENT_CHARGER = registerBlockCustomItem("ancient_charger",
                        () -> new AncientChargerBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.GOLD)
                                        .strength(3f)
                                        .sound(SoundType.NETHERITE_BLOCK)
                                        .lightLevel(state -> 5)
                                        .requiresCorrectToolForDrops()),
                        // Custom BlockItem for energy tooltip
                        block -> new net.nicotfpn.alientech.item.custom.AncientChargerBlockItem(block,
                                        new Item.Properties()));

        // === Phase 3: Decay Chamber ===

        public static final DeferredBlock<Block> DECAY_CHAMBER_CONTROLLER = registerBlock("decay_chamber_controller",
                        () -> new net.nicotfpn.alientech.machine.decay.DecayChamberControllerBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.COLOR_GREEN)
                                                        .strength(3.5f)
                                                        .sound(SoundType.NETHERITE_BLOCK)
                                                        .requiresCorrectToolForDrops()));

        public static final DeferredBlock<Block> DECAY_CHAMBER = registerBlock("decay_chamber",
                        () -> new net.nicotfpn.alientech.machine.decay.DecayChamberBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.COLOR_GREEN)
                                                        .strength(3.5f)
                                                        .sound(SoundType.NETHERITE_BLOCK)
                                                        .requiresCorrectToolForDrops()));

        // === Phase 4: Entropy Reservoir ===

        public static final DeferredBlock<Block> ENTROPY_RESERVOIR = registerBlock("entropy_reservoir",
                        () -> new net.nicotfpn.alientech.machine.entropy.EntropyReservoirBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.COLOR_PURPLE)
                                                        .strength(3.5f)
                                                        .sound(SoundType.NETHERITE_BLOCK)
                                                        .requiresCorrectToolForDrops()));

        // === Phase 5: Quantum Vacuum Turbine ===

        public static final DeferredBlock<Block> QUANTUM_VACUUM_TURBINE = registerBlock("quantum_vacuum_turbine",
                        () -> new net.nicotfpn.alientech.machine.turbine.QuantumVacuumTurbineBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.COLOR_CYAN)
                                                        .strength(4f)
                                                        .sound(SoundType.NETHERITE_BLOCK)
                                                        .lightLevel(state -> 5)
                                                        .requiresCorrectToolForDrops()));

        // === Phase 6: Pyramid Structure ===

        public static final DeferredBlock<Block> ALIEN_PYRAMID_CASING = registerBlock("alien_pyramid_casing",
                        () -> new net.nicotfpn.alientech.block.custom.AlienPyramidCasingBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.SAND)
                                                        .strength(3f)
                                                        .sound(SoundType.STONE)
                                                        .requiresCorrectToolForDrops()));

        // === Phase 7: Entropy Transport ===

        public static final DeferredBlock<Block> ENTROPY_CABLE = registerBlock("entropy_cable",
                        () -> new net.nicotfpn.alientech.block.custom.EntropyCableBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.METAL)
                                                        .strength(1.5f)
                                                        .sound(SoundType.METAL)
                                                        .noOcclusion()));

        // === Phase 9: Evolution Chamber ===

        public static final DeferredBlock<Block> EVOLUTION_CHAMBER = registerBlock("evolution_chamber",
                        () -> new net.nicotfpn.alientech.machine.evolution.EvolutionChamberBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.COLOR_PURPLE)
                                                        .strength(3.0f)
                                                        .sound(SoundType.AMETHYST_BLOCK)
                                                        .lightLevel(state -> 5)
                                                        .requiresCorrectToolForDrops()
                                                        .noOcclusion()));

        /*
         * public static final DeferredBlock<Block> ANCIENT_BATTERY =
         * registerBlockCustomItem("ancient_battery",
         * () -> new AncientBatteryBlock(BlockBehaviour.Properties.of()
         * .mapColor(MapColor.GOLD)
         * .strength(3f)
         * .sound(SoundType.NETHERITE_BLOCK)
         * .lightLevel(state -> 3)
         * .requiresCorrectToolForDrops()),
         * block -> new
         * net.nicotfpn.alientech.item.custom.AncientBatteryBlockItem(block,
         * new Item.Properties().stacksTo(1)));
         */

        private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
                DeferredBlock<T> toReturn = BLOCKS.register(name, block);
                registerBlockItem(name, toReturn);
                return toReturn;
        }

        private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
                ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        }

        private static <T extends Block> DeferredBlock<T> registerBlockCustomItem(String name, Supplier<T> block,
                        java.util.function.Function<T, BlockItem> itemFactory) {
                DeferredBlock<T> toReturn = BLOCKS.register(name, block);
                ModItems.ITEMS.register(name, () -> itemFactory.apply(toReturn.get()));
                return toReturn;
        }

        public static void register(IEventBus eventBus) {
                BLOCKS.register(eventBus);
        }
}
