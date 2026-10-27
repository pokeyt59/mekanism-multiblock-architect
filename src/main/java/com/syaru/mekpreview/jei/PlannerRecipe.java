package com.syaru.mekpreview.jei;

import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import net.minecraft.resources.ResourceLocation;

/**
 * One planner page. JEMM (or a supported addon) still owns the live design object;
 * this only remembers the original JEI recipe it came from and a stable ID for bookmarks.
 */
public record PlannerRecipe(MultiblockWidget model, String title, Object source, ResourceLocation id) {}
