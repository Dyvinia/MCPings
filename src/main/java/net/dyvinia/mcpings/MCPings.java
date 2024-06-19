package net.dyvinia.mcpings;

import net.dyvinia.mcpings.network.JoinPayload;
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

	public static final Identifier JOIN = Identifier.of("mcpings:join");
	public static final Identifier PING = Identifier.of("mcpings:ping");

	private final List<ServerPlayerEntity> moddedPlayers = new ArrayList<>();

	@Override
	public void onInitialize() {
		LOGGER.info("Server Init");

		PayloadTypeRegistry.playS2C().register(PingPayload.ID, PingPayload.CODEC);

		PayloadTypeRegistry.playC2S().register(PingPayload.ID, PingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(JoinPayload.ID, JoinPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PingPayload.ID, (payload, context) -> {
			// clean list
			moddedPlayers.removeIf(p -> !PlayerLookup.all(context.server()).contains(p));

			// send ping to modded players
			for (ServerPlayerEntity p : PlayerLookup.world(context.player().getServerWorld())) {
				if (moddedPlayers.contains(p)) {
					ServerPlayNetworking.send(p, payload);
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(JoinPayload.ID, (payload, context) -> moddedPlayers.add(context.player()));
	}
}