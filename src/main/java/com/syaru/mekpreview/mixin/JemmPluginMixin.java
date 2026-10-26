package com.syaru.mekpreview.mixin;

import giselle.jei_mekanism_multiblocks.common.JEI_MekanismMultiblocks;
import giselle.jei_mekanism_multiblocks.client.jei.JEI_MekanismMultiblocks_JeiPlugin;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** JEMM normally skips JEI recipe registration when EMI is installed.
 * TMRV replaces JEMI, so let TMRV import JEMM's ordinary JEI recipes instead. */
@Mixin(value = JEI_MekanismMultiblocks_JeiPlugin.class, remap = false)
public abstract class JemmPluginMixin {
    @Redirect(method = "registerRecipes", at = @At(value = "FIELD",
            target = "Lgiselle/jei_mekanism_multiblocks/common/JEI_MekanismMultiblocks;EMILoaded:Z", opcode = 178))
    private boolean mekpreview$useJemiRegistration() {
        return JEI_MekanismMultiblocks.EMILoaded && !ModList.get().isLoaded("toomanyrecipeviewers");
    }
}
