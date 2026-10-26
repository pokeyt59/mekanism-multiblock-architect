package com.syaru.mekpreview.client.design;

import com.syaru.mekpreview.jemm.NovaMultiblockWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.IntConsumer;

public final class NovaSearchPanel extends SidePanel {
    private final NovaMultiblockWidget model;
    private final IntConsumer choose;
    private final Input input;
    private List<String> documents;
    private List<Integer> matches;
    private String query = "";
    private boolean focusPending;
    public NovaSearchPanel(NovaMultiblockWidget model, IntConsumer choose) {
        this.model = model; this.choose = choose;
        documents = model.performance().searchDocuments(); matches = RecipeSearch.filter(documents, "");
        input = new Input(0, 0, 180, "mekpreview.search.hint", () -> query, this::setQuery);
        input.setId("nova-query"); addWidget(input);
    }
    public void open() { documents = model.performance().searchDocuments(); setQuery(query); focusPending = true; }
    public void blur() { focusPending = false; input.setFocus(false); }
    @Override public void updateScreen() {
        super.updateScreen();
        if (focusPending && isVisible()) { input.setFocus(true); focusPending = false; }
    }
    public void setQuery(String value) { query = value; matches = RecipeSearch.filter(documents, query); scroll = 0; }
    public List<Integer> matches() { return matches; }
    public void choose(int matchedIndex) { choose.accept(matches.get(matchedIndex)); }
    @Override public void drawInBackground(GuiGraphics g, int x, int y, float partial) {
        super.drawInBackground(g, x, y, partial);
        text(g, matches.size() + " / " + documents.size(), 2, 19, 176, 0x404040);
        for (int row = 0; row < 5 && scroll + row < matches.size(); row++) {
            int index = matches.get(scroll + row), top = 29 + row * 18;
            int px = getPosition().x, py = getPosition().y + top;
            g.fill(px, py, px + 180, py + 17, index == model.recipeIndex() ? 0xFFDBE9CD : 0xFFB5B5B5);
            text(g, Integer.toString(index + 1), 2, top + 4, 27, 0x404040);
            text(g, model.performance().recipes().get(index).name().getString(), 31, top + 4, 147, 0x303030);
        }
        if (matches.isEmpty()) text(g, Component.translatable("mekpreview.search.empty").getString(), 2, 36, 176, 0x606060);
    }
    @Override public void drawInForeground(GuiGraphics g, int x, int y, float partial) {
        super.drawInForeground(g, x, y, partial);
        int row = rowAt(x, y, 29, 18, matches.size());
        if (row >= 0) {
            var recipe = model.performance().recipes().get(matches.get(row));
            List<Component> lines = new ArrayList<>(); lines.add(recipe.name());
            recipe.inputs().forEach(f -> lines.add(Component.translatable("mekpreview.nova.input", f.name(), f.amount(), f.volume() ? "mB" : "")));
            tooltip(lines);
        }
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        int row = rowAt(x, y, 29, 18, matches.size());
        if (row >= 0 && button == 0) { choose(row); return true; }
        return isMouseOverElement(x, y);
    }
    @Override public boolean mouseWheelMove(double x, double y, double delta) { return scroll(x, y, delta, matches.size(), 5); }
}
