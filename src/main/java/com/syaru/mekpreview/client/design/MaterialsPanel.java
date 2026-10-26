package com.syaru.mekpreview.client.design;

import com.syaru.mekpreview.client.BlockResolver;
import com.syaru.mekpreview.client.Hologram;
import com.syaru.mekpreview.build.MaterialInventory;
import com.syaru.mekpreview.jemm.JemmPreviewWidget;
import com.syaru.mekpreview.structure.MaterialPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class MaterialsPanel extends SidePanel {
    private final JemmPreviewWidget editor;
    private boolean visibleOnly, missingOnly = true;
    private List<MaterialPlan.Row> rows = List.of();
    private int ticks;
    private record Inputs(com.syaru.mekpreview.structure.Blueprint blueprint, boolean visibleOnly,
                          com.syaru.mekpreview.structure.LayerMode mode, int layer, boolean hideShell,
                          boolean missingOnly, Map<String, Long> inventory, long placedRevision) {}
    private Inputs previous;
    public MaterialsPanel(JemmPreviewWidget editor) {
        this.editor = editor;
        button("materials-all", 0, 0, 45, () -> label("all", !visibleOnly), () -> { visibleOnly = false; refresh(); });
        button("materials-visible", 47, 0, 57, () -> label("visible", visibleOnly), () -> { visibleOnly = true; refresh(); });
        button("materials-missing", 106, 0, 74, () -> label("missing_only", missingOnly), () -> { missingOnly = !missingOnly; refresh(); });
    }
    private static String label(String key, boolean selected) {
        String text = Component.translatable("mekpreview.materials." + key).getString(); return selected ? "[" + text + "]" : text;
    }
    public void setVisibleOnly(boolean value) { visibleOnly = value; refresh(); }
    public List<MaterialPlan.Row> rows() { return rows; }
    public void refresh() {
        var player = Minecraft.getInstance().player;
        Map<String, Long> inventory = player == null ? Map.of() : MaterialInventory.available(player.getInventory());
        var settings = editor.settings();
        var current = new Inputs(editor.snapshot(), visibleOnly, settings.layerMode, settings.layer, settings.hideShell, missingOnly, inventory,
                Hologram.placedRevision(editor.snapshot(), settings.rotation));
        if (current.equals(previous)) return;
        previous = current;
        rows = MaterialPlan.compare(editor.snapshot(), visibleOnly, settings.layerMode, settings.layer, settings.hideShell, inventory, missingOnly,
                BlockResolver::itemId, Hologram.placedMaterials(editor.snapshot(), settings.rotation, visibleOnly,
                        settings.layerMode, settings.layer, settings.hideShell));
        scroll = Math.min(scroll, Math.max(0, rows.size() - 5));
    }
    @Override public void updateScreen() { super.updateScreen(); if (isVisible() && ++ticks % 5 == 0) refresh(); }
    private static String count(long value) { return value >= 1_000_000 ? value / 1_000_000 + "M" : value >= 10_000 ? value / 1000 + "k" : Long.toString(value); }
    private void number(GuiGraphics g, String value, int right, int top, int color) {
        var font = Minecraft.getInstance().font; text(g, value, right - font.width(value), top, 30, color);
    }
    @Override public void drawInBackground(GuiGraphics g, int x, int y, float partial) {
        super.drawInBackground(g, x, y, partial);
        number(g, Component.translatable("mekpreview.materials.remaining").getString(), 107, 21, 0x404040);
        number(g, Component.translatable("mekpreview.materials.have").getString(), 141, 21, 0x404040);
        number(g, Component.translatable("mekpreview.materials.short_header").getString(), 178, 21, 0x404040);
        for (int i = 0; i < 5 && scroll + i < rows.size(); i++) {
            var row = rows.get(scroll + i); int top = 34 + i * 17;
            var stack = BlockResolver.item(row.itemId());
            g.renderItem(stack, getPosition().x, getPosition().y + top);
            text(g, stack.getHoverName().getString(), 19, top + 4, 60, 0x303030);
            number(g, count(row.remaining()), 107, top + 4, 0x404040);
            number(g, count(row.available()), 141, top + 4, 0x225C32);
            number(g, count(row.missing()), 178, top + 4, row.missing() > 0 ? 0xB02A22 : 0x225C32);
        }
        if (rows.isEmpty()) text(g, Component.translatable("mekpreview.materials.empty").getString(), 2, 40, 176, 0x404040);
    }
    @Override public void drawInForeground(GuiGraphics g, int x, int y, float partial) {
        super.drawInForeground(g, x, y, partial);
        int index = rowAt(x, y, 34, 17, rows.size());
        if (index >= 0) {
            var row = rows.get(index);
            tooltip(List.of(BlockResolver.item(row.itemId()).getHoverName(),
                    Component.translatable("mekpreview.materials.progress_counts", row.required(), row.placed(), row.remaining(), row.available(), row.missing()),
                    Component.translatable("mekpreview.materials.inventory")));
        }
    }
    @Override public boolean mouseWheelMove(double x, double y, double delta) { return scroll(x, y, delta, rows.size(), 5); }
    @Override public boolean mouseClicked(double x, double y, int button) { return super.mouseClicked(x, y, button) || isMouseOverElement(x, y); }
}
