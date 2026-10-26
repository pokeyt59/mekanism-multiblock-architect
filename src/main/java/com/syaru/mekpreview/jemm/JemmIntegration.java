package com.syaru.mekpreview.jemm;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import giselle.jei_mekanism_multiblocks.client.emi.EMIMultiblockRecipe;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import com.syaru.mekpreview.structure.StructureKind;

public final class JemmIntegration {
    private JemmIntegration() {}
    public static MultiblockWidget model(EmiRecipe recipe) {
        if (recipe instanceof JemmRecipeAccess access) return access.mekpreview$model();
        return recipe instanceof EMIMultiblockRecipe<?> linked
                && linked.getRecipe() instanceof MultiblockWidget model && supports(model) ? model : null;
    }
    public static boolean supports(MultiblockWidget model) { return model.canCreatePreview() || MtePreview.supports(model); }
    public static MultiblockWidget adapt(Object source) {
        if (source instanceof MultiblockWidget model) return supports(model) ? model : null;
        if (source != null && source.getClass().getName().equals(NovaMultiblockWidget.ENTRY_CLASS)) {
            try { return new NovaMultiblockWidget(source); }
            catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Nova structure bridge unavailable; keeping original recipe view", e);
            }
        }
        return null;
    }
    public static void openFirst() {
        EmiApi.displayRecipeCategory(JemmCategoryPlugin.CATEGORY);
    }
    public static String equipmentKey(MultiblockWidget model) {
        return model instanceof NovaMultiblockWidget nova ? nova.equipmentKey() : model.getClass().getName();
    }
    public static String title(MultiblockWidget model) {
        if (model instanceof NovaMultiblockWidget nova) return nova.title();
        if (MtePreview.supports(model)) return MtePreview.title(model);
        return giselle.jei_mekanism_multiblocks.client.jei.JEI_MekanismMultiblocks_JeiPlugin.instance()
                .getCategories().stream().filter(c -> c.getRecipeType().getRecipeClass().equals(model.getClass()))
                .map(c -> c.getTitle().getString()).findFirst()
                .orElseGet(() -> net.minecraft.network.chat.Component.translatable(kind(model).translationKey()).getString());
    }
    public static String title(EmiRecipe recipe, MultiblockWidget model) {
        if (model instanceof NovaMultiblockWidget nova) return nova.title();
        if (recipe instanceof JemmRecipeAccess access && access.mekpreview$title() != null) return access.mekpreview$title();
        return title(model);
    }
    public static StructureKind kind(MultiblockWidget model) {
        for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getSimpleName();
            if (name.contains("DynamicTank")) return StructureKind.DYNAMIC_TANK;
            if (name.contains("Evaporation")) return StructureKind.EVAPORATION;
            if (name.contains("Matrix")) return StructureKind.INDUCTION_MATRIX;
            if (name.contains("Boiler")) return StructureKind.BOILER;
            if (name.contains("SPS")) return StructureKind.SPS;
            if (name.contains("Fission")) return StructureKind.FISSION_REACTOR;
            if (name.contains("Fusion")) return StructureKind.FUSION_REACTOR;
            if (name.contains("Turbine")) return StructureKind.TURBINE;
        }
        return StructureKind.DYNAMIC_TANK; // Only a display fallback; geometry always comes from JEMM.
    }
}
