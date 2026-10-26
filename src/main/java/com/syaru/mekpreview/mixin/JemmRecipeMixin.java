package com.syaru.mekpreview.mixin;

import com.syaru.mekpreview.jemm.JemmRecipeView;
import com.syaru.mekpreview.jemm.JemmPreviewWidget;
import com.syaru.mekpreview.jemm.JemmCategoryPlugin;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.jemi.JemiRecipe;
import giselle.jei_mekanism_multiblocks.client.emi.EMIMultiblockRecipe;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Keep JEMM's category, recipe identity, costs and invalidation; replace only the presentation. */
@Mixin(value = EMIMultiblockRecipe.class, remap = false)
public abstract class JemmRecipeMixin {
    @Shadow @Final private JemiRecipe<?> original;
    @Unique private boolean mekpreview$supported() {
        return original.recipe instanceof MultiblockWidget model && com.syaru.mekpreview.jemm.JemmIntegration.supports(model);
    }
    @Inject(method = "getCategory", at = @At("HEAD"), cancellable = true)
    private void mekpreview$category(CallbackInfoReturnable<EmiRecipeCategory> cir) {
        if (mekpreview$supported()) cir.setReturnValue(JemmCategoryPlugin.CATEGORY);
    }
    @Inject(method = "getDisplayWidth", at = @At("HEAD"), cancellable = true)
    private void mekpreview$width(CallbackInfoReturnable<Integer> cir) {
        if (mekpreview$supported()) cir.setReturnValue(JemmPreviewWidget.WIDTH);
    }
    @Inject(method = "getDisplayHeight", at = @At("HEAD"), cancellable = true)
    private void mekpreview$height(CallbackInfoReturnable<Integer> cir) {
        if (mekpreview$supported()) cir.setReturnValue(JemmPreviewWidget.HEIGHT);
    }
    @Inject(method = "addWidgets", at = @At("HEAD"), cancellable = true)
    private void mekpreview$widgets(WidgetHolder widgets, CallbackInfo ci) {
        if (mekpreview$supported()) {
            new JemmRecipeView(original, (MultiblockWidget) original.recipe).addWidgets(widgets);
            ci.cancel();
        }
    }
}
