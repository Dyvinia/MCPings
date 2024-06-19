package net.dyvinia.mcpings;

import net.dyvinia.mcpings.network.PingPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MCPings implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("mcpings");

	public static final String VERSION_STRING = FabricLoader.getInstance().getModContainer("mcpings")
			.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("1.0.0");

	public static final Identifier C2S_JOIN = Identifier.of("mcpings-c2s:join");
	public static final Identifier PING = Identifier.of("mcpings:ping");
	//public static final Identifier S2C_PING = Identifier.of("mcpings-s2c:ping");

	private final List<ServerPlayerEntity> moddedPlayers = new ArrayList<>();

	@Override
	public void onInitialize() {
		LOGGER.info("Server Init");

		PayloadTypeRegistry.playS2C().register(PingPayload.ID, PingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);

		//c2s
		ServerPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
			//PacketByteBuf packet = PacketByteBufs.copy(buf);
			for (ServerPlayerEntity p : PlayerLookup.world(context.player().getServerWorld())) {
				ServerPlayNetworking.send(p, payload);
			}
		});
	}
}