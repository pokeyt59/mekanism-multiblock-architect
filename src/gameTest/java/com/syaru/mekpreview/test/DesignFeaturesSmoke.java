package com.syaru.mekpreview.test;

import com.syaru.mekpreview.client.design.*;
import com.syaru.mekpreview.client.BlockResolver;
import com.syaru.mekpreview.jei.*;
import com.syaru.mekpreview.jemm.*;
import com.syaru.mekpreview.structure.*;
import giselle.jei_mekanism_multiblocks.client.jei.category.DynamicTankCategory.DynamicTankWidget;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import java.util.*;
import java.util.function.Predicate;

/** Real JEI/LDLib widgets, isolated development inventory, and on-disk design persistence. */
final class DesignFeaturesSmoke {
    private static FavoritesStore.Favorite tank, novaFavorite;
    private static String material;
    private static long required;
    private static int selectedRecipe, linkedIndex;
    private static NovaMultiblockWidget nova;
    private static NovaRecipeLink.Link rawLink;
    static void tick(Minecraft mc, int tick) throws Exception {
        if (tick == 1220) MekaPlanJeiPlugin.show(MekaPlanJeiPlugin.recipes().stream()
                .filter(r -> r.model() instanceof DynamicTankWidget).findFirst().orElseThrow());
        if (tick == 1240) {
            var root = ClientSmoke.preview(mc); root.showPanel("materials"); root.materialsPanel().refresh();
            var row = root.materialsPanel().rows().stream().filter(r -> r.required() > 3).findFirst().orElseThrow();
            material = row.itemId(); required = row.required();
            require(row.available() == 0 && row.missing() == row.required(), "Empty inventory shortage was incorrect");
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                var stack = BlockResolver.item(material); stack.setCount(3);
                player.getInventory().setItem(12, stack); player.inventoryMenu.broadcastChanges();
            });
            ClientSmoke.capture(mc, "features-01-materials-all.png");
        }
        if (tick == 1260) {
            var root = ClientSmoke.preview(mc); root.materialsPanel().refresh();
            var row = root.materialsPanel().rows().stream().filter(r -> r.itemId().equals(material)).findFirst().orElseThrow();
            require(row.available() == 3 && row.missing() == required - 3, "Inventory update did not change shortage");
            if (root.settings().layerMode == LayerMode.ALL) ClientSmoke.press(mc, "layer");
            ClientSmoke.press(mc, "materials-visible");
        }
        if (tick == 1280) {
            var root = ClientSmoke.preview(mc); root.materialsPanel().refresh(); var view = root.settings();
            var expected = MaterialPlan.compare(root.snapshot(), true, view.layerMode, view.layer, view.hideShell, Map.of(material, 3L), true);
            require(expected.equals(root.materialsPanel().rows()), "Visible-layer material counts were stale");
            ClientSmoke.capture(mc, "features-02-materials-layer.png");
            ClientSmoke.press(mc, "favorites"); ClientSmoke.press(mc, "favorite-name");
            String label = "QA tank " + UUID.randomUUID();
            for (char character : label.toCharArray())
                require(ClientSmoke.type(mc, character), "Favorite name did not accept typed character");
            ClientSmoke.press(mc, "favorite-save");
            tank = FavoriteClient.store().list().stream().filter(f -> f.label().equals(label)).findFirst().orElseThrow();
            root.model().setDimensionWidth(3); root.model().setDimensionHeight(3);
            ClientSmoke.press(mc, "shell"); ClientSmoke.press(mc, "rotate"); ClientSmoke.press(mc, "mode");
        }
        if (tick == 1290) ClientSmoke.capture(mc, "features-03-favorites-panel.png");
        if (tick == 1300) ClientSmoke.preview(mc).loadFavorite(tank);
        if (tick == 1320) {
            verifyRestored(ClientSmoke.preview(mc), tank);
            require(new FavoritesStore(FavoriteClient.store().file()).list().contains(tank), "Favorite did not persist to disk");
            ClientSmoke.capture(mc, "features-03-favorite-restored.png");
        }
        boolean hasNova = net.minecraftforge.fml.ModList.get().isLoaded("mekanism_more_multiblock");
        if (tick == 1340 && hasNova) {
            var recipe = MekaPlanJeiPlugin.recipes().stream().filter(r -> r.model() instanceof NovaMultiblockWidget n
                    && n.performance().kind().equals("BLAST_FURNACE")).findFirst().orElseThrow();
            nova = (NovaMultiblockWidget) recipe.model(); MekaPlanJeiPlugin.show(recipe);
        }
        if (tick == 1360 && hasNova) {
            ClientSmoke.press(mc, "nova-search");
        }
        if (tick == 1364 && hasNova) {
            var root = ClientSmoke.preview(mc);
            var screen = mc.screen;
            require(ClientSmoke.key(mc, org.lwjgl.glfw.GLFW.GLFW_KEY_E) && mc.screen == screen,
                    "Inventory shortcut closed the recipe while search was focused");
            var query = nova.performance().recipes().get(Math.min(3, nova.performance().recipes().size() - 1)).name().getString();
            for (char character : query.toCharArray())
                require(ClientSmoke.type(mc, character), "Search field did not accept typed character");
            require(!root.searchPanel().matches().isEmpty(), "Nova name search had no results");
            require(root.searchPanel().matches().size() < nova.performance().recipes().size(), "Typed search did not filter recipes");
            var input = (com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget) find(root, "nova-query");
            require(input.getCurrentString().equals(query), "Search field did not retain typed query");
            selectedRecipe = root.searchPanel().matches().get(0);
        }
        if (tick == 1370 && hasNova) ClientSmoke.capture(mc, "features-04-nova-search.png");
        if (tick == 1372 && hasNova) {
            var page = ClientSmoke.page(mc);
            var pos = page.editor().searchPanel().getPosition();
            require(ClientSmoke.click(mc, page.x() + pos.x + 65, page.y() + pos.y + 36), "Nova search row did not accept click");
        }
        if (tick == 1380 && hasNova) {
            var root = ClientSmoke.preview(mc);
            require(nova.recipeIndex() == selectedRecipe && !root.searchPanel().isVisible(), "Search selection did not update the native recipe");
            nova.setSpeedUpgrades(2); nova.setEnergyUpgrades(1); nova.setCoil(Math.max(2, nova.coil()));
            novaFavorite = root.saveFavorite("QA Nova " + UUID.randomUUID());
            nova.setRecipeIndex(0); nova.setSpeedUpgrades(0); nova.setEnergyUpgrades(0); nova.setCoil(0);
            root.loadFavorite(novaFavorite);
        }
        if (tick == 1400 && hasNova) {
            verifyRestored(ClientSmoke.preview(mc), novaFavorite);
            var manager = MekaPlanJeiPlugin.runtime().getRecipeManager();
            var smelting = manager.createRecipeLookup(RecipeTypes.SMELTING).get().findFirst().orElseThrow();
            require(NovaRecipeLink.find(smelting, smelting.getId()).stream().anyMatch(l -> l.model().performance().kind().equals("SMELTING")), "Vanilla JEI recipe-ID link did not match Nova smelter");
            Object primitive = recipeWhere(manager, r -> r.getClass().getName().endsWith("$PbfDisplayRecipe"));
            require(hasButton(manager, primitive) && NovaRecipeLink.find(primitive, null).stream()
                    .anyMatch(l -> l.model().performance().kind().equals("PBF")), "Primitive blast-furnace preview link missing");
            linkedIndex = nova.performance().recipes().size() - 1;
            Object source = nova.performance().recipes().get(linkedIndex).nativeRecipe();
            Object raw = recipeWhere(manager, r -> r == source);
            rawLink = NovaRecipeLink.find(raw, null).stream().filter(l -> l.model() == nova && l.recipeIndex() == linkedIndex)
                    .findFirst().orElseThrow(() -> new IllegalStateException("JEI native recipe link was not exact"));
            require(hasButton(manager, raw), "Missing JEI equipment-preview button");
            nova.setRecipeIndex(0);
        }
        if (tick == 1410 && hasNova) require(NovaRecipeLink.open(rawLink), "JEI preview button did not open the linked design");
        if (tick == 1430) {
            if (hasNova) {
                require(ClientSmoke.preview(mc).model() == nova && nova.recipeIndex() == linkedIndex, "JEI linked the wrong Nova operation");
                ClientSmoke.capture(mc, "features-06-jei-linked-design.png");
            }
            require(FavoriteClient.open(tank), "Cross-equipment favorite could not be opened");
        }
        if (tick == 1450) {
            verifyRestored(ClientSmoke.preview(mc), tank);
            FavoriteClient.store().remove(tank.id());
            if (novaFavorite != null) FavoriteClient.store().remove(novaFavorite.id());
            System.out.println("MEKPREVIEW_DESIGN_FEATURES_VERIFIED search/native JEI recipe button link/inventory shortages/visible layers/persistent and cross-equipment favorites");
        }
    }
    private static void verifyRestored(JemmPreviewWidget root, FavoritesStore.Favorite favorite) throws Exception {
        var state = new CompoundTag(); root.model().save(state);
        require(state.equals(TagParser.parseTag(favorite.state())), "Native design settings were not restored exactly");
        var view = root.settings();
        require(view.layerMode.name().equals(favorite.view().mode()) && view.layer == favorite.view().layer()
                && view.rotation == favorite.view().rotation() && view.hideShell == favorite.view().hideShell(), "Saved layer/rotation/shell view was not restored");
    }
    /** The first JEI recipe, in any category including hidden ones, that passes the test. */
    private static Object recipeWhere(IRecipeManager manager, Predicate<Object> test) {
        return manager.createRecipeCategoryLookup().includeHidden().get()
                .flatMap(c -> manager.createRecipeLookup(c.getRecipeType()).includeHidden().get().map(r -> (Object) r))
                .filter(test).findFirst().orElseThrow(() -> new IllegalStateException("No matching JEI recipe"));
    }
    /** Whether JEI would show MekaPlan's equipment-preview button beside this recipe. */
    private static boolean hasButton(IRecipeManager manager, Object raw) {
        return manager.createRecipeCategoryLookup().includeHidden().get()
                .anyMatch(c -> manager.createRecipeLookup(c.getRecipeType()).includeHidden().get().anyMatch(r -> r == raw)
                        && hasButton(manager, c, raw));
    }
    private static <T> boolean hasButton(IRecipeManager manager, IRecipeCategory<T> category, Object raw) {
        T recipe = category.getRecipeType().getRecipeClass().cast(raw);
        var focus = MekaPlanJeiPlugin.runtime().getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        return manager.createRecipeLayoutDrawable(category, recipe, focus)
                .map(layout -> new NovaRecipeLink.ButtonFactory().createButtonController(layout) != null).orElse(false);
    }
    private static com.lowdragmc.lowdraglib.gui.widget.Widget find(com.lowdragmc.lowdraglib.gui.widget.Widget root, String id) {
        if (id.equals(root.getId())) return root;
        if (root instanceof com.lowdragmc.lowdraglib.gui.widget.WidgetGroup group) for (var child : group.widgets) {
            var found = find(child, id); if (found != null) return found;
        }
        return null;
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
