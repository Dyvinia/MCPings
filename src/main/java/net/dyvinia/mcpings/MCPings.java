package net.dyvinia.mcpings;

import net.dyvinia.mcpings.config.MCPingsConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPings implements ModInitializer {
	public static final MCPingsConfig CONFIG = MCPingsConfig.createAndLoad();

	public static final Logger LOGGER = LoggerFactory.getLogger("mcpings");

	public static final Identifier C2S_PING = new Identifier("mcpings-c2s:ping");
	public static final Identifier S2C_PING = new Identifier("mcpings-s2c:ping");

	@Override
	public void onInitialize() {
		LOGGER.info("Server Init");

		ServerPlayNetworking.registerGlobalReceiver(C2S_PING, (server, player, handler, buf, responseSender) -> {
			PacketByteBuf packet = PacketByteBufs.copy(buf);

			for (ServerPlayerEntity p : PlayerLookup.world(player.getWorld())) {
				ServerPlayNetworking.send(p, S2C_PING, packet);
			}
		});
	}
}