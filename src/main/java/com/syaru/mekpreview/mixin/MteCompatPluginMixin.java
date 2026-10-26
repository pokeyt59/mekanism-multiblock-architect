package com.syaru.mekpreview.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "io.github.masyumero.morethermalevaporationcompat.client.jei.MTECompatJEI", remap = false)
public abstract class MteCompatPluginMixin {
    @Redirect(method = "registerRecipes", at = @At(value = "FIELD",
            target = "Lgiselle/jei_mekanism_multiblocks/common/JEI_MekanismMultiblocks;EMILoaded:Z", opcode = 178))
    private boolean mekpreview$importCompatRecipes() { return false; }
}
