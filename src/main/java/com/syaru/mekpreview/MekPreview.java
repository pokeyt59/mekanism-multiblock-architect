package com.syaru.mekpreview;

import net.minecraftforge.fml.common.Mod;

@Mod(MekPreview.MOD_ID)
public final class MekPreview {
    public static final String MOD_ID = "mekpreview";
    public MekPreview() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                ClientOptions.SPEC, "mekpreview-client.toml");
        com.syaru.mekpreview.build.BuildNetwork.init();
    }
}
