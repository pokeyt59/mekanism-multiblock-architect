package com.syaru.mekpreview.client;

import com.syaru.mekpreview.ClientOptions;
import com.syaru.mekpreview.structure.Blueprint;
import com.syaru.mekpreview.structure.LayerMode;
import com.syaru.mekpreview.structure.MaterialPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** A screen-sized viewport, not a truncated bill of materials. */
public final class MaterialHud {
    private record Line(ItemStack stack, List<FormattedCharSequence> name, Component counts, int height, boolean missing) {}
    private record Scope(Blueprint plan, LayerMode mode, int layer, int rotation, boolean shell) {}
    private static List<MaterialPlan.Row> cachedRows;
    private static List<Line> lines = List.of();
    private static List<Integer> heights = List.of();
    private static Scope scope;
    private static int cachedWidth, cachedReload, requestedOffset, right, bottom;
    private static String cachedLanguage;
    private static MaterialHudLayout.View view = MaterialHudLayout.fit(0, List.of(), 0);

    public static void clear() {
        cachedRows = null; lines = List.of(); heights = List.of(); scope = null;
        requestedOffset = right = bottom = 0;
    }
    private static int width(Minecraft mc) {
        int screen = mc.getWindow().getGuiScaledWidth();
        return Math.max(1, Math.min(311, screen >= 416 ? screen - 264 : screen - 16));
    }
    private static void prepare(Minecraft mc) {
        var settings = Hologram.settings();
        var currentScope = new Scope(Hologram.blueprint(), settings.layerMode, settings.layer, settings.rotation, settings.hideShell);
        if (!currentScope.equals(scope)) { scope = currentScope; requestedOffset = 0; }
        var rows = ConstructionMaterials.rows();
        int width = width(mc), reload = HologramRenderer.reloadVersion();
        if (cachedRows == rows && cachedWidth == width && cachedReload == reload
                && mc.options.languageCode.equals(cachedLanguage)) return;
        cachedRows = rows; cachedWidth = width; cachedReload = reload; cachedLanguage = mc.options.languageCode;
        var counts = rows.stream().map(row -> Component.translatable("mekpreview.build.material_row",
                row.remaining(), row.available(), row.missing())).toList();
        int countWidth = counts.stream().mapToInt(mc.font::width).max().orElse(0);
        int nameWidth = Math.max(10, width - 20 - countWidth - 12);
        var prepared = new java.util.ArrayList<Line>();
        for (int i = 0; i < rows.size(); i++) {
            var stack = BlockResolver.item(rows.get(i).itemId());
            var name = mc.font.split(stack.getHoverName(), nameWidth);
            prepared.add(new Line(stack, name, counts.get(i), Math.max(16, name.size() * mc.font.lineHeight + 4) + 2,
                    rows.get(i).missing() > 0));
        }
        lines = List.copyOf(prepared); heights = lines.stream().map(Line::height).toList();
    }
    private static MaterialHudLayout.View layout(Minecraft mc) {
        int height = mc.getWindow().getGuiScaledHeight();
        // On narrow screens leave the existing target card unobscured. Wider screens can dock it beside the list.
        if (Hologram.target() != null && mc.getWindow().getGuiScaledWidth() < width(mc) + 264)
            height = Math.min(height, height / 2 + 26 - 12 + MaterialHudLayout.BOTTOM_MARGIN);
        return MaterialHudLayout.fit(height, heights, requestedOffset);
    }
    public static boolean scroll(double delta) {
        var mc = Minecraft.getInstance();
        if (!Double.isFinite(delta) || delta == 0 || mc.screen != null || mc.options.hideGui
                || !Hologram.shown() || !ClientOptions.MATERIAL_HUD.get() || ConstructionMaterials.rows().isEmpty()) return false;
        prepare(mc); view = layout(mc);
        if (!view.scrollable()) return false;
        long change = (long) Math.copySign(Math.max(1, Math.min(Integer.MAX_VALUE, Math.ceil(Math.abs(delta) * 18))), delta);
        requestedOffset = (int) Math.max(0L, Math.min(view.maxOffset(), (long) view.offset() - change));
        return true;
    }
    public static void render(GuiGraphics g) {
        right = bottom = 0;
        if (!ClientOptions.MATERIAL_HUD.get() || ConstructionMaterials.rows().isEmpty()) return;
        var mc = Minecraft.getInstance(); prepare(mc); view = layout(mc); requestedOffset = view.offset();
        int x = 8, top = MaterialHudLayout.TOP, width = width(mc), rowTop = top + MaterialHudLayout.HEADER;
        right = x + width + 3; bottom = rowTop + view.height() + (view.scrollable() ? MaterialHudLayout.FOOTER : 0) + 3;
        g.fill(x - 3, top - 3, right, bottom, 0xB0101722);
        g.drawString(mc.font, Component.translatable(width < 250 ? "mekpreview.build.materials_compact" : "mekpreview.build.materials"),
                x, top, 0x8FE7FF, false);
        g.enableScissor(x, rowTop, x + width - 5, rowTop + view.height());
        int y = rowTop - view.offset();
        for (int i = 0; i < view.first(); i++) y += lines.get(i).height();
        if (view.first() < lines.size() && lines.get(view.first()).height() <= view.height()) y = rowTop;
        for (int i = view.first(); i < view.end(); i++) {
            var line = lines.get(i);
            g.renderItem(line.stack(), x, y);
            for (int j = 0; j < line.name().size(); j++)
                g.drawString(mc.font, line.name().get(j), x + 20, y + 4 + j * mc.font.lineHeight, 0xFFFFFF, false);
            g.drawString(mc.font, line.counts(), x + width - 8 - mc.font.width(line.counts()), y + 4,
                    line.missing() ? 0xFF7979 : 0x9FE0A3, false);
            y += line.height();
        }
        g.disableScissor();
        if (view.scrollable()) {
            int barHeight = Math.min(view.height(), Math.max(5, view.height() * view.height() / view.contentHeight()));
            int barY = rowTop + (int) ((long) view.offset() * (view.height() - barHeight) / Math.max(1, view.maxOffset()));
            g.fill(x + width - 3, rowTop, x + width - 1, rowTop + view.height(), 0x503A4A5A);
            g.fill(x + width - 3, barY, x + width - 1, barY + barHeight, 0xCC8FE7FF);
            g.drawString(mc.font, Component.translatable("mekpreview.build.material_page",
                    view.height() == 0 ? 0 : view.first() + 1, view.end(), lines.size()),
                    x, rowTop + view.height() + 2, 0xCCD6E4, false);
        }
    }
    public static boolean overlaps(int x, int y, int width, int height) {
        return right > 0 && x < right && x + width > 5 && y < bottom && y + height > MaterialHudLayout.TOP - 3;
    }
    public static int targetX(int original, int y) {
        int beside = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 236;
        return overlaps(original - 4, y - 4, 232, 63) && beside - 4 >= right + 8 ? beside : original;
    }
    public static MaterialHudLayout.View viewport() { return view; }
    private MaterialHud() {}
}
