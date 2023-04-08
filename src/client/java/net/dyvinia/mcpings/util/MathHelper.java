package net.dyvinia.mcpings.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;


public class MathHelper {
    public static Vector4f project3Dto2D(Vec3d pos3D, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        Vec3d in3D = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().negate().add(pos3D);
        Window wnd = MinecraftClient.getInstance().getWindow();

        Quaternionf quaternion = new Quaternionf(in3D.x, in3D.y, in3D.z, 1f);

        Quaternionf result = multiply(projectionMatrix, multiply(modelViewMatrix, quaternion));

        Quaternionf screenCoords = toScreen(result);

        float x = screenCoords.x * wnd.getWidth();
        float y = screenCoords.y * wnd.getHeight();

        if (Float.isInfinite(x) || Float.isInfinite(y)) return null;

        return new Vector4f(
                Math.round(x),
                Math.round(wnd.getHeight() - y),
                screenCoords.z,
                1f / (screenCoords.w * 2f));
    }

    public static Quaternionf multiply(Matrix4f m, Quaternionf q) {
        return new Quaternionf(
                m.m00() * q.x + m.m10() * q.y + m.m20() * q.z + m.m30() * q.w,
                m.m01() * q.x + m.m11() * q.y + m.m21() * q.z + m.m31() * q.w,
                m.m02() * q.x + m.m12() * q.y + m.m22() * q.z + m.m32() * q.w,
                m.m03() * q.x + m.m13() * q.y + m.m23() * q.z + m.m33() * q.w
        );
    }

    public static Quaternionf toScreen(Quaternionf q) {
        var newW = 1f / q.w * 0.5f;

        return new Quaternionf(
                q.x * newW + 0.5f,
                q.y * newW + 0.5f,
                q.z * newW + 0.5f,
                newW
        );
    }
}
