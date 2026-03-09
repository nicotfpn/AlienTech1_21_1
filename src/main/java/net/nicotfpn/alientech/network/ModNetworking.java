package net.nicotfpn.alientech.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.ui.sync.AlienContainerMenu;
import net.minecraft.world.entity.player.Player;

import net.nicotfpn.alientech.block.entity.ISideConfigurable;
import net.nicotfpn.alientech.network.packet.ClientboundSideConfigPacket;
import net.nicotfpn.alientech.network.packet.ServerboundSideConfigPacket;
import net.nicotfpn.alientech.machine.core.AlienMachineBlockEntity;
import net.nicotfpn.alientech.machine.core.component.SideConfigComponent;
import net.minecraft.nbt.CompoundTag;

/**
 * Registers and handles all network packets for AlienTech mod.
 */
@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AlienTech.MOD_ID);

        // Sync Data Packet (server -> client GUI sync)
        registrar.playToClient(
                SyncDataPacket.TYPE,
                SyncDataPacket.STREAM_CODEC,
                ModNetworking::handleSyncDataClient);

        // Side config packet (client -> server)
        registrar.playToServer(
                SideConfigPacket.TYPE,
                SideConfigPacket.STREAM_CODEC,
                ModNetworking::handleSideConfigServer);

        // Serverbound SideConfig (Wave 3)
        registrar.playToServer(
                ServerboundSideConfigPacket.TYPE,
                ServerboundSideConfigPacket.STREAM_CODEC,
                ModNetworking::handleServerboundSideConfig);

        // Clientbound SideConfig (Wave 3)
        registrar.playToClient(
                ClientboundSideConfigPacket.TYPE,
                ClientboundSideConfigPacket.STREAM_CODEC,
                ModNetworking::handleClientboundSideConfig);

        // Ability activation packet (client -> server)
        registrar.playToServer(
                AbilityActivationPacket.TYPE,
                AbilityActivationPacket.STREAM_CODEC,
                ModNetworking::handleAbilityActivationServer);

        // Battery mode toggle packet (client -> server)
        /*
         * registrar.playToServer(
         * BatteryModePacket.TYPE,
         * BatteryModePacket.STREAM_CODEC,
         * ModNetworking::handleBatteryModeServer);
         */
    }

    /**
     * Handles side configuration packet on the server.
     */
    private static void handleSideConfigServer(SideConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            if (player.blockPosition().distSqr(packet.pos()) > 64) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
            if (blockEntity instanceof ISideConfigurable configurable) {
                if (packet.reverseDirection()) {
                    configurable.cycleModeReverse(packet.getRelativeSide());
                } else {
                    configurable.cycleMode(packet.getRelativeSide());
                }
            }
        });
    }

    private static void handleServerboundSideConfig(ServerboundSideConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            if (player.blockPosition().distSqr(packet.machinePos()) > 64) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(packet.machinePos());
            if (blockEntity instanceof AlienMachineBlockEntity machine) {
                if (machine.hasComponent(SideConfigComponent.class)) {
                    SideConfigComponent config = machine.getComponent(SideConfigComponent.class);
                    config.setMode(packet.face(), packet.capType(), packet.newMode());
                    machine.setChanged();

                    // Sincronizar de volta para o cliente
                    CompoundTag tag = new CompoundTag();
                    config.save(tag, player.level().registryAccess());
                    context.reply(new ClientboundSideConfigPacket(packet.machinePos(), tag));
                }
            }
        });
    }

    private static void handleClientboundSideConfig(ClientboundSideConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.world.entity.player.Player player = context.player();
            if (player != null && player.level() != null) {
                BlockEntity blockEntity = player.level().getBlockEntity(packet.machinePos());
                if (blockEntity instanceof AlienMachineBlockEntity machine) {
                    if (machine.hasComponent(SideConfigComponent.class)) {
                        machine.getComponent(SideConfigComponent.class).load(packet.fullSideConfigNBT(),
                                player.level().registryAccess());
                    }
                }
            }
        });
    }

    /**
     * Handles ability activation packet on the server.
     * <p>
     * Validates all inputs server-side. Client cannot bypass checks.
     */
    private static void handleAbilityActivationServer(AbilityActivationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Validate player is actually a ServerPlayer
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // Validate ability ID is from our mod
            if (packet.abilityId() == null) {
                return;
            }

            // Only accept abilities from our namespace
            if (!packet.abilityId().getNamespace().equals(net.nicotfpn.alientech.AlienTech.MOD_ID)) {
                return;
            }

            // Look up ability
            net.nicotfpn.alientech.evolution.ability.IEvolutionAbility ability = net.nicotfpn.alientech.evolution.ability.AbilityRegistry
                    .get(packet.abilityId());

            if (ability == null) {
                // Invalid ability ID - ignore (client may have outdated registry)
                return;
            }

            // Activate ability (all validation happens inside activate())
            ability.activate(player);
        });
    }

    /**
     * Handles battery mode toggle packet on the server.
     * (Deprecated/Removed for Energy Cube style)
     */
    /*
     * private static void handleBatteryModeServer(BatteryModePacket packet,
     * IPayloadContext context) {
     * context.enqueueWork(() -> {
     * ServerPlayer player = (ServerPlayer) context.player();
     * 
     * if (player.blockPosition().distSqr(packet.pos()) > 64) {
     * return;
     * }
     * 
     * BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
     * if (blockEntity instanceof AncientBatteryBlockEntity battery) {
     * // battery.toggleMode(); // Method removed
     * }
     * }
     * 
     * /**
     * Handles receiving compressed GUI sync payloads.
     */
    private static void handleSyncDataClient(SyncDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null && player.containerMenu instanceof AlienContainerMenu alienMenu) {
                if (alienMenu.containerId == packet.containerId()) {
                    alienMenu.handleSync(packet.payloads());
                }
            }
        });
    }
}
