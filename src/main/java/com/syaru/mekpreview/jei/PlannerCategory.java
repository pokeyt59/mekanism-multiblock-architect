package com.syaru.mekpreview.jei;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.syaru.mekpreview.MekPreview;
import com.syaru.mekpreview.client.BlockResolver;
import com.syaru.mekpreview.client.design.SidePanel;
import com.syaru.mekpreview.jemm.JemmPreviewWidget;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The single "Mekanism multiblocks" JEI category. Each page hosts MekaPlan's LDLib editor
 * around a live JEMM design, the same widget tree the EMI version embedded.
 *
 * <p>LDLib's stock {@code ModularUIRecipeCategory} is not used: its cache drops editors without
 * firing close listeners, which would leak the preview's GPU buffers, and it re-initialises the
 * whole widget tree every time JEI rebuilds a layout. Here one editor lives per page, is found
 * again by recipe on every call, and is disposed once JEI stops drawing it.</p>
 */
public final class PlannerCategory implements IRecipeCategory<PlannerRecipe> {
    public static final RecipeType<PlannerRecipe> TYPE = RecipeType.create(MekPreview.MOD_ID, "multiblocks", PlannerRecipe.class);
    /** Editors JEI has not drawn for this long give their GPU caches back. */
    private static final long IDLE_NANOS = 10_000_000_000L;
    /** An editor drawn within this window still accepts typed text. */
    private static final long VISIBLE_NANOS = 1_000_000_000L;
    /** An editor drawn within this window is on the page JEI shows now. */
    private static final long SHOWN_NANOS = 250_000_000L;
    private static final Map<PlannerRecipe, Page> PAGES = new IdentityHashMap<>();
    private final IDrawable icon;

    PlannerCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(BlockResolver.item("mekanism:dynamic_tank"));
    }

    @Override public RecipeType<PlannerRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("mekpreview.category"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return JemmPreviewWidget.WIDTH; }
    @Override public int getHeight() { return JemmPreviewWidget.HEIGHT; }
    @Override @Nullable public ResourceLocation getRegistryName(PlannerRecipe recipe) { return recipe.id(); }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PlannerRecipe recipe, IFocusGroup focuses) {
        // JEMM's costs keep "show uses" working for casings, valves and so on.
        // Nothing is drawn by JEI itself: the editor shows materials in its own panel.
        var model = recipe.model();
        model.initialize();
        var costs = model.getCosts();
        if (!costs.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(costs);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, PlannerRecipe recipe, IFocusGroup focuses) {
        builder.addGuiEventListener(new Input(recipe));
        builder.addWidget(new Foreground(recipe));
    }

    @Override
    public void draw(PlannerRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var page = page(recipe);
        var pose = graphics.pose().last().pose();
        page.originX = Math.round(pose.m30());
        page.originY = Math.round(pose.m31());
        page.lastDrawn = System.nanoTime();
        page.ui.draw(graphics, (int) mouseX, (int) mouseY, Minecraft.getInstance().getFrameTime());
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, PlannerRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        var page = PAGES.get(recipe);
        if (page == null) return;
        if (page.ui.tooltipTexts != null && !page.ui.tooltipTexts.isEmpty()) tooltip.addAll(page.ui.tooltipTexts);
        if (page.ui.tooltipComponent != null) tooltip.add(page.ui.tooltipComponent);
    }

    /** The editor text box being typed into, if a planner page on screen has one focused. */
    @Nullable public static SidePanel.Input focusedInput() {
        long now = System.nanoTime();
        for (var page : PAGES.values()) {
            if (now - page.lastDrawn > VISIBLE_NANOS) continue;
            var input = page.ui.getWidget().focusedInput();
            if (input != null) return input;
        }
        return null;
    }

    /** Client tick: free editors JEI stopped drawing, or all of them once no screen is open. */
    public static void tick(boolean screenOpen) {
        if (PAGES.isEmpty()) return;
        long now = System.nanoTime();
        PAGES.values().removeIf(page -> {
            if (screenOpen && now - page.lastDrawn < IDLE_NANOS) return false;
            page.dispose();
            return true;
        });
    }

    public static void disposeAll() {
        PAGES.values().forEach(Page::dispose);
        PAGES.clear();
    }

    /** The top planner page JEI is showing and its top-left corner on screen; for tests and diagnostics. */
    @Nullable public static Visible visible() {
        long now = System.nanoTime();
        Page top = null;
        for (var page : PAGES.values()) {
            if (now - page.lastDrawn > SHOWN_NANOS) continue;
            if (top == null || page.originY < top.originY || page.originY == top.originY && page.originX < top.originX) top = page;
        }
        return top == null ? null : new Visible(top.ui.getWidget(), top.originX, top.originY);
    }

    public record Visible(JemmPreviewWidget editor, int x, int y) {}

    private static Page page(PlannerRecipe recipe) {
        return PAGES.computeIfAbsent(recipe, Page::new);
    }

    private static final class Page {
        final ModularWrapper<JemmPreviewWidget> ui;
        long lastDrawn = System.nanoTime();
        int originX, originY;

        Page(PlannerRecipe recipe) {
            ui = new ModularWrapper<>(new JemmPreviewWidget(recipe.model(), recipe.title()));
            ui.setRecipeWidget(0, 0);
        }

        void dispose() { ui.modularUI.triggerCloseListeners(); }
    }

    /** JEI routes mouse input here in page coordinates. Typed text arrives through ClientEvents instead. */
    private record Input(PlannerRecipe recipe) implements IJeiGuiEventListener {
        @Override public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, JemmPreviewWidget.WIDTH, JemmPreviewWidget.HEIGHT);
        }
        @Override public void mouseMoved(double x, double y) { page(recipe).ui.mouseMoved(x, y); }
        @Override public boolean mouseClicked(double x, double y, int button) { return page(recipe).ui.mouseClicked(x, y, button); }
        @Override public boolean mouseReleased(double x, double y, int button) { return page(recipe).ui.mouseReleased(x, y, button); }
        @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
            return page(recipe).ui.mouseDragged(x, y, button, dx, dy);
        }
        @Override public boolean mouseScrolled(double x, double y, double delta) { return page(recipe).ui.mouseScrolled(x, y, delta); }
        @Override public boolean keyPressed(double x, double y, int key, int scan, int modifiers) {
            // Straight to the widgets: the wrapper's own screen fallback would treat E or Esc as "close".
            return page(recipe).ui.modularUI.mainGroup.keyPressed(key, scan, modifiers);
        }
    }

    /** Foreground and overlays above JEI's slots, as LDLib's own JEI widget draws them. */
    private record Foreground(PlannerRecipe recipe) implements IRecipeWidget {
        @Override public ScreenPosition getPosition() { return new ScreenPosition(0, 0); }
        @Override public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            var page = PAGES.get(recipe);
            if (page == null) return;
            var main = page.ui.modularUI.mainGroup;
            float partial = Minecraft.getInstance().getFrameTime();
            graphics.pose().pushPose();
            graphics.pose().translate(-page.ui.getLeft(), -page.ui.getTop(), 0);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            main.drawInForeground(graphics, (int) mouseX, (int) mouseY, partial);
            main.drawOverlay(graphics, (int) mouseX, (int) mouseY, partial);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            graphics.pose().popPose();
        }
    }
}
