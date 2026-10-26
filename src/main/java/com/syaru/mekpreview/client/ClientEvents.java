package com.syaru.mekpreview.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.syaru.mekpreview.MekPreview;
import com.syaru.mekpreview.jemm.JemmIntegration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.List;

@Mod.EventBusSubscriber(modid = MekPreview.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static KeyMapping key(String action, int code) {
        return new KeyMapping("key.mekpreview." + action, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, code, "key.categories.mekpreview");
    }
    public static final KeyMapping OPEN = key("open", GLFW.GLFW_KEY_K);
    public static final KeyMapping ANCHOR = key("anchor", GLFW.GLFW_KEY_G);
    public static final KeyMapping TOGGLE = key("toggle", GLFW.GLFW_KEY_V);
    public static final KeyMapping ROTATE = key("rotate", GLFW.GLFW_KEY_R);
    public static final KeyMapping OPACITY = key("opacity", GLFW.GLFW_KEY_H);
    public static final KeyMapping CLEAR = key("clear", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping PICK = key("pick", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping LAYER_UP = key("layer_up", GLFW.GLFW_KEY_RIGHT_BRACKET);
    public static final KeyMapping LAYER_DOWN = key("layer_down", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping LAYER_MODE = key("layer_mode", GLFW.GLFW_KEY_BACKSLASH);
    public static final KeyMapping WEST = key("west", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping EAST = key("east", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping NORTH = key("north", GLFW.GLFW_KEY_UP);
    public static final KeyMapping SOUTH = key("south", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping UP = key("up", GLFW.GLFW_KEY_PAGE_UP);
    public static final KeyMapping DOWN = key("down", GLFW.GLFW_KEY_PAGE_DOWN);
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Hologram.tick();
        InventoryAssist.tick();
        ConstructionMaterials.tick();
        if (mc.screen != null || mc.player == null) return;
        while (OPEN.consumeClick()) JemmIntegration.openFirst();
        while (ANCHOR.consumeClick()) Hologram.anchor();
        while (TOGGLE.consumeClick()) Hologram.toggle();
        while (ROTATE.consumeClick()) Hologram.rotate();
        while (OPACITY.consumeClick()) Hologram.cycleOpacity();
        while (CLEAR.consumeClick()) Hologram.clear();
        while (PICK.consumeClick()) InventoryAssist.selectTarget();
        while (LAYER_UP.consumeClick()) Hologram.layer(1);
        while (LAYER_DOWN.consumeClick()) Hologram.layer(-1);
        while (LAYER_MODE.consumeClick()) Hologram.cycleLayerMode();
        while (WEST.consumeClick()) Hologram.nudge(-1, 0, 0);
        while (EAST.consumeClick()) Hologram.nudge(1, 0, 0);
        while (NORTH.consumeClick()) Hologram.nudge(0, 0, -1);
        while (SOUTH.consumeClick()) Hologram.nudge(0, 0, 1);
        while (UP.consumeClick()) Hologram.nudge(0, 1, 0);
        while (DOWN.consumeClick()) Hologram.nudge(0, -1, 0);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) { HologramRenderer.render(event); }
    @SubscribeEvent public static void recipeKey(net.minecraftforge.client.event.ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof com.syaru.mekpreview.client.design.DesignInputScreen editor
                && editor.mekpreview$focusedInput() != null) return;
        if (!(event.getScreen() instanceof dev.emi.emi.screen.RecipeScreen) || dev.emi.emi.api.EmiApi.isSearchFocused()
                || !OPEN.matches(event.getKeyCode(), event.getScanCode())) return;
        if (event.getScreen().getFocused() instanceof net.minecraft.client.gui.components.EditBox input && input.isFocused()) return;
        var hovered = dev.emi.emi.api.EmiApi.getHoveredStack(true).getRecipeContext();
        if (com.syaru.mekpreview.jemm.NovaRecipeLink.open(hovered)) event.setCanceled(true);
    }
    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Post event) {
        // Use a single vanilla overlay event, rather than drawing once per overlay.
        if (!event.getOverlay().id().toString().equals("minecraft:hotbar") || !Hologram.shown()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        var g = event.getGuiGraphics();
        int x = 8, y = 8;
        g.fill(x - 3, y - 3, x + 308, y + 73, 0xB0101722);
        g.drawString(mc.font, Hologram.settings().title(), x, y, 0x8FE7FF, false);
        g.drawString(mc.font, Component.translatable("mekpreview.progress", Hologram.correct(), Hologram.total(),
                Hologram.missing(), Hologram.wrong()), x, y + 12, 0xFFFFFF, false);
        g.drawString(mc.font, Component.translatable("mekpreview.hologram_controls",
                ANCHOR.getTranslatedKeyMessage(), TOGGLE.getTranslatedKeyMessage(), ROTATE.getTranslatedKeyMessage()),
                x, y + 24, 0xCCD6E4, false);
        g.drawString(mc.font, Component.translatable(Hologram.anchored() ? "mekpreview.anchored" : "mekpreview.following",
                Hologram.origin().getX(), Hologram.origin().getY(), Hologram.origin().getZ()), x, y + 36, 0xCCD6E4, false);
        g.drawString(mc.font, Component.translatable(Hologram.settings().layerMode.translationKey())
                .append(" / " + (Hologram.settings().layer + 1)), x, y + 48, 0xCCD6E4, false);
        g.drawString(mc.font, Component.translatable("mekpreview.opacity", Math.round(Hologram.opacity() * 100),
                OPACITY.getTranslatedKeyMessage()), x, y + 60, 0xCCD6E4, false);
        if (com.syaru.mekpreview.ClientOptions.MATERIAL_HUD.get() && !ConstructionMaterials.rows().isEmpty()) {
            var rows = ConstructionMaterials.rows(); int top = y + 82;
            int count = Math.min(5, rows.size());
            g.fill(x - 3, top - 3, x + 308, top + 14 + count * 18 + (rows.size() > count ? 12 : 0), 0xB0101722);
            g.drawString(mc.font, Component.translatable("mekpreview.build.materials"), x, top, 0x8FE7FF, false);
            for (int i = 0; i < count; i++) {
                var row = rows.get(i); int ry = top + 13 + i * 18;
                var stack = BlockResolver.item(row.itemId()); g.renderItem(stack, x, ry);
                g.drawString(mc.font, mc.font.plainSubstrByWidth(stack.getHoverName().getString(), 175), x + 20, ry + 4, 0xFFFFFF, false);
                g.drawString(mc.font, Component.translatable("mekpreview.build.material_row", row.remaining(), row.available(), row.missing()),
                        x + 198, ry + 4, row.missing() > 0 ? 0xFF7979 : 0x9FE0A3, false);
            }
            if (rows.size() > count) g.drawString(mc.font, Component.translatable("mekpreview.build.more", rows.size() - count),
                    x, top + 14 + count * 18, 0xCCD6E4, false);
        }
        var target = Hologram.target();
        if (target != null) {
            var stack = new net.minecraft.world.item.ItemStack(target.expected().getBlock());
            int tx = event.getWindow().getGuiScaledWidth() / 2 - 112;
            int ty = event.getWindow().getGuiScaledHeight() / 2 + 26;
            g.fill(tx - 4, ty - 4, tx + 228, ty + 59, 0xDA101722);
            g.renderItem(stack, tx, ty);
            g.drawString(mc.font, stack.getHoverName(), tx + 22, ty + 3, 0xFFFFFF, false);
            g.drawString(mc.font, Component.translatable(target.wrong() ? "mekpreview.target_wrong" : "mekpreview.target_missing",
                    target.position().getX(), target.position().getY(), target.position().getZ()),
                    tx, ty + 22, target.wrong() ? 0xFF7979 : 0x8FE7FF, false);
            g.drawString(mc.font, Component.translatable(BuildControls.serverSupported() ? "mekpreview.build.hint" : "mekpreview.build.manual_hint"), tx, ty + 34, 0xCCD6E4, false);
            g.drawString(mc.font, PICK.isUnbound() ? Component.translatable("mekpreview.pick.bind_hint")
                    : Component.translatable("mekpreview.pick.hint", PICK.getTranslatedKeyMessage()), tx, ty + 46, 0xCCD6E4, false);
        }
    }
    @Mod.EventBusSubscriber(modid = MekPreview.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
            for (KeyMapping key : List.of(OPEN, ANCHOR, TOGGLE, ROTATE, OPACITY, CLEAR, PICK, LAYER_UP, LAYER_DOWN,
                    LAYER_MODE, WEST, EAST, NORTH, SOUTH, UP, DOWN)) event.register(key);
        }
        @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                    manager -> HologramRenderer.resourceReload());
        }
    }
}
