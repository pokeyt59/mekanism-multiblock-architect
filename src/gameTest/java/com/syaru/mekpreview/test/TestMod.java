package com.syaru.mekpreview.test;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("mekpreview_test")
public final class TestMod {
    public TestMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterGameTestsEvent e) -> e.register(FormationTests.class));
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> {
            try {
                var tag = TagParser.parseTag("{size:[22,22,22],entities:[],blocks:[],palette:[{Name:\"minecraft:air\"}]}");
                event.getServer().getStructureManager().getOrCreate(new ResourceLocation("mekpreview_test", "empty"))
                        .load(event.getServer().registryAccess().lookupOrThrow(Registries.BLOCK), tag);
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new java.io.File("results.xml")));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }
}
