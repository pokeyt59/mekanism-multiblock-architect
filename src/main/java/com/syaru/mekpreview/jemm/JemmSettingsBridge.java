package com.syaru.mekpreview.jemm;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Embeds JEMM's own controls and calculation results, including its advanced layout editor. */
final class JemmSettingsBridge extends Widget {
    private final MultiblockWidget source;
    JemmSettingsBridge(MultiblockWidget source) {
        super(183, 3, 180, 120);
        this.source = source;
        source.initialize();
        // Keep native calculated values beside the live scene from the first frame.
        // Materials remain accessible without replacing the scene or the settings.
        source.showResultPanel();
        source.setWidth(180);
        source.setHeight(120);
        // The adjacent inline scene replaces JEMM's separate preview screen.
        for (var child : source.getChildren()) {
            if (!(child.getMessage().getContents() instanceof TranslatableContents text)) continue;
            switch (text.getKey()) {
                case "text.jei_mekanism_multiblocks.preview" -> child.visible = false;
                case "text.jei_mekanism_multiblocks.results" -> child.setMessage(Component.translatable("mekpreview.performance"));
                case "text.jei_mekanism_multiblocks.costs" -> child.setMessage(Component.translatable("mekpreview.materials"));
            }
        }
    }
    @Override public void drawInBackground(GuiGraphics g, int x, int y, float partial) {
        var pos = getPosition();
        g.pose().pushPose();
        g.pose().translate(pos.x, pos.y, 0);
        source.render(g, x - pos.x, y - pos.y, partial);
        g.pose().popPose();
    }
    @Override public void drawInForeground(GuiGraphics g, int x, int y, float partial) {
        var pos = getPosition();
        var tooltip = source.getTooltip(x - pos.x, y - pos.y);
        if (!tooltip.isEmpty() && gui != null) gui.getModularUIGui().setHoverTooltip(tooltip,
                net.minecraft.world.item.ItemStack.EMPTY, null, null);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        return source.mouseClicked(x - getPosition().x, y - getPosition().y, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return source.mouseReleased(x - getPosition().x, y - getPosition().y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return source.mouseDragged(x - getPosition().x, y - getPosition().y, button, dx, dy);
    }
    @Override public boolean mouseWheelMove(double x, double y, double delta) {
        return source.mouseScrolled(x - getPosition().x, y - getPosition().y, delta);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) { return source.keyPressed(key, scan, modifiers); }
    @Override public boolean keyReleased(int key, int scan, int modifiers) { return source.keyReleased(key, scan, modifiers); }
    @Override public boolean charTyped(char c, int modifiers) { return source.charTyped(c, modifiers); }
}
