package net.nicotfpn.alientech.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncDataPacket(int containerId, List<SyncPayload> payloads) implements CustomPacketPayload {

    public static final Type<SyncDataPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("alientech", "sync_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncDataPacket> STREAM_CODEC = StreamCodec.ofMember(
            SyncDataPacket::write, SyncDataPacket::new);

    public SyncDataPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), readPayloads(buffer));
    }

    private static List<SyncPayload> readPayloads(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<SyncPayload> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new SyncPayload(buffer.readVarInt(), buffer.readVarLong()));
        }
        return list;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        // Critical AAA feature: Explicit Index Count Header limits loop allocation
        buffer.writeVarInt(payloads.size());

        for (SyncPayload payload : payloads) {
            buffer.writeVarInt(payload.index());
            buffer.writeVarLong(payload.value()); // Heavy VarLong compression
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record SyncPayload(int index, long value) {
    }
}
