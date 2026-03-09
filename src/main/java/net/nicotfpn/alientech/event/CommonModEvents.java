package net.nicotfpn.alientech.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.entity.ModBlockEntities;
import net.nicotfpn.alientech.item.ModItems;
import net.nicotfpn.alientech.item.custom.ActivatedEyeOfHorusItem;
import net.nicotfpn.alientech.item.custom.ItemEnergyStorage;
import net.nicotfpn.alientech.item.custom.PharaohSwordItem;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.block.entity.EnergyCableBlockEntity;
import net.nicotfpn.alientech.block.entity.PrimalCatalystBlockEntity;
import net.nicotfpn.alientech.block.entity.CreativeAncientBatteryBlockEntity;

@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
                // Primal Catalyst - Energy Storage
                event.registerBlockEntity(
                                Capabilities.EnergyStorage.BLOCK,
                                ModBlockEntities.PRIMAL_CATALYST_BE.get(),
                                (be, context) -> be.getEnergyStorage());

                // Primal Catalyst - Item Handler
                event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                ModBlockEntities.PRIMAL_CATALYST_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedItemHandler(context);
                                        }
                                        return be.getItemHandler();
                                });

                // Pyramid Core - Energy Storage (output from all sides)
                event.registerBlockEntity(
                                Capabilities.EnergyStorage.BLOCK,
                                ModBlockEntities.PYRAMID_CORE_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedEnergyStorage(context);
                                        }
                                        return be.getEnergyStorage();
                                });

                // Pyramid Core - Item Handler
                event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                ModBlockEntities.PYRAMID_CORE_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedItemHandler(context);
                                        }
                                        return be.getItemHandler();
                                });

                // Ancient Charger - Energy Storage
                event.registerBlockEntity(
                                Capabilities.EnergyStorage.BLOCK,
                                ModBlockEntities.ANCIENT_CHARGER_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedEnergyStorage(context);
                                        }
                                        return be.getEnergyStorage();
                                });

                // Ancient Charger - Item Handler
                event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                ModBlockEntities.ANCIENT_CHARGER_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedItemHandler(context);
                                        }
                                        return be.getItemHandler();
                                });

                // Ancient Battery - Energy Storage
                event.registerBlockEntity(
                                Capabilities.EnergyStorage.BLOCK,
                                ModBlockEntities.ANCIENT_BATTERY_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedEnergyStorage(context);
                                        }
                                        return be.getEnergyStorage();
                                });

                // Ancient Battery - Item Handler
                event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                ModBlockEntities.ANCIENT_BATTERY_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedItemHandler(context);
                                        }
                                        return be.getItemHandler();
                                });

                // Energy Cables - Energy Storage (pass-through)
                event.registerBlockEntity(
                                Capabilities.EnergyStorage.BLOCK,
                                ModBlockEntities.ENERGY_CABLE_BE.get(),
                                (be, context) -> {
                                        if (be instanceof EnergyCableBlockEntity cable)
                                                return cable.getEnergyStorage();
                                        return null;
                                });

                // Creative Ancient Battery - Energy Storage
                event.registerBlockEntity(
                                Capabilities.EnergyStorage.BLOCK,
                                ModBlockEntities.CREATIVE_ANCIENT_BATTERY_BE.get(),
                                (be, context) -> {
                                        if (be instanceof CreativeAncientBatteryBlockEntity cab) {
                                                return cab.getEnergyStorage();
                                        }
                                        return null;
                                });

                // Activated Eye of Horus - Battery (5M FE)
                event.registerItem(
                                Capabilities.EnergyStorage.ITEM,
                                (stack, context) -> new ItemEnergyStorage(
                                                stack,
                                                ActivatedEyeOfHorusItem.MAX_ENERGY, // 5M capacity
                                                50000, // maxReceive
                                                50000 // maxExtract
                                ),
                                ModItems.HORUS_EYE_ACTIVATED.get());

                // Ancient Battery BlockItem - Energy Storage (1M FE)
                // Allows battery to hold energy when in inventory/charger
                event.registerItem(
                                Capabilities.EnergyStorage.ITEM,
                                (stack, context) -> new ItemEnergyStorage(
                                                stack,
                                                1_000_000, // 1M capacity
                                                50000, // maxReceive
                                                50000 // maxExtract
                                ),
                                net.nicotfpn.alientech.registration.AlienBlocks.ANCIENT_BATTERY.asItem());

                // Ancient Charger BlockItem - Energy Storage (10M FE)
                // Allows charger to hold energy when in inventory
                event.registerItem(
                                Capabilities.EnergyStorage.ITEM,
                                (stack, context) -> new ItemEnergyStorage(
                                                stack,
                                                10_000_000, // 10M capacity
                                                100000, // maxReceive
                                                100000 // maxExtract
                                ),
                                net.nicotfpn.alientech.block.ModBlocks.ANCIENT_CHARGER.asItem());

                // Pharaoh Sword - Energy Weapon (1M FE)
                event.registerItem(
                                Capabilities.EnergyStorage.ITEM,
                                (stack, context) -> new ItemEnergyStorage(
                                                stack,
                                                net.nicotfpn.alientech.Config.PHARAOH_SWORD_CAPACITY.get(),
                                                PharaohSwordItem.MAX_TRANSFER,
                                                PharaohSwordItem.MAX_TRANSFER),
                                ModItems.PHARAOH_SWORD.get());

                // === Phase 3: Decay Chamber ===

                // Decay Chamber Controller - Entropy output
                event.registerBlockEntity(
                                net.nicotfpn.alientech.entropy.ModCapabilities.ENTROPY,
                                ModBlockEntities.DECAY_CHAMBER_CONTROLLER_BE.get(),
                                (be, context) -> be.getEntropyStorage());

                // Decay Chamber Controller - Item Handler
                event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                ModBlockEntities.DECAY_CHAMBER_CONTROLLER_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedItemHandler(context);
                                        }
                                        return be.getOutputInventory();
                                });

                // === Phase 4: Entropy Reservoir ===
                // Entropy Reservoir - expose entropy capability for cables
                event.registerBlockEntity(
                                net.nicotfpn.alientech.entropy.ModCapabilities.ENTROPY,
                                ModBlockEntities.ENTROPY_RESERVOIR_BE.get(),
                                (be, context) -> {
                                        if (be instanceof net.nicotfpn.alientech.machine.entropy.EntropyReservoirBlockEntity reservoir)
                                                return reservoir.getEntropyHandler();
                                        return null;
                                });

                // === Phase 5: Quantum Vacuum Turbine ===

                // QVT - Energy Storage (output)
                event.registerBlockEntity(
                                Capabilities.EnergyStorage.BLOCK,
                                ModBlockEntities.QUANTUM_VACUUM_TURBINE_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedEnergyStorage(context);
                                        }
                                        return be.getEnergyStorage();
                                });

                // QVT - Item Handler (fuel input)
                event.registerBlockEntity(
                                Capabilities.ItemHandler.BLOCK,
                                ModBlockEntities.QUANTUM_VACUUM_TURBINE_BE.get(),
                                (be, context) -> {
                                        if (be instanceof AlienMachineBlockEntity ambe && context != null) {
                                                return ambe.getSidedItemHandler(context);
                                        }
                                        return be.getFuelInventory();
                                });

                // === Phase 9: Evolution Chamber ===

                // Evolution Chamber - Entropy input (consumes entropy)
                event.registerBlockEntity(
                                net.nicotfpn.alientech.entropy.ModCapabilities.ENTROPY,
                                ModBlockEntities.EVOLUTION_CHAMBER_BE.get(),
                                (be, context) -> {
                                        if (be instanceof net.nicotfpn.alientech.machine.evolution.EvolutionChamberBlockEntity chamber) {
                                                return chamber.getEntropyHandler();
                                        }
                                        return null;
                                });
                // Primal Catalyst - expose entropy capability (local buffer)
                event.registerBlockEntity(
                                net.nicotfpn.alientech.entropy.ModCapabilities.ENTROPY,
                                ModBlockEntities.PRIMAL_CATALYST_BE.get(),
                                (be, context) -> {
                                        if (be instanceof PrimalCatalystBlockEntity pc)
                                                return pc.getEntropyHandler();
                                        return null;
                                });
        }
}
