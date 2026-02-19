package net.nicotfpn.alientech.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
                        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AlienTech.MOD_ID);

        public static final Supplier<BlockEntityType<PrimalCatalystBlockEntity>> PRIMAL_CATALYST_BE = BLOCK_ENTITIES
                        .register("primal_catalyst_be", () -> BlockEntityType.Builder.of(PrimalCatalystBlockEntity::new,
                                        ModBlocks.PRIMAL_CATALYST.get()).build(null));

        public static final Supplier<BlockEntityType<PyramidCoreBlockEntity>> PYRAMID_CORE_BE = BLOCK_ENTITIES
                        .register("pyramid_core_be", () -> BlockEntityType.Builder.of(PyramidCoreBlockEntity::new,
                                        ModBlocks.PYRAMID_CORE.get()).build(null));

        public static final Supplier<BlockEntityType<AncientChargerBlockEntity>> ANCIENT_CHARGER_BE = BLOCK_ENTITIES
                        .register("ancient_charger_be", () -> BlockEntityType.Builder.of(AncientChargerBlockEntity::new,
                                        ModBlocks.ANCIENT_CHARGER.get()).build(null));

        public static final Supplier<BlockEntityType<AncientBatteryBlockEntity>> ANCIENT_BATTERY_BE = BLOCK_ENTITIES
                        .register("ancient_battery_be", () -> BlockEntityType.Builder.of(AncientBatteryBlockEntity::new,
                                        net.nicotfpn.alientech.registration.AlienBlocks.ANCIENT_BATTERY.get())
                                        .build(null));

        // === Phase 3: Decay Chamber ===

        public static final Supplier<BlockEntityType<net.nicotfpn.alientech.machine.decay.DecayChamberControllerBlockEntity>> DECAY_CHAMBER_CONTROLLER_BE = BLOCK_ENTITIES
                        .register("decay_chamber_controller_be", () -> BlockEntityType.Builder.of(
                                        net.nicotfpn.alientech.machine.decay.DecayChamberControllerBlockEntity::new,
                                        ModBlocks.DECAY_CHAMBER_CONTROLLER.get()).build(null));

        // === Phase 4: Entropy Reservoir ===

        public static final Supplier<BlockEntityType<net.nicotfpn.alientech.machine.entropy.EntropyReservoirBlockEntity>> ENTROPY_RESERVOIR_BE = BLOCK_ENTITIES
                        .register("entropy_reservoir_be", () -> BlockEntityType.Builder.of(
                                        net.nicotfpn.alientech.machine.entropy.EntropyReservoirBlockEntity::new,
                                        ModBlocks.ENTROPY_RESERVOIR.get()).build(null));

        // === Phase 5: Quantum Vacuum Turbine ===

        public static final Supplier<BlockEntityType<net.nicotfpn.alientech.machine.turbine.QuantumVacuumTurbineBlockEntity>> QUANTUM_VACUUM_TURBINE_BE = BLOCK_ENTITIES
                        .register("quantum_vacuum_turbine_be", () -> BlockEntityType.Builder.of(
                                        net.nicotfpn.alientech.machine.turbine.QuantumVacuumTurbineBlockEntity::new,
                                        ModBlocks.QUANTUM_VACUUM_TURBINE.get()).build(null));

        // === Phase 7: Entropy Transport ===

        public static final Supplier<BlockEntityType<EntropyCableBlockEntity>> ENTROPY_CABLE_BE = BLOCK_ENTITIES
                        .register("entropy_cable_be", () -> BlockEntityType.Builder.of(
                                        EntropyCableBlockEntity::new,
                                        ModBlocks.ENTROPY_CABLE.get()).build(null));

        // === Phase 9: Evolution Chamber ===

        public static final Supplier<BlockEntityType<net.nicotfpn.alientech.machine.evolution.EvolutionChamberBlockEntity>> EVOLUTION_CHAMBER_BE = BLOCK_ENTITIES
                        .register("evolution_chamber_be", () -> BlockEntityType.Builder.of(
                                        net.nicotfpn.alientech.machine.evolution.EvolutionChamberBlockEntity::new,
                                        ModBlocks.EVOLUTION_CHAMBER.get()).build(null));

        public static void register(IEventBus eventBus) {
                BLOCK_ENTITIES.register(eventBus);
        }
}
