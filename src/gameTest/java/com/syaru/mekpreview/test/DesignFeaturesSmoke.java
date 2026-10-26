package com.syaru.mekpreview.test;

import com.syaru.mekpreview.client.design.*;
import com.syaru.mekpreview.client.BlockResolver;
import com.syaru.mekpreview.jemm.*;
import com.syaru.mekpreview.structure.*;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import giselle.jei_mekanism_multiblocks.client.jei.category.DynamicTankCategory.DynamicTankWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import java.util.*;

/** Real EMI/LDLib widgets, isolated development inventory, and on-disk design persistence. */
final class DesignFeaturesSmoke {
    private static FavoritesStore.Favorite tank, novaFavorite;
    private static String material;
    private static long required;
    private static int selectedRecipe, linkedIndex;
    private static NovaMultiblockWidget nova;
    static void tick(Minecraft mc, int tick) throws Exception {
        if (tick == 1220) EmiApi.displayRecipe(EmiApi.getRecipeManager().getRecipes().stream()
                .filter(r -> JemmIntegration.model(r) instanceof DynamicTankWidget).findFirst().orElseThrow());
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
                require(mc.screen.charTyped(character, 0), "Favorite name did not accept typed character");
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
            var recipe = EmiApi.getRecipeManager().getRecipes().stream().filter(r -> JemmIntegration.model(r) instanceof NovaMultiblockWidget n
                    && n.performance().kind().equals("BLAST_FURNACE")).findFirst().orElseThrow();
            nova = (NovaMultiblockWidget) JemmIntegration.model(recipe); EmiApi.displayRecipe(recipe);
        }
        if (tick == 1360 && hasNova) {
            ClientSmoke.press(mc, "nova-search");
        }
        if (tick == 1364 && hasNova) {
            var root = ClientSmoke.preview(mc);
            var screen = mc.screen;
            require(screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_E, 0, 0) && mc.screen == screen,
                    "Inventory shortcut closed the recipe while search was focused");
            var query = nova.performance().recipes().get(Math.min(3, nova.performance().recipes().size() - 1)).name().getString();
            for (char character : query.toCharArray())
                require(mc.screen.charTyped(character, 0), "Search field did not accept typed character");
            require(!root.searchPanel().matches().isEmpty(), "Nova name search had no results");
            require(root.searchPanel().matches().size() < nova.performance().recipes().size(), "Typed search did not filter recipes");
            var input = (com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget) find(root, "nova-query");
            require(input.getCurrentString().equals(query), "Search field did not retain typed query");
            selectedRecipe = root.searchPanel().matches().get(0);
        }
        if (tick == 1370 && hasNova) ClientSmoke.capture(mc, "features-04-nova-search.png");
        if (tick == 1372 && hasNova) {
            var root = ClientSmoke.preview(mc);
            var pos = root.searchPanel().getPosition();
            require(mc.screen.mouseClicked(pos.x + 65, pos.y + 36, 0), "Nova search row did not accept click");
            mc.screen.mouseReleased(pos.x + 65, pos.y + 36, 0);
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
            var cooking = EmiApi.getRecipeManager().getRecipes().stream().filter(r -> r instanceof dev.emi.emi.recipe.EmiCookingRecipe
                    && r.getCategory().id.getPath().equals("smelting")).findFirst().orElseThrow();
            require(NovaRecipeLink.find(cooking).stream().anyMatch(l -> l.model().performance().kind().equals("SMELTING")), "Vanilla EMI recipe-ID link did not match Nova smelter");
            var primitive = EmiApi.getRecipeManager().getRecipes().stream().filter(r -> r instanceof JemmRecipeAccess a
                    && a.mekpreview$source() != null && a.mekpreview$source().getClass().getName().endsWith("$PbfDisplayRecipe"))
                    .findFirst().orElseThrow();
            require(NovaRecipeLink.hasFooter(primitive) && NovaRecipeLink.find(primitive).stream()
                    .anyMatch(l -> l.model().performance().kind().equals("PBF")), "Primitive blast-furnace preview link missing");
            linkedIndex = nova.performance().recipes().size() - 1;
            Object source = nova.performance().recipes().get(linkedIndex).nativeRecipe();
            EmiRecipe raw = EmiApi.getRecipeManager().getRecipes().stream().filter(r -> r instanceof JemmRecipeAccess a
                    && a.mekpreview$source() == source).findFirst().orElseThrow();
            require(NovaRecipeLink.find(raw).stream().anyMatch(l -> l.model() == nova && l.recipeIndex() == linkedIndex), "EMI native recipe link was not exact");
            nova.setRecipeIndex(0); EmiApi.displayRecipe(raw);
        }
        if (tick == 1405 && hasNova) ClientSmoke.capture(mc, "features-05-emi-link-button.png");
        if (tick == 1410 && hasNova) {
            var field = RecipeScreen.class.getDeclaredField("currentPage"); field.setAccessible(true);
            @SuppressWarnings("unchecked") var groups = (List<WidgetGroup>) field.get(mc.screen);
            var button = groups.stream().flatMap(g -> g.widgets.stream())
                    .filter(w -> w.getClass().getName().startsWith(NovaRecipeLink.class.getName() + "$"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Missing EMI equipment-preview button"));
            var b = button.getBounds();
            require(button.mouseClicked(b.x() + b.width() / 2, b.y() + 8, 0), "EMI preview button did not open the linked design");
        }
        if (tick == 1430) {
            if (hasNova) {
                require(ClientSmoke.preview(mc).model() == nova && nova.recipeIndex() == linkedIndex, "EMI linked the wrong Nova operation");
                ClientSmoke.capture(mc, "features-06-emi-linked-design.png");
            }
            require(FavoriteClient.open(tank), "Cross-equipment favorite could not be opened");
        }
        if (tick == 1450) {
            verifyRestored(ClientSmoke.preview(mc), tank);
            FavoriteClient.store().remove(tank.id());
            if (novaFavorite != null) FavoriteClient.store().remove(novaFavorite.id());
            System.out.println("MEKPREVIEW_DESIGN_FEATURES_VERIFIED search/native EMI footer link/inventory shortages/visible layers/persistent and cross-equipment favorites");
        }
    }
    private static void verifyRestored(JemmPreviewWidget root, FavoritesStore.Favorite favorite) throws Exception {
        var state = new CompoundTag(); root.model().save(state);
        require(state.equals(TagParser.parseTag(favorite.state())), "Native design settings were not restored exactly");
        var view = root.settings();
        require(view.layerMode.name().equals(favorite.view().mode()) && view.layer == favorite.view().layer()
                && view.rotation == favorite.view().rotation() && view.hideShell == favorite.view().hideShell(), "Saved layer/rotation/shell view was not restored");
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
