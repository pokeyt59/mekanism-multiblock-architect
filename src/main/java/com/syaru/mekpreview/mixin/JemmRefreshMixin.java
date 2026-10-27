package com.syaru.mekpreview.mixin;

import com.syaru.mekpreview.jei.MekaPlanJeiPlugin;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After every settings change JEMM makes JEI rebuild the whole recipe page so its cost slots refresh.
 * Planner pages show materials in their own panel, so that rebuild is skipped for designs MekaPlan hosts.
 * JEMM still recalculates, saves and notifies its own listeners as usual.
 */
@Mixin(value = MultiblockWidget.class, remap = false)
public abstract class JemmRefreshMixin {
    @Inject(method = "notifyStateChange", at = @At("HEAD"), cancellable = true)
    private void mekpreview$keepPlannerPage(CallbackInfo ci) {
        if (MekaPlanJeiPlugin.hosts((MultiblockWidget) (Object) this)) ci.cancel();
    }
}
