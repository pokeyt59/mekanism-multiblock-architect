package com.syaru.mekpreview.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** MTE also skips JEI recipes whenever EMI is loaded; import its actual models instead. */
@Pseudo
@Mixin(targets = "morethermalevaporation.client.jei.MoreThermalEvaporationJEIHelper", remap = false)
public abstract class MtePluginMixin {
    @Redirect(method = "registerRecipes", at = @At(value = "FIELD",
            target = "Lgiselle/jei_mekanism_multiblocks/common/JEI_MekanismMultiblocks;EMILoaded:Z", opcode = 178))
    private static boolean mekpreview$importMteRecipes() { return false; }
}
