package com.syaru.mekpreview.test;

import com.syaru.mekpreview.ClientOptions;
import com.syaru.mekpreview.client.*;
import com.syaru.mekpreview.structure.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import java.util.*;

/** Real HUD rendering at two GUI scales; never opens the user's Prism world. */
final class MaterialHudSmoke {
    private static List<String> materials;
    static void tick(Minecraft mc, int tick) throws Exception {
        if (tick == 20) {
            mc.player.closeContainer(); mc.setScreen(null); mc.mouseHandler.releaseMouse();
            ClientOptions.HIDE_COMPLETED.set(false); ClientOptions.MATERIAL_HUD.set(true);
            materials = BuiltInRegistries.BLOCK.stream().filter(block -> block.asItem() != Items.AIR)
                    .map(BuiltInRegistries.BLOCK::getKey).filter(id -> id.getNamespace().equals("mekanism"))
                    .map(Object::toString).sorted().limit(24).toList();
            require(materials.size() == 24, "Missing HUD fixture materials");
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.getAbilities().flying = true; player.onUpdateAbilities();
                player.connection.teleport(4.5, 180, -9.5, 0, -23);
            });
            start(mc, 8);
        }
        if (tick == 45) {
            require(ConstructionMaterials.rows().size() == 8, "Material source lost rows");
            require(MaterialHud.viewport().end() > 5, "HUD still truncates at five materials");
            ClientSmoke.capture(mc, "hud-01-more-than-five.png");
        }
        if (tick == 55) start(mc, 24);
        if (tick == 40 || tick == 75) { mc.player.setYRot(0); mc.player.setXRot(-23); Hologram.tick(); }
        if (tick == 80) {
            require(MaterialHud.viewport().scrollable(), "Overflow has no scroll access");
            require(MaterialHud.scroll(-1000), "HUD did not consume material-list scroll");
        }
        if (tick == 90) {
            var view = MaterialHud.viewport();
            require(view.end() == 24 && view.offset() + view.height() == view.contentHeight(), "Final material is not fully reachable");
            require(ConstructionMaterials.rows().size() == 24, "Scrolling changed material quantities");
            require(Hologram.target() != null, "No target card in HUD overlap test");
            verifyTargetCard(mc);
            ClientSmoke.capture(mc, "hud-02-final-material.png");
            mc.options.guiScale().set(3); mc.resizeDisplay();
        }
        if (tick == 110) { require(MaterialHud.scroll(-1000), "Small-screen scroll failed"); }
        if (tick == 120) {
            var view = MaterialHud.viewport();
            require(view.end() == 24 && view.height() > 0, "Small GUI scale lost last material");
            verifyTargetCard(mc);
            int hudWidth = mc.getWindow().getGuiScaledWidth() - 264;
            require(mc.font.width(net.minecraft.network.chat.Component.translatable("mekpreview.build.materials_compact")) <= hudWidth,
                    "Small-screen header escapes the list");
            require(mc.font.width(net.minecraft.network.chat.Component.translatable("mekpreview.build.material_page",
                    view.first() + 1, view.end(), 24)) <= hudWidth, "Scroll footer escapes the list");
            ClientSmoke.capture(mc, "hud-03-small-gui-final.png");
            Hologram.cycleLayerMode();
        }
        if (tick == 140) {
            require(MaterialHud.viewport().offset() == 0, "Layer change retained stale scroll position");
            mc.options.guiScale().set(2); mc.resizeDisplay(); start(mc, 6);
        }
        if (tick == 165) {
            var view = MaterialHud.viewport();
            require(view.offset() == 0 && view.end() == 6 && !view.scrollable(), "Shorter design was cut off after a scrolled design");
            require(!MaterialHud.scroll(-1), "A fitting list intercepted normal scrolling");
            ClientSmoke.capture(mc, "hud-04-six-materials-fit.png");
            Hologram.toggle();
            require(!MaterialHud.scroll(-1), "Hidden hologram consumed scroll");
            Hologram.toggle();
            mc.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            require(!MaterialHud.scroll(-1), "GUI-open scroll was intercepted"); mc.setScreen(null);
        }
        if (tick == 190) { Hologram.clear(); ConstructionMaterials.tick(); }
        if (tick == 210) {
            require(ConstructionMaterials.rows().isEmpty(), "Cleared hologram retained HUD rows");
            start(mc, 24);
        }
        if (tick == 230) {
            require(MaterialHud.viewport().offset() == 0, "New projection inherited an old scroll offset");
            Hologram.clear();
            System.out.println("MEKPREVIEW_MATERIAL_HUD_VERIFIED 24 materials, final row, wrapped names, GUI scales 2/3, layer/plan reset and input guards");
        }
    }
    private static void start(Minecraft mc, int count) {
        var blocks = new LinkedHashMap<Cell, Part>();
        for (int i = 0; i < count; i++) blocks.put(new Cell(i % 6, i / 6, 0), new Part(materials.get(i), false));
        var plan = new Blueprint(StructureKind.DYNAMIC_TANK, new Dimensions(6, 4, 1), blocks, Map.of());
        var settings = new PreviewSettings(StructureKind.DYNAMIC_TANK);
        settings.source = () -> plan; settings.dimensions = plan.dimensions(); settings.layer = 3;
        Hologram.start(settings);
        var origin = Hologram.origin(); Hologram.nudge(4 - origin.getX(), 183 - origin.getY(), -5 - origin.getZ());
        ConstructionMaterials.clear(); ConstructionMaterials.tick();
    }
    private static void verifyTargetCard(Minecraft mc) {
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 26;
        int x = MaterialHud.targetX(mc.getWindow().getGuiScaledWidth() / 2 - 112, y);
        require(!MaterialHud.overlaps(x - 4, y - 4, 232, 63), "Target card hides HUD materials");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
