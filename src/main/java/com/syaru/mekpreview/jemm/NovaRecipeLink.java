package com.syaru.mekpreview.jemm;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Exact native object or recipe-ID matching, never a fuzzy output-only match. */
public final class NovaRecipeLink {
    public record Link(EmiRecipe equipment, NovaMultiblockWidget model, int recipeIndex) {}
    private NovaRecipeLink() {}
    private static Object source(EmiRecipe recipe) { return recipe instanceof JemmRecipeAccess access ? access.mekpreview$source() : null; }
    private static final String PRIMITIVE = "com.falcon2235.moremultiblock.client.jei.PbfRecipeCategory$PbfDisplayRecipe";
    public static boolean hasFooter(EmiRecipe recipe) {
        Object source = source(recipe);
        return source != null && (source.getClass().getName().equals("com.falcon2235.moremultiblock.machine.ChemRecipe")
                || source.getClass().getName().equals(PRIMITIVE));
    }
    public static List<Link> find(EmiRecipe selected) {
        if (selected == null) return List.of();
        var links = new ArrayList<Link>();
        for (var recipe : EmiApi.getRecipeManager().getRecipes()) {
            if (JemmIntegration.model(recipe) instanceof NovaMultiblockWidget nova) {
                Object raw = source(selected);
                int index = raw != null && raw.getClass().getName().equals(PRIMITIVE) && nova.performance().kind().equals("PBF")
                        ? 0 : nova.performance().findRecipe(raw, selected.getId());
                if (index >= 0) links.add(new Link(recipe, nova, index));
            }
        }
        return List.copyOf(links);
    }
    public static boolean open(EmiRecipe selected) {
        var links = find(selected);
        if (links.isEmpty()) return false;
        var link = links.get(0);
        link.model().setRecipeIndex(link.recipeIndex()); link.model().markNeedUpdate();
        EmiApi.displayRecipe(link.equipment()); return true;
    }
    public static void addButton(WidgetHolder holder, EmiRecipe recipe) {
        if (!hasFooter(recipe)) return;
        holder.add(new Widget() {
            @Override public Bounds getBounds() { return new Bounds(0, holder.getHeight() - 17, holder.getWidth(), 16); }
            @Override public void render(GuiGraphics g, int x, int y, float partial) {
                var b = getBounds(); boolean over = b.contains(x, y);
                g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), over ? 0xFFD9DFE7 : 0xFF9A9A9A);
                g.drawCenteredString(Minecraft.getInstance().font, Component.translatable("mekpreview.search.preview"),
                        b.x() + b.width() / 2, b.y() + 4, 0xFFFFFF);
            }
            @Override public boolean mouseClicked(int x, int y, int button) { return button == 0 && getBounds().contains(x, y) && open(recipe); }
        });
    }
}
