package com.syaru.mekpreview.mixin;

import com.syaru.mekpreview.jemm.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.jemi.JemiRecipe;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** The same optional adapters work when real JEI/JEMI is used instead of TMRV. */
@Mixin(value = JemiRecipe.class, remap = false)
public abstract class JemiRecipeMixin implements JemmRecipeAccess {
    @Shadow public Object recipe;
    @Shadow public mezz.jei.api.recipe.category.IRecipeCategory<?> category;
    @Override public String mekpreview$title() { return category.getTitle().getString(); }
    @Override public Object mekpreview$source() { return recipe; }
    @Unique private boolean mekpreview$resolved;
    @Unique private MultiblockWidget mekpreview$adapted;
    @Override public MultiblockWidget mekpreview$model() {
        if (!mekpreview$resolved) {
            mekpreview$resolved = true; mekpreview$adapted = JemmIntegration.adapt(recipe);
        }
        return mekpreview$adapted;
    }
    @Inject(method = "getCategory", at = @At("HEAD"), cancellable = true)
    private void mekpreview$category(CallbackInfoReturnable<EmiRecipeCategory> cir) {
        if (mekpreview$model() != null) cir.setReturnValue(JemmCategoryPlugin.CATEGORY);
    }
    @Inject(method = "getDisplayWidth", at = @At("HEAD"), cancellable = true)
    private void mekpreview$width(CallbackInfoReturnable<Integer> cir) {
        if (mekpreview$model() != null) cir.setReturnValue(JemmPreviewWidget.WIDTH);
    }
    @Inject(method = "getDisplayHeight", at = @At("HEAD"), cancellable = true)
    private void mekpreview$height(CallbackInfoReturnable<Integer> cir) {
        if (mekpreview$model() != null) cir.setReturnValue(JemmPreviewWidget.HEIGHT);
    }
    @Inject(method = "addWidgets", at = @At("HEAD"), cancellable = true)
    private void mekpreview$widgets(WidgetHolder widgets, CallbackInfo ci) {
        var model = mekpreview$model();
        if (model != null) {
            new JemmRecipeView((EmiRecipe) (Object) this, model).addWidgets(widgets);
            ci.cancel();
        }
    }
    @Inject(method = "getDisplayHeight", at = @At("RETURN"), cancellable = true)
    private void mekpreview$linkHeight(CallbackInfoReturnable<Integer> cir) {
        if (mekpreview$model() == null && NovaRecipeLink.hasFooter((EmiRecipe) (Object) this)) cir.setReturnValue(cir.getReturnValue() + 18);
    }
    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void mekpreview$link(WidgetHolder widgets, CallbackInfo ci) {
        if (mekpreview$model() == null) NovaRecipeLink.addButton(widgets, (EmiRecipe) (Object) this);
    }
}
