package com.syaru.mekpreview.test;

import com.syaru.mekpreview.jemm.*;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Exercises published Nova's own calculations without ticking or placing a detached controller. */
final class NovaSmoke {
    static void verify(List<NovaMultiblockWidget> models) throws Exception {
        int recipes = 0;
        for (var model : models) {
            model.initialize();
            CompoundTag saved = new CompoundTag(); model.save(saved);
            var api = model.performance();
            for (int i = 0; i < api.recipes().size(); i++) {
                model.setRecipeIndex(i); refresh(model);
                var result = model.calculated(); var recipe = api.recipes().get(i);
                check(result.ticks() > 0 && result.joulesPerTick() >= 0, "Invalid native calculation " + model.title());
                check(result.outputs().stream().allMatch(f -> Double.isFinite(f.amount()) && f.amount() >= 0
                        && f.probability() > 0 && f.probability() <= 1), "Invalid output probability");
                if (recipe.nativeRecipe() != null && !api.parallel()
                        && !Set.of("VOID_MINER", "COMBUSTION_GENERATOR", "ANNIHILATION_GENERATOR").contains(api.kind())) {
                    int ticks = (int) nativeNumber(recipe.nativeRecipe(), "ticks");
                    int bonus = model.coil() - recipe.requiredCoil();
                    int expected = bonus <= 0 ? ticks : Math.max(1, ticks >> bonus);
                    check(result.ticks() == Math.max(1, expected), "Base native duration mismatch");
                    check(result.joulesPerTick() == (long) nativeNumber(recipe.nativeRecipe(), "energyPerTick"), "Base native energy mismatch");
                }
                recipes++;
            }
            if (!api.recipes().isEmpty()) {
                model.setRecipeIndex(0); model.setSpeedUpgrades(0); model.setEnergyUpgrades(0); refresh(model);
                var base = model.calculated();
                model.setSpeedUpgrades(4); refresh(model);
                var faster = model.calculated();
                check(faster.ticks() <= base.ticks(), "Speed upgrades made a recipe slower");
                model.setEnergyUpgrades(4); refresh(model);
                check(model.calculated().joulesPerTick() <= faster.joulesPerTick(), "Energy upgrades raised power consumption");
                if (api.parallel()) {
                    model.setParallelTier(6); refresh(model);
                    check(model.calculated().batch() > 1 && model.calculated().batch() <= 300, "Invalid parallel cap");
                    check(JemmBlueprint.capture(model).materials().keySet().stream().anyMatch(id -> id.contains("parallel")), "Parallel processor not in material list");
                }
                if (Set.of("BLAST_FURNACE", "ALLOY_BLAST_FURNACE", "REACTOR").contains(api.kind())) {
                    model.setCoil(1); refresh(model);
                    check(JemmBlueprint.capture(model).blocks().values().stream().anyMatch(p -> p.blockId().contains("coil")), "Coil preview disappeared");
                }
            }
            if (api.recipes().size() > 1) {
                model.setRecipeIndex(api.recipes().size() - 1);
                CompoundTag keyed = new CompoundTag(); model.save(keyed);
                String key = keyed.getString("NovaRecipeKey"); keyed.putInt("NovaRecipe", 0);
                model.load(keyed);
                check(api.recipeKey(model.recipeIndex()).equals(key), "Recipe restore depended on a stale numeric index");
            }
            model.load(saved); refresh(model);
            CompoundTag restored = new CompoundTag(); model.save(restored);
            check(saved.equals(restored), "Nova control state did not round-trip");
        }
        System.out.println("MEKPREVIEW_NOVA_PERFORMANCE_VERIFIED machines=" + models.size() + " recipes=" + recipes);
    }
    private static double nativeNumber(Object recipe, String field) throws Exception {
        return ((Number) recipe.getClass().getField(field).get(recipe)).doubleValue();
    }
    private static void refresh(NovaMultiblockWidget model) throws Exception {
        var update = MultiblockWidget.class.getDeclaredMethod("updateAll"); update.setAccessible(true); update.invoke(model);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
