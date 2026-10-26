package com.syaru.mekpreview.client.design;

import com.syaru.mekpreview.jemm.*;
import com.syaru.mekpreview.structure.LayerMode;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class FavoriteClient {
    private static final Map<MultiblockWidget, FavoritesStore.View> VIEWS = new WeakHashMap<>();
    private static FavoritesStore store;
    private FavoriteClient() {}
    public static synchronized FavoritesStore store() {
        if (store == null) store = new FavoritesStore(Minecraft.getInstance().gameDirectory.toPath().resolve("config/mekpreview/favorites.json"));
        return store;
    }
    public static FavoritesStore.View view(MultiblockWidget model) { return VIEWS.get(model); }
    public static void remember(MultiblockWidget model, FavoritesStore.View view) { VIEWS.put(model, view); }
    public static Map<String, EmiRecipe> available() {
        Map<String, EmiRecipe> designs = new LinkedHashMap<>();
        for (var recipe : EmiApi.getRecipeManager().getRecipes()) {
            var model = JemmIntegration.model(recipe);
            if (model != null) designs.putIfAbsent(JemmIntegration.equipmentKey(model), recipe);
        }
        return designs;
    }
    public static void restoreModel(MultiblockWidget model, FavoritesStore.Favorite favorite) throws Exception {
        if (!favorite.equipment().equals(JemmIntegration.equipmentKey(model))) throw new IllegalArgumentException("Wrong equipment");
        LayerMode.valueOf(favorite.view().mode());
        CompoundTag restored = TagParser.parseTag(favorite.state());
        CompoundTag previous = new CompoundTag(); model.save(previous);
        try { model.load(restored); model.markNeedUpdate(); }
        catch (RuntimeException e) { model.load(previous); model.markNeedUpdate(); throw e; }
        remember(model, favorite.view());
        if (model instanceof NovaMultiblockWidget nova && nova.missingSavedRecipe()) message("mekpreview.favorites.recipe_missing");
    }
    public static boolean open(FavoritesStore.Favorite favorite) {
        var recipe = available().get(favorite.equipment());
        if (recipe == null) { message("mekpreview.favorites.unavailable"); return false; }
        try { restoreModel(JemmIntegration.model(recipe), favorite); EmiApi.displayRecipe(recipe); return true; }
        catch (Exception e) { failure(e); return false; }
    }
    public static void message(String key) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable(key), true);
    }
    public static void failure(Exception e) {
        com.mojang.logging.LogUtils.getLogger().warn("Unable to save/load design; existing data preserved", e);
        message("mekpreview.favorites.error");
    }
}
