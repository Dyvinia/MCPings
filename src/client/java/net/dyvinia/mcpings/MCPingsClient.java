package net.dyvinia.mcpings;

import com.google.common.collect.Iterables;
import io.wispforest.owo.config.ui.ConfigScreen;
import net.dyvinia.mcpings.config.MCPingsConfig;
import net.dyvinia.mcpings.network.PingPayload;
import net.dyvinia.mcpings.render.PingHud;
import net.dyvinia.mcpings.util.DirectionalSoundInstance;
import net.dyvinia.mcpings.util.MathHelper;
import net.dyvinia.mcpings.util.PingData;
import net.dyvinia.mcpings.util.RayCasting;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
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
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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

	private final int COOLDOWN = 10;
	private static int cooldownCounter = 0;


	@Override
	public void onInitializeClient() {
		MCPings.LOGGER.info("Client Init");

		keyPing = KeyBindingHelper.registerKeyBinding(new KeyBinding("mcpings.key.mark-location", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, keysCategory));

		ClientPlayNetworking.registerGlobalReceiver(PingPayload.ID, MCPingsClient::onReceivePing);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if ((keyPing.wasPressed() || keyPing.isPressed()) && cooldownCounter >= COOLDOWN) {
				markLoc();
				cooldownCounter = 0;
			}
			else if (cooldownCounter < COOLDOWN) {
				cooldownCounter++;
			}
		});/*
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			PacketByteBuf packet = PacketByteBufs.create(); // create packet
			packet.writeString(MCPings.VERSION_STRING);
			ClientPlayNetworking.send(MCPings.C2S_JOIN, packet);
		});*/


		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommandManager.literal("mcpings").executes(context -> {
				context.getSource().sendFeedback(Text.literal("MCPings v" + FabricLoader.getInstance().getModContainer("mcpings")
						.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("1.0.0")));
				return 1; // idk what the return values mean
			}).then(ClientCommandManager.literal("config").executes(context -> {
				ConfigScreen screen = ConfigScreen.create(MCPingsClient.CONFIG, null);
				MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(screen));
				return 0;
			}))
		));

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
		UUID uuid = cameraEnt.getGameProfile().getId();
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

		PingPayload payload = new PingPayload(hitResult.getPos().toVector3f(), channel, username, uuid, pingType.ordinal());

		packet.writeDouble(hitResult.getPos().x); // pos x
		packet.writeDouble(hitResult.getPos().y); // pos y
		packet.writeDouble(hitResult.getPos().z); // pos z

		packet.writeString(channel); // channel
		packet.writeString(username); // sender's username
		packet.writeUuid(uuid); // sender's uuid
		packet.writeInt(pingType.ordinal()); // ping type

		if (hitResult instanceof EntityHitResult) {
			packet.writeBoolean(true);
			packet.writeUuid(((EntityHitResult) hitResult).getEntity().getUuid()); // hit entity uuid
		}
		else packet.writeBoolean(false);

		ClientPlayNetworking.send(payload);
	}

	private static void onReceivePing(PingPayload payload, ClientPlayNetworking.Context context) {
		String currentChannel = MCPingsClient.CONFIG.pingChannel();

		Vec3d pingPos = new Vec3d(payload.pos());

		String pingChannel = payload.channel();
		if (!pingChannel.equals(currentChannel)) return;

		// Max Ping Distance
		if (context.player().getPos().distanceTo(pingPos) > MCPingsClient.CONFIG.visualsNest.pingDistance())
			return;

		String pingSender = payload.username();
		UUID pingSenderId = payload.uuid();

		PlayerEntity pingSenderEnt = context.client().world.getPlayerByUuid(pingSenderId);
		if (pingSenderEnt != null && context.player().getPos().distanceTo(pingSenderEnt.getPos()) >= MCPingsClient.CONFIG.visualsNest.pingSourceDistance())
			return;

		int pingTypeOrdinal = payload.type();

		//UUID pingEntity = buf.readBoolean() ? buf.readUuid() : null;
		UUID pingEntity = null;

		context.client().execute(() -> {
			PingData ping = new PingData(pingSender, pingSenderId, PingData.Type.fromOrdinal(pingTypeOrdinal), pingPos, pingEntity, context.client().world.getTime());

			pingList.add(ping);
			playPingSound(context.client(), ping);
		});
	}

	public static void onRenderWorld(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta) {
		ClientWorld world =  MinecraftClient.getInstance().world;

		processPing(tickDelta);

		for (PingData ping : pingList) {
			if (ping.hitEntity != null) {
				Entity ent = Iterables.tryFind(world.getEntities(), entity -> entity.getUuid().equals(ping.hitEntity)).orNull();
				if (ent != null) {
					if (ent instanceof ItemEntity itemEnt) {
						ping.itemStack = itemEnt.getStack().copy();
					}
					ping.pos = ent.getLerpedPos(tickDelta).add(0.0, ent.getBoundingBox().getLengthY(), 0.0);
				}
			}

			ping.screenPos = MathHelper.project3Dto2D(ping.pos, modelViewMatrix, projectionMatrix);
			ping.aliveTime = Math.toIntExact(world.getTime() - ping.spawnTime);
		}

		pingList.removeIf(p -> p.aliveTime > MCPingsClient.CONFIG.visualsNest.pingDuration() * 20);
	}
	
	public static void playPingSound(MinecraftClient client, PingData ping) {
		client.getSoundManager().play(
				new DirectionalSoundInstance(
						SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
						SoundCategory.MASTER,
						MCPingsClient.CONFIG.audioNest.pingVolume() / 100f,
						1f,
						0,
						ping.pos
				)
		);
	}
}