package net.dyvinia.mcpings.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dyvinia.mcpings.MCPings;
import net.dyvinia.mcpings.MCPingsClient;
import net.dyvinia.mcpings.util.PingData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

public class PingHud implements HudRenderCallback {

    private static final Identifier PING_STANDARD = new Identifier("mcpings", "textures/ping_standard.png");

    @Override
    public void onHudRender(MatrixStack stack, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        double uiScale = client.getWindow().getScaleFactor();
        Vec3d cameraPosVec = client.player.getCameraPosVec(tickDelta);

        int pingColor = MCPings.CONFIG.pingStandardColor().argb();
        int shadowBlack = ColorHelper.Argb.getArgb(135, 0, 0, 0);
        int scaleDist = 10;

        for (PingData ping : MCPingsClient.pingList) {
            stack.push();

            double distance = cameraPosVec.distanceTo(ping.pos);
            Vector4f screenPos = screenPosWindowed(ping.screenPos, 16, client.getWindow());
            boolean onScreen = screenPos == ping.screenPos;

            stack.translate(screenPos.x/uiScale, screenPos.y/uiScale, 0); // stack to ping center
            stack.scale((float) (2/uiScale), (float) (2/uiScale), 1); // constant scale

            // scale if ping is far and onscreen
            if (distance > scaleDist && onScreen) stack.scale(0.5f, 0.5f, 1);

            // draw ping icon
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, PING_STANDARD);
            DrawableHelper.drawTexture(stack, -4, -2, 0, 0, 8, 8, 8, 8);

            // skip drawing text if ping not on screen
            if (!onScreen) {
                stack.pop();
                continue;
            }

            // distance text
            String distanceText = String.format("%.1fm", distance);
            int distanceTextWidth = client.textRenderer.getWidth(distanceText);

            stack.translate(-distanceTextWidth/2, -12, 0);
            DrawableHelper.fill(stack, -2, -2, client.textRenderer.getWidth(distanceText) + 1, client.textRenderer.fontHeight, shadowBlack);
            client.textRenderer.drawWithShadow(stack, distanceText, 0f, 0f, pingColor);
            stack.translate(distanceTextWidth/2, 0, 0); // recenter x

            // username text
            String nameText = ping.senderName;
            int nameTextWidth = client.textRenderer.getWidth(nameText);

            stack.scale(0.5f, 0.5f, 1f);
            if (distance > scaleDist) stack.scale(2, 2, 1);

            stack.translate(-nameTextWidth/2, -14, 0);
            DrawableHelper.fill(stack, -2, -2, client.textRenderer.getWidth(nameText) + 1, client.textRenderer.fontHeight, shadowBlack);
            client.textRenderer.drawWithShadow(stack, nameText, 0f, 0f, pingColor);
            stack.translate(nameTextWidth/2, 0, 0); // recenter x

            // end
            stack.pop();
        }
    }

    private Vector4f screenPosWindowed(Vector4f screenPos, int margin, Window wnd) {
        Vector4f newScreenPos = screenPos;
        int width = wnd.getWidth();
        int height = wnd.getHeight();

        if (newScreenPos.w < 0) newScreenPos = new Vector4f(width - newScreenPos.x, height - margin, newScreenPos.z, -newScreenPos.w);
        if (newScreenPos.x > width - margin) newScreenPos = new Vector4f(width - margin, newScreenPos.y, newScreenPos.z, newScreenPos.w);
        else if (newScreenPos.x < margin) newScreenPos = new Vector4f(margin, newScreenPos.y, newScreenPos.z, newScreenPos.w);
        if (newScreenPos.y > height - margin) newScreenPos = new Vector4f(newScreenPos.x, height - margin, newScreenPos.z, newScreenPos.w);
        else if (newScreenPos.y < margin) newScreenPos = new Vector4f(newScreenPos.x, margin, newScreenPos.z, newScreenPos.w);

        return newScreenPos;
    }
}
