package com.syaru.mekpreview.jemm;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipe;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import net.minecraft.resources.ResourceLocation;

/** A view adapter only; this is never registered as a second EMI recipe. */
public final class JemmRecipeView extends ModularEmiRecipe<JemmPreviewWidget> {
    private final EmiRecipe original;
    public JemmRecipeView(EmiRecipe original, MultiblockWidget model) {
        super(() -> new JemmPreviewWidget(model, JemmIntegration.title(original, model)));
        this.original = original;
    }
    @Override public EmiRecipeCategory getCategory() { return JemmCategoryPlugin.CATEGORY; }
    @Override public ResourceLocation getId() { return original.getId(); }
    @Override public boolean supportsRecipeTree() { return false; }
}
