package com.syaru.mekpreview.client.design;

import com.syaru.mekpreview.jemm.JemmPreviewWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class FavoritesPanel extends SidePanel {
    private final JemmPreviewWidget editor;
    private final Input input;
    private List<FavoritesStore.Favorite> entries = List.of();
    private Set<String> available = Set.of();
    private String name = "", error;
    public FavoritesPanel(JemmPreviewWidget editor) {
        this.editor = editor;
        input = new Input(0, 0, 158, "mekpreview.favorites.name", () -> name, value -> name = value);
        input.setId("favorite-name"); addWidget(input);
        button("favorite-save", 162, 0, 18, () -> "+", this::save).setHoverTooltips(Component.translatable("mekpreview.favorites.save"));
    }
    public void open() { refresh(); }
    public void blur() { input.setFocus(false); }
    private void refresh() {
        try { entries = FavoriteClient.store().list(); available = Set.copyOf(FavoriteClient.available().keySet()); error = null; }
        catch (Exception e) { error = "mekpreview.favorites.error"; FavoriteClient.failure(e); }
        scroll = Math.min(scroll, Math.max(0, entries.size() - 5));
    }
    private void save() {
        try { editor.saveFavorite(name); refresh(); name = ""; input.setCurrentString(""); }
        catch (Exception e) { error = "mekpreview.favorites.error"; FavoriteClient.failure(e); }
    }
    @Override public void drawInBackground(GuiGraphics g, int x, int y, float partial) {
        super.drawInBackground(g, x, y, partial);
        text(g, error == null ? "★ " + entries.size() : Component.translatable(error).getString(), 2, 19, 176, error == null ? 0x404040 : 0xB02A22);
        for (int i = 0; i < 5 && scroll + i < entries.size(); i++) {
            var entry = entries.get(scroll + i); int top = 29 + i * 18;
            int px = getPosition().x, py = getPosition().y + top;
            g.fill(px, py, px + 158, py + 17, 0xFFB5B5B5);
            g.fill(px + 162, py, px + 180, py + 17, 0xFFA0A0A0);
            text(g, entry.label(), 2, top + 4, 154, available.contains(entry.equipment()) ? 0x303030 : 0x777777);
            text(g, "×", 168, top + 4, 10, 0x852A22);
        }
    }
    @Override public void drawInForeground(GuiGraphics g, int x, int y, float partial) {
        super.drawInForeground(g, x, y, partial);
        int index = rowAt(x, y, 29, 18, entries.size());
        if (index >= 0) {
            var entry = entries.get(index);
            tooltip(List.of(Component.literal(entry.label()), Component.translatable(x >= getPosition().x + 162
                    ? "mekpreview.favorites.delete" : available.contains(entry.equipment())
                    ? "mekpreview.favorites.load" : "mekpreview.favorites.unavailable")));
        }
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        int index = rowAt(x, y, 29, 18, entries.size());
        if (index >= 0 && button == 0) {
            var entry = entries.get(index);
            if (x >= getPosition().x + 162) {
                try { FavoriteClient.store().remove(entry.id()); refresh(); }
                catch (Exception e) { error = "mekpreview.favorites.error"; FavoriteClient.failure(e); }
            } else editor.loadFavorite(entry);
            return true;
        }
        return isMouseOverElement(x, y);
    }
    @Override public boolean mouseWheelMove(double x, double y, double delta) { return scroll(x, y, delta, entries.size(), 5); }
}
