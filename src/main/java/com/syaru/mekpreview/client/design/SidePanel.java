package com.syaru.mekpreview.client.design;

import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.*;

/** Reuses the editor's right column; never replaces or enlarges its 3-D scene. */
public abstract class SidePanel extends WidgetGroup {
    protected int scroll;
    protected SidePanel() { super(183, 3, 180, 120); setBackground(new ColorRectTexture(0xFFC6C6C6)); }
    public Input focusedInput() {
        if (!isVisible() || !isActive()) return null;
        return widgets.stream().filter(w -> w instanceof Input input && input.isFocus())
                .map(w -> (Input) w).findFirst().orElse(null);
    }
    protected ButtonWidget button(String id, int x, int y, int w, Supplier<String> label, Runnable action) {
        var button = new ButtonWidget(x, y, w, 17, click -> action.run());
        button.setId(id);
        button.setButtonTexture(new ColorRectTexture(0xC0606060), new TextTexture(label).setWidth(w - 2));
        button.setHoverBorderTexture(1, 0xFFFFFFFF); addWidget(button); return button;
    }
    protected void text(GuiGraphics g, String value, int x, int y, int width, int color) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, font.plainSubstrByWidth(value, width), getPosition().x + x, getPosition().y + y, color, false);
    }
    protected void tooltip(List<Component> lines) {
        if (gui != null && gui.getModularUIGui() != null) gui.getModularUIGui().setHoverTooltip(lines,
                net.minecraft.world.item.ItemStack.EMPTY, null, null);
    }
    protected int rowAt(double x, double y, int top, int height, int count) {
        if (!isMouseOverElement(x, y) || y < getPosition().y + top) return -1;
        if (y >= getPosition().y + top + ((getSize().height - top) / height) * height) return -1;
        int index = scroll + ((int) y - getPosition().y - top) / height;
        return index < count ? index : -1;
    }
    protected boolean scroll(double x, double y, double delta, int count, int visible) {
        if (!isMouseOverElement(x, y)) return false;
        scroll = Math.max(0, Math.min(Math.max(0, count - visible), scroll - (int) Math.signum(delta)));
        return true;
    }
    public static final class Input extends TextFieldWidget {
        public Input(int x, int y, int width, String hint, Supplier<String> value, Consumer<String> changed) {
            super(x, y, width, 17, value, changed);
            maxStringLength = 120;
            if (textField != null) { textField.setMaxLength(120); textField.setHint(Component.translatable(hint)); }
        }
    }
}
