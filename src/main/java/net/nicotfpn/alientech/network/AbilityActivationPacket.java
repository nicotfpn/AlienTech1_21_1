package net.nicotfpn.alientech.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.AlienTech;

/**
 * Packet sent from client to server to activate an evolution ability.
 */
public record AbilityActivationPacket(ResourceLocation abilityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilityActivationPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "ability_activate"));

    public static final StreamCodec<FriendlyByteBuf, AbilityActivationPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            AbilityActivationPacket::abilityId,
            AbilityActivationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
