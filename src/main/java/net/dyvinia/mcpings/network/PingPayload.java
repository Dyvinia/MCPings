package net.dyvinia.mcpings.network;

import net.dyvinia.mcpings.MCPings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.UUID;

public record PingPayload(
        Vector3f pos,
        String channel,
        String username,
        UUID uuid,
        int type,
        UUID hitEntity
) implements CustomPayload {
    public static final CustomPayload.Id<PingPayload> ID = new CustomPayload.Id<>(MCPings.PING);

    public static final PacketCodec<RegistryByteBuf, PingPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VECTOR3F, PingPayload::pos,
            PacketCodecs.STRING, PingPayload::channel,
            PacketCodecs.STRING, PingPayload::username,
            Uuids.PACKET_CODEC, PingPayload::uuid,
            PacketCodecs.INTEGER, PingPayload::type,
            Uuids.PACKET_CODEC, PingPayload::hitEntity,
            PingPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
