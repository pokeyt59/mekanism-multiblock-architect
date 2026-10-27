package com.syaru.mekpreview.jemm;

import com.syaru.mekpreview.client.BlockResolver;
import com.syaru.mekpreview.jei.MekaPlanJeiPlugin;
import com.syaru.mekpreview.jei.PlannerCategory;
import com.syaru.mekpreview.jei.PlannerRecipe;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.IIconButtonController;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Exact native object or recipe-ID matching, never a fuzzy output-only match. */
public final class NovaRecipeLink {
    public record Link(PlannerRecipe equipment, NovaMultiblockWidget model, int recipeIndex) {}
    private static final String PRIMITIVE = "com.falcon2235.moremultiblock.client.jei.PbfRecipeCategory$PbfDisplayRecipe";
    private NovaRecipeLink() {}

    /** Nova machines that can run this JEI recipe, matched by the recipe object or its ID. */
    public static List<Link> find(@Nullable Object raw, @Nullable ResourceLocation id) {
        if (raw == null) return List.of();
        var links = new ArrayList<Link>();
        for (var recipe : MekaPlanJeiPlugin.recipes()) {
            if (recipe.model() instanceof NovaMultiblockWidget nova) {
                int index = raw.getClass().getName().equals(PRIMITIVE) && nova.performance().kind().equals("PBF")
                        ? 0 : nova.performance().findRecipe(raw, id);
                if (index >= 0) links.add(new Link(recipe, nova, index));
            }
        }
        return List.copyOf(links);
    }

    /** Selects the linked operation on the Nova design and shows it in the planner. */
    public static boolean open(Link link) {
        link.model().setRecipeIndex(link.recipeIndex());
        link.model().markNeedUpdate();
        return MekaPlanJeiPlugin.show(link.equipment());
    }

    /** Adds an equipment-preview button beside every JEI recipe a Nova machine can run (JEI 15.38+). */
    public static final class ButtonFactory implements IRecipeButtonControllerFactory {
        @Override @Nullable
        public <T> IIconButtonController createButtonController(IRecipeLayoutDrawable<T> layout) {
            var category = layout.getRecipeCategory();
            if (category.getRecipeType().equals(PlannerCategory.TYPE)) return null;
            T recipe = layout.getRecipe();
            var links = find(recipe, category.getRegistryName(recipe));
            return links.isEmpty() ? null : new Button(links.get(0));
        }
    }

    private record Button(Link link) implements IIconButtonController {
        private static final ItemStack ICON = BlockResolver.item("mekanism:dynamic_tank");
        @Override public boolean onPress(IJeiUserInput input) {
            if (!input.isSimulate()) open(link);
            return true;
        }
        @Override public void getTooltips(ITooltipBuilder tooltip) {
            tooltip.add(Component.translatable("mekpreview.search.preview"));
            tooltip.add(Component.literal(link.equipment().title()).withStyle(ChatFormatting.GRAY));
        }
        @Override public void drawExtras(GuiGraphics graphics, Rect2i area, int mouseX, int mouseY, float partialTicks) {
            // A half-size equipment icon fits JEI's small side buttons.
            graphics.pose().pushPose();
            graphics.pose().translate(area.getX() + (area.getWidth() - 8) / 2f, area.getY() + (area.getHeight() - 8) / 2f, 0);
            graphics.pose().scale(0.5f, 0.5f, 1);
            graphics.renderItem(ICON, 0, 0);
            graphics.pose().popPose();
        }
    }
}
