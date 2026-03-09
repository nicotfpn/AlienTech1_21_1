package net.nicotfpn.alientech.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nicotfpn.alientech.AlienTech;

public record ClientboundSideConfigPacket(BlockPos machinePos, CompoundTag fullSideConfigNBT)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundSideConfigPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AlienTech.MOD_ID, "clientbound_side_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSideConfigPacket> STREAM_CODEC = StreamCodec
            .composite(
                    BlockPos.STREAM_CODEC, ClientboundSideConfigPacket::machinePos,
                    ByteBufCodecs.COMPOUND_TAG, ClientboundSideConfigPacket::fullSideConfigNBT,
                    ClientboundSideConfigPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
