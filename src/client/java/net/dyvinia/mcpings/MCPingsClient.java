package net.dyvinia.mcpings;

import com.google.common.collect.Iterables;
import net.dyvinia.mcpings.render.PingHud;
import net.dyvinia.mcpings.util.DirectionalSoundInstance;
import net.dyvinia.mcpings.util.MathHelper;
import net.dyvinia.mcpings.util.PingData;
import net.dyvinia.mcpings.util.RayCasting;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
	private static final String keysCategory = "mcpings.name";

	private static KeyBinding keyPing;

	public static List<PingData> pingList = new ArrayList<>();
	private static boolean queuePing = false;


	@Override
	public void onInitializeClient() {
		MCPings.LOGGER.info("Client Init");

		keyPing = KeyBindingHelper.registerKeyBinding(new KeyBinding("mcpings.key.mark-location", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_3, keysCategory));

		ClientPlayNetworking.registerGlobalReceiver(MCPings.S2C_PING_LOC, MCPingsClient::onReceivePing);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (keyPing.wasPressed()) {
				markLoc();
			}
		});

		HudRenderCallback.EVENT.register(new PingHud());
	}

	public static void markLoc() {
		queuePing = true;
	}

	private static void processPing(float tickDelta) {
		if (!queuePing) return;
		else queuePing = false;

		ClientPlayerEntity cameraEnt = (ClientPlayerEntity) MinecraftClient.getInstance().cameraEntity;
		HitResult hitResult = RayCasting.traceDirectional(cameraEnt.getRotationVec(tickDelta), tickDelta, 256, cameraEnt.isSneaking());
		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return;
		String username = cameraEnt.getGameProfile().getName();
		String channel = "";

		PacketByteBuf packet = PacketByteBufs.create(); // create packet

		packet.writeString(channel); // channel
		packet.writeString(username); // sender's username
		packet.writeString(""); // ping type

		packet.writeDouble(hitResult.getPos().x); // pos x
		packet.writeDouble(hitResult.getPos().y); // pos y
		packet.writeDouble(hitResult.getPos().z); // pos z

		if (hitResult.getType() == HitResult.Type.ENTITY) {
			packet.writeBoolean(true);
			packet.writeUuid(((EntityHitResult) hitResult).getEntity().getUuid()); // hit entity uuid
		}
		else packet.writeBoolean(false);

		ClientPlayNetworking.send(MCPings.C2S_PING_LOC, packet);
	}

	private static void onReceivePing(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		String currentChannel = "";

		String pingChannel = buf.readString();
		String pingSender = buf.readString();
		String pingType = buf.readString();

		Vec3d pingPos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());

		UUID pingEntity = buf.readBoolean() ? buf.readUuid() : null;

		client.execute(() -> {
			pingList.add(new PingData(pingSender, pingType, pingPos, pingEntity, client.world.getTime()));

			client.getSoundManager().play(
					new DirectionalSoundInstance(
							SoundEvents.BLOCK_ANVIL_LAND,
							SoundCategory.MASTER,
							1f,
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

		pingList.removeIf(p -> p.aliveTime > 8 * 20);
	}
}