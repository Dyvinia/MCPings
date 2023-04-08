package net.dyvinia.mcpings.util;

import net.dyvinia.mcpings.MCPings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class DirectionalSoundInstance extends MovingSoundInstance {

    private Vec3d pos;

    public DirectionalSoundInstance(SoundEvent soundEvent, SoundCategory soundCategory, Float volume, Float pitch, long seed, Vec3d pos) {
        super(soundEvent, soundCategory, Random.create(seed));
        this.volume = volume;
        this.pitch = pitch;
        this.pos = pos;

        updateSoundPos();
    }

    @Override
    public void tick() {
        updateSoundPos();
    }

    private void updateSoundPos() {
        Vec3d playerPos = MinecraftClient.getInstance().player.getPos();

        Vec3d vecBetween = playerPos.relativize(this.pos);
        double mappedDistance = Math.min(vecBetween.length(), 64.0) / 64.0 * MCPings.CONFIG.pingVolumeFalloff();
        Vec3d soundDirection = vecBetween.normalize().multiply(mappedDistance);
        Vec3d soundPos = playerPos.add(soundDirection);

        this.x = soundPos.x;
        this.y = soundPos.y;
        this.z = soundPos.z;
    }
}
