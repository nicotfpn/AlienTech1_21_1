package net.nicotfpn.alientech.machine.core;

import net.nicotfpn.alientech.block.entity.base.AbstractMachineBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Static helper for registering NeoForge capabilities for machines.
 * Uses the NeoForge 1.21.1 capability system (RegisterCapabilitiesEvent, NOT
 * LazyOptional).
 *
 * Registers both ITEM_HANDLER and ENERGY capabilities by delegating to the
 * machine's composed components.
 */
public final class MachineCapabilities {

    private MachineCapabilities() {
        // Static utility class
    }

    /**
     * Register both ItemHandler and EnergyStorage capabilities for a machine block
     * entity.
     * The block entity must extend AbstractMachineBlockEntity to expose its
     * components.
     *
     * @param event the capability registration event
     * @param type  the block entity type to register for
     * @param <T>   block entity type extending AbstractMachineBlockEntity
     */
    public static <T extends AbstractMachineBlockEntity> void registerMachine(
            RegisterCapabilitiesEvent event, BlockEntityType<T> type) {

        // Energy Storage capability
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, type,
                (be, side) -> be.getEnergy().getEnergyStorage());

        // Item Handler capability (sided access for automation)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, type,
                (be, side) -> be.getAutomation().getHandlerForSide(
                        side, be.getInventory(), be.getSlotAccessRules()));
    }
}
