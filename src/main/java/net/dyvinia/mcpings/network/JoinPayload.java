package net.dyvinia.mcpings.network;

import net.dyvinia.mcpings.MCPings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record JoinPayload(String version) implements CustomPayload {
    public static final CustomPayload.Id<JoinPayload> ID = new CustomPayload.Id<>(MCPings.JOIN);

    public static final PacketCodec<RegistryByteBuf, JoinPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, JoinPayload::version,
            JoinPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
