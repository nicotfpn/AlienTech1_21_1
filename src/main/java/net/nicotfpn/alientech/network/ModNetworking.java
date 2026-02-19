package net.nicotfpn.alientech.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.nicotfpn.alientech.AlienTech;

import net.nicotfpn.alientech.block.entity.ISideConfigurable;

/**
 * Registers and handles all network packets for AlienTech mod.
 */
@EventBusSubscriber(modid = AlienTech.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AlienTech.MOD_ID);

        // Side config packet (client -> server)
        registrar.playToServer(
                SideConfigPacket.TYPE,
                SideConfigPacket.STREAM_CODEC,
                ModNetworking::handleSideConfigServer);

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
            net.nicotfpn.alientech.evolution.ability.IEvolutionAbility ability = 
                net.nicotfpn.alientech.evolution.ability.AbilityRegistry.get(packet.abilityId());
            
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
     * });
     * }
     */
}
