package net.nicotfpn.alientech.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.AlienTech;
import net.nicotfpn.alientech.network.sideconfig.CapabilityType;
import net.nicotfpn.alientech.network.sideconfig.IOSideMode;

public record ServerboundSideConfigPacket(
                BlockPos machinePos,
                Direction face,
                CapabilityType capType,
                IOSideMode newMode) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ServerboundSideConfigPacket> TYPE = new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "serverbound_side_config"));

        public static final StreamCodec<FriendlyByteBuf, ServerboundSideConfigPacket> STREAM_CODEC = StreamCodec
                        .composite(
                                        BlockPos.STREAM_CODEC, ServerboundSideConfigPacket::machinePos,
                                        Direction.STREAM_CODEC, ServerboundSideConfigPacket::face,
                                        ByteBufCodecs.VAR_INT.map(i -> CapabilityType.values()[i],
                                                        CapabilityType::ordinal),
                                        ServerboundSideConfigPacket::capType,
                                        ByteBufCodecs.VAR_INT.map(i -> IOSideMode.values()[i], IOSideMode::ordinal),
                                        ServerboundSideConfigPacket::newMode,
                                        ServerboundSideConfigPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }
}
