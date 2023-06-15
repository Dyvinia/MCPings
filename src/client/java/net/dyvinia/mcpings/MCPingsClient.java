package net.dyvinia.mcpings;

import com.google.common.collect.Iterables;
import net.dyvinia.mcpings.config.MCPingsConfig;
import net.dyvinia.mcpings.render.PingHud;
import net.dyvinia.mcpings.util.DirectionalSoundInstance;
import net.dyvinia.mcpings.util.MathHelper;
import net.dyvinia.mcpings.util.PingData;
import net.dyvinia.mcpings.util.RayCasting;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MCPingsClient implements ClientModInitializer {
	public static final MCPingsConfig CONFIG = MCPingsConfig.createAndLoad();

	private static final String keysCategory = "mcpings.name";
	private static KeyBinding keyPing;

	public static List<PingData> pingList = new ArrayList<>();
	private static boolean queuePing = false;


	@Override
	public void onInitializeClient() {
		MCPings.LOGGER.info("Client Init");

		keyPing = KeyBindingHelper.registerKeyBinding(new KeyBinding("mcpings.key.mark-location", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, keysCategory));

		ClientPlayNetworking.registerGlobalReceiver(MCPings.S2C_PING, MCPingsClient::onReceivePing);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (keyPing.wasPressed()) {
				markLoc();
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				ClientPlayNetworking.send(MCPings.C2S_JOIN, PacketByteBufs.create()));

		HudRenderCallback.EVENT.register(new PingHud());
	}

	public static void markLoc() {
		queuePing = true;
	}

	private static void processPing(float tickDelta) {
		if (!queuePing) return;
		else queuePing = false;

		ClientPlayerEntity cameraEnt = (ClientPlayerEntity) MinecraftClient.getInstance().cameraEntity;
		HitResult hitResult = RayCasting.traceDirectional(
				cameraEnt.getRotationVec(tickDelta),
				tickDelta, 256, cameraEnt.isSneaking(), MCPingsClient.CONFIG.visualsNest.pingHitOnlySolid());

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return;
		String username = cameraEnt.getGameProfile().getName();
		String channel = MCPingsClient.CONFIG.pingChannel();
		PingData.Type pingType = PingData.Type.STANDARD;

		if (hitResult instanceof EntityHitResult entHit) {
			Entity ent = entHit.getEntity();
			if (ent instanceof PlayerEntity)
				pingType = PingData.Type.PLAYER;
			else if (ent instanceof Monster)
				pingType = PingData.Type.MONSTER;
			else if (ent instanceof Angerable)
				pingType = PingData.Type.ANGERABLE;
			else if (ent instanceof MobEntity)
				pingType = PingData.Type.FRIENDLY;
		}

		PacketByteBuf packet = PacketByteBufs.create(); // create packet

		packet.writeDouble(hitResult.getPos().x); // pos x
		packet.writeDouble(hitResult.getPos().y); // pos y
		packet.writeDouble(hitResult.getPos().z); // pos z

		packet.writeString(channel); // channel
		packet.writeString(username); // sender's username
		packet.writeInt(pingType.ordinal()); // ping type

		if (hitResult instanceof EntityHitResult) {
			packet.writeBoolean(true);
			packet.writeUuid(((EntityHitResult) hitResult).getEntity().getUuid()); // hit entity uuid
		}
		else packet.writeBoolean(false);

		ClientPlayNetworking.send(MCPings.C2S_PING, packet);
	}

	private static void onReceivePing(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		String currentChannel = MCPingsClient.CONFIG.pingChannel();

		Vec3d pingPos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());

		String pingChannel = buf.readString();
		if (!pingChannel.equals(currentChannel)) return;

		String pingSender = buf.readString();
		int pingTypeOrdinal = buf.readInt();

		UUID pingEntity = buf.readBoolean() ? buf.readUuid() : null;

		client.execute(() -> {
			pingList.add(new PingData(pingSender, PingData.Type.fromOrdinal(pingTypeOrdinal), pingPos, pingEntity, client.world.getTime()));

			client.getSoundManager().play(
					new DirectionalSoundInstance(
							SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
							SoundCategory.MASTER,
							MCPingsClient.CONFIG.audioNest.pingVolume() / 100f,
							1f,
							0,
							pingPos
					)
			);
		});
	}

	public static void onRenderWorld(MatrixStack stack, Matrix4f projectionMatrix, float tickDelta) {
		ClientWorld world =  MinecraftClient.getInstance().world;
		Matrix4f modelViewMatrix = stack.peek().getPositionMatrix();

		processPing(tickDelta);

		for (PingData ping : pingList) {
			if (ping.hitEntity != null) {
				Entity ent = Iterables.tryFind(world.getEntities(), entity -> entity.getUuid().equals(ping.hitEntity)).orNull();
				if (ent != null) {
					if (ent instanceof ItemEntity itemEnt) {
						ping.itemStack = itemEnt.getStack().copy();
					}
					ping.pos = ent.getLerpedPos(tickDelta).add(0.0, ent.getBoundingBox().getYLength(), 0.0);
				}
			}

			ping.screenPos = MathHelper.project3Dto2D(ping.pos, modelViewMatrix, projectionMatrix);
			ping.aliveTime = Math.toIntExact(world.getTime() - ping.spawnTime);
		}

		pingList.removeIf(p -> p.aliveTime > MCPingsClient.CONFIG.visualsNest.pingDuration() * 20);
	}
}