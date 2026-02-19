package net.nicotfpn.alientech.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.AlienTech;

/**
 * Packet to toggle battery charge/discharge mode.
 */
public record BatteryModePacket(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BatteryModePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "battery_mode"));

    public static final StreamCodec<FriendlyByteBuf, BatteryModePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            BatteryModePacket::pos,
            BatteryModePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
