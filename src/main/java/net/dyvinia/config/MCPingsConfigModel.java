package net.dyvinia.config;

import io.wispforest.owo.config.annotation.*;
import io.wispforest.owo.ui.core.Color;


@Modmenu(modId = "mcpings")
@Config(name = "mcpings-config", wrapperName = "MCPingsConfig")
public class MCPingsConfigModel {
    @SectionHeader("audio")
    @RangeConstraint(min = 0, max = 100)
    public int pingVolume = 100;
    @RangeConstraint(min = 0, max = 16)
    public int pingVolumeFalloff = 8;

    @SectionHeader("visuals")
    @RangeConstraint(min = 1, max = 16)
    public int pingDuration = 8;
    @RangeConstraint(min = 1, max = 16)
    public int pingMaxCount = 8;

    @SectionHeader("color")
    public Color pingStandardColor = Color.ofRgb(0xFFFFFF);
}
