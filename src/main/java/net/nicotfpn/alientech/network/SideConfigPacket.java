package net.nicotfpn.alientech.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.block.entity.SideConfiguration;

/**
 * Packet to sync side configuration from client to server.
 * Sent when player clicks on a side button in the config screen.
 */
public record SideConfigPacket(
        BlockPos pos,
        int relativeSideOrdinal,
        boolean reverseDirection) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SideConfigPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "side_config"));

    public static final StreamCodec<FriendlyByteBuf, SideConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SideConfigPacket::pos,
            ByteBufCodecs.INT,
            SideConfigPacket::relativeSideOrdinal,
            ByteBufCodecs.BOOL,
            SideConfigPacket::reverseDirection,
            SideConfigPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Gets the relative side from the ordinal value.
     */
    public SideConfiguration.RelativeSide getRelativeSide() {
        return SideConfiguration.RelativeSide.values()[relativeSideOrdinal];
    }
}
