package com.syaru.mekpreview.servertest;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("mekpreview_server_test")
public final class ServerTestMod {
    public ServerTestMod() {
        for (String id : new String[]{"jei_mekanism_multiblocks", "jei", "ldlib", "emi", "toomanyrecipeviewers", "mekanism"})
            if (ModList.get().isLoaded(id)) throw new IllegalStateException("Standalone test unexpectedly loaded " + id);
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterGameTestsEvent e) -> e.register(PlacementTests.class));
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> {
            try {
                var tag = TagParser.parseTag("{size:[8,8,8],entities:[],blocks:[],palette:[{Name:\"minecraft:air\"}]}");
                event.getServer().getStructureManager().getOrCreate(new ResourceLocation("mekpreview_server_test", "empty"))
                        .load(event.getServer().registryAccess().lookupOrThrow(Registries.BLOCK), tag);
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new java.io.File("results.xml")));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }
}
