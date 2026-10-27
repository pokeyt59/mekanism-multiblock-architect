package com.syaru.mekpreview.jei;

import com.mojang.logging.LogUtils;
import com.syaru.mekpreview.MekPreview;
import com.syaru.mekpreview.client.BlockResolver;
import com.syaru.mekpreview.jemm.JemmIntegration;
import com.syaru.mekpreview.jemm.MtePreview;
import com.syaru.mekpreview.jemm.NovaMultiblockWidget;
import com.syaru.mekpreview.jemm.NovaRecipeLink;
import com.syaru.mekpreview.structure.StructureKind;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.util.*;

/**
 * Collects every JEMM-compatible design JEI knows about (JEMM itself, Mekanism Extras through JEMM,
 * More Thermal Evaporation and Nova Mekanism structures) into one planner category, then hides the
 * pages they came from. JEMM keeps owning the design objects, settings and calculations.
 */
@JeiPlugin
public final class MekaPlanJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation UID = new ResourceLocation(MekPreview.MOD_ID, "planner");
    private static final Comparator<PlannerRecipe> ORDER = Comparator.<PlannerRecipe>comparingInt(recipe ->
                    recipe.model() instanceof NovaMultiblockWidget ? 10000
                            : JemmIntegration.kind(recipe.model()).ordinal() * 100 + (MtePreview.supports(recipe.model()) ? 10 : 0))
            .thenComparing(PlannerRecipe::title);
    @Nullable private static volatile IJeiRuntime runtime;
    private static volatile List<PlannerRecipe> recipes = List.of();

    @Override public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new PlannerCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var kind : StructureKind.values())
            if (BuiltInRegistries.ITEM.containsKey(new ResourceLocation(kind.icon())))
                registration.addRecipeCatalyst(BlockResolver.item(kind.icon()), PlannerCategory.TYPE);
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeButtonFactory(new NovaRecipeLink.ButtonFactory());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        PlannerCategory.disposeAll();
        var manager = jeiRuntime.getRecipeManager();
        var found = new ArrayList<PlannerRecipe>();
        var absorbed = new ArrayList<RecipeType<?>>();
        for (var category : manager.createRecipeCategoryLookup().get().toList())
            collect(manager, category, found, absorbed);
        found.sort(ORDER);
        var pages = withIds(found);
        if (!pages.isEmpty()) manager.addRecipes(PlannerCategory.TYPE, pages);
        absorbed.forEach(manager::hideRecipeCategory);
        recipes = pages;
        runtime = jeiRuntime;
        LOGGER.info("MekaPlan: {} multiblock designs moved into one JEI category from {} source categories", pages.size(), absorbed.size());
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        recipes = List.of();
        PlannerCategory.disposeAll();
    }

    private static <T> void collect(IRecipeManager manager, IRecipeCategory<T> category, List<PlannerRecipe> found, List<RecipeType<?>> absorbed) {
        RecipeType<T> type = category.getRecipeType();
        if (type.equals(PlannerCategory.TYPE)) return;
        boolean expected = MultiblockWidget.class.isAssignableFrom(type.getRecipeClass())
                || type.getRecipeClass().getName().equals(NovaMultiblockWidget.ENTRY_CLASS);
        int seen = 0, adapted = 0;
        for (var it = manager.createRecipeLookup(type).get().iterator(); it.hasNext(); ) {
            T recipe = it.next();
            seen++;
            var model = JemmIntegration.adapt(recipe);
            if (model == null) {
                // Structure categories hold nothing but structures; leave ordinary ones after one look.
                if (!expected && adapted == 0) return;
                continue;
            }
            String title = model instanceof NovaMultiblockWidget nova ? nova.title() : category.getTitle().getString();
            if (title.isBlank()) title = JemmIntegration.title(model);
            found.add(new PlannerRecipe(model, title, recipe, null));
            adapted++;
        }
        // Hide the original page only when every design on it moved to the planner.
        if (adapted > 0 && adapted == seen) absorbed.add(type);
    }

    /** Stable IDs from each design's equipment key, so JEI bookmarks survive restarts. */
    private static List<PlannerRecipe> withIds(List<PlannerRecipe> sorted) {
        var used = new HashSet<String>();
        var out = new ArrayList<PlannerRecipe>(sorted.size());
        for (var recipe : sorted) {
            String base = JemmIntegration.equipmentKey(recipe.model()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
            String path = base;
            for (int n = 2; !used.add(path); n++) path = base + "_" + n;
            out.add(new PlannerRecipe(recipe.model(), recipe.title(), recipe.source(), new ResourceLocation(MekPreview.MOD_ID, path)));
        }
        return List.copyOf(out);
    }

    public static List<PlannerRecipe> recipes() { return recipes; }
    @Nullable public static IJeiRuntime runtime() { return runtime; }

    /** True when the planner hosts this design, so JEMM's own JEI refresh can be skipped. */
    public static boolean hosts(MultiblockWidget model) {
        for (var recipe : recipes) if (recipe.model() == model) return true;
        return false;
    }

    /** Opens the planner category (the K key). */
    public static boolean openCategory() {
        var jei = runtime;
        if (jei == null || recipes.isEmpty()) return false;
        jei.getRecipesGui().showTypes(List.of(PlannerCategory.TYPE));
        return true;
    }

    /** Opens the planner on one design, keeping the other designs reachable with JEI's page buttons. */
    public static boolean show(PlannerRecipe target) {
        var jei = runtime;
        var all = recipes;
        int index = all.indexOf(target);
        if (jei == null || index < 0) return false;
        var ordered = new ArrayList<PlannerRecipe>(all.size());
        ordered.addAll(all.subList(index, all.size()));
        ordered.addAll(all.subList(0, index));
        jei.getRecipesGui().showRecipes(jei.getRecipeManager().getRecipeCategory(PlannerCategory.TYPE), ordered, List.of());
        return true;
    }
}
