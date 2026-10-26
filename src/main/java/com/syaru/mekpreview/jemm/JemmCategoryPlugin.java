package com.syaru.mekpreview.jemm;

import com.syaru.mekpreview.structure.StructureKind;
import com.syaru.mekpreview.client.BlockResolver;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/** One equipment category; JEMM/TMRV continue to own the existing recipes and models. */
@EmiEntrypoint
public final class JemmCategoryPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            new ResourceLocation("mekpreview", "multiblocks"), EmiStack.of(BlockResolver.item("mekanism:dynamic_tank"))) {
        @Override public Component getName() { return Component.translatable("mekpreview.category"); }
        @Override public java.util.Comparator<dev.emi.emi.api.recipe.EmiRecipe> getSort() {
            return java.util.Comparator.<dev.emi.emi.api.recipe.EmiRecipe>comparingInt(recipe -> {
                var model = JemmIntegration.model(recipe);
                return model instanceof NovaMultiblockWidget ? 10000
                        : JemmIntegration.kind(model).ordinal() * 100 + (MtePreview.supports(model) ? 10 : 0);
            }).thenComparing(recipe -> JemmIntegration.title(recipe, JemmIntegration.model(recipe)));
        }
    };
    @Override public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        for (var kind : StructureKind.values())
            if (BuiltInRegistries.ITEM.containsKey(new ResourceLocation(kind.icon())))
                registry.addWorkstation(CATEGORY, EmiStack.of(BlockResolver.item(kind.icon())));
    }
}
