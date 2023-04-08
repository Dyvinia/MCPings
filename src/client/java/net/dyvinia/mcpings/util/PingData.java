package net.dyvinia.mcpings.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

import java.util.UUID;

/*
public record PingData(
        String senderName,
        String pingType,
        Vec3d pos,
        Integer spawnTime,
        Integer aliveTime,
        UUID hitEntity

) {}*/


public class PingData {
    public String senderName;
    public String pingType;
    public Vec3d pos;
    public Vector4f screenPos;
    public UUID hitEntity;
    public ItemStack itemStack;
    public Integer spawnTime;
    public Integer aliveTime;


    public PingData(String senderName, String pingType, Vec3d pos, UUID hitEntity, long spawnTime) {
        this.senderName = senderName;
        this.pingType = pingType;
        this.pos = pos;
        this.hitEntity = hitEntity;
        this.spawnTime = (int) spawnTime;
    }
}