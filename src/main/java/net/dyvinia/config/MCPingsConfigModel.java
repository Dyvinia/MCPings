package net.dyvinia.config;

import io.wispforest.owo.config.annotation.*;
import io.wispforest.owo.ui.core.Color;


@SuppressWarnings("unused")
@Modmenu(modId = "mcpings")
@Config(name = "mcpings-config", wrapperName = "MCPingsConfig")
public class MCPingsConfigModel {

    @SectionHeader("main")
    @RangeConstraint(min = 0, max = 100)
    public int pingVolume = 100;
    @RangeConstraint(min = 1, max = 15)
    public int pingVolumeFalloff = 10;

    @RangeConstraint(min = 1, max = 16)
    public int pingDuration = 8;

    public SizeMultiplier pingSizeMult = SizeMultiplier.ONE;

    public enum SizeMultiplier {
        ONE, TWO, THREE
    }

    public boolean pingShowUsername = true;
    public boolean pingHitOnlySolid = true;


    @SectionHeader("color")
    public Color pingStandardColor = Color.ofRgb(0xFFFFFF);
    public Color pingMonsterColor = Color.ofRgb(0xFF5050);
    public Color pingAngerableColor = Color.ofRgb(0xFFBB55);
    public Color pingFriendlyColor = Color.ofRgb(0x45FF45);
    public Color pingPlayerColor = Color.ofRgb(0x45B5FF);
}
