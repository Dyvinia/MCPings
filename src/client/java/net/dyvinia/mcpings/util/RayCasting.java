package net.dyvinia.mcpings.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;
import java.util.function.Predicate;

public class RayCasting {

    public static EntityHitResult traceEntity(Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate) {
        double minDist = min.squaredDistanceTo(max);
        EntityHitResult minHitResult = null;

        for (Entity ent : entity.getWorld().getOtherEntities(entity, box, predicate)) {
            Box targetBoundingBox = ent.getBoundingBox().expand(ent.getTargetingMargin()).expand(0.25);
            Optional<Vec3d> hitPos = targetBoundingBox.raycast(min, max);

            if (hitPos.isEmpty()) continue;

            EntityHitResult hitResult = new EntityHitResult(ent, hitPos.get());
            double hitDist = min.squaredDistanceTo(hitResult.getPos());

            if (minDist > hitDist) {
                minDist = hitDist;
                minHitResult = hitResult;
            }
        }

        return minHitResult;
    }

    public static HitResult traceDirectional(Vec3d direction, float tickDelta, double maxDistance, boolean hitFluids, boolean hitOnlySolid) {
        Entity cameraEnt = MinecraftClient.getInstance().cameraEntity;

        if (cameraEnt == null || cameraEnt.getWorld() == null) {
            return null;
        }

        Vec3d rayStartVec = cameraEnt.getCameraPosVec(tickDelta);
        Vec3d rayEndVec = rayStartVec.add(direction.multiply(maxDistance));
        Box boundingBox = cameraEnt.getBoundingBox()
                .stretch(cameraEnt.getRotationVec(1.0f).multiply(maxDistance))
                .expand(1.0, 1.0, 1.0);

        BlockHitResult blockHitResult = cameraEnt.getWorld().raycast(
                new RaycastContext(
                        rayStartVec,
                        rayEndVec,
                        hitOnlySolid ? RaycastContext.ShapeType.VISUAL : RaycastContext.ShapeType.OUTLINE,
                        hitFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                        cameraEnt
                )
		);

        EntityHitResult entityHitResult = traceEntity(
                cameraEnt,
                rayStartVec,
                rayEndVec,
                boundingBox,
                targetEntity -> !targetEntity.isSpectator() &&
                        (targetEntity instanceof LivingEntity || targetEntity instanceof ItemEntity)
        );

        if (entityHitResult == null || rayStartVec.squaredDistanceTo(entityHitResult.getPos()) > rayStartVec.squaredDistanceTo(blockHitResult.getPos()))
            return blockHitResult;
        else
            return entityHitResult;
    }
}
