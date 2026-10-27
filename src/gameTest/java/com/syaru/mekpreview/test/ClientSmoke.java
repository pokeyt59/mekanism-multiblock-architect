package com.syaru.mekpreview.test;

import com.syaru.mekpreview.client.*;
import com.syaru.mekpreview.jei.*;
import com.syaru.mekpreview.jemm.*;
import com.syaru.mekpreview.structure.*;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import giselle.jei_mekanism_multiblocks.client.jei.category.DynamicTankCategory.DynamicTankWidget;
import giselle.jei_mekanism_multiblocks.client.jei.category.FissionReactorCategory.FissionReactorCategoryWidget;
import giselle.jei_mekanism_multiblocks.client.jei.category.TurbineCategory.TurbineWidget;
import giselle.jei_mekanism_multiblocks.client.jei.ListLineWidget;
import giselle.jei_mekanism_multiblocks.client.jei.ResultWidget;
import giselle.jei_mekanism_multiblocks.common.util.VolumeTextHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Files;
import java.util.*;

/** Isolated development client: exercises actual JEI controls against authoritative JEMM data. */
@Mod.EventBusSubscriber(modid = "mekpreview_test", value = Dist.CLIENT)
public final class ClientSmoke {
    private static boolean requested, finished;
    private static int tick, widthBefore;
    private static String previousOutput;
    private static int viewCaptures, hologramBuilds;
    private static PreviewScene layerScene;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("mekpreview.visualSmoke") || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (mc.screen instanceof AccessibilityOnboardingScreen intro) { intro.onClose(); return; }
            if (!requested && mc.screen instanceof TitleScreen) {
                requested = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.guiScale().set(2);
                mc.options.renderDistance().set(6);
                mc.options.simulationDistance().set(5);
                mc.options.languageCode = "en_us";
                mc.options.save();
                mc.resizeDisplay();
                var flows = new WorldOpenFlows(mc, mc.getLevelSource());
                String worldName = "MekPreviewJEIQA";
                if (Files.exists(mc.gameDirectory.toPath().resolve("saves/" + worldName + "/level.dat")))
                    flows.loadLevel(mc.screen, worldName);
                else flows.createFreshLevel(worldName,
                        new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42, false, false), WorldPresets::createNormalWorldDimensions);
                return;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            tick++;
            if (Boolean.getBoolean("mekpreview.hudSmoke")) {
                MaterialHudSmoke.tick(mc, tick);
                if (tick == 240) finish(mc, "PASS: material HUD displays more than five types, wraps names, scrolls to the final item, clamps after resizing and resets with layer/plan/world changes.");
                return;
            }
            if (tick == 150 && Boolean.getBoolean("mekpreview.placementSmoke")) tick = 1099;
            if (tick == 20) {
                mc.setScreen(null);
                mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    for (int x = -3; x < 18; x++) for (int z = -5; z < 18; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 179, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        for (int y = 180; y < 200; y++)
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    player.setGameMode(GameType.CREATIVE);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.connection.teleport(4, 185, -9, 0, 24);
                    level.setDayTime(6000);
                    level.setWeatherParameters(100000, 0, false, false);
                });
            }
            if (tick == 150) {
                require(MekaPlanJeiPlugin.runtime().getRecipeManager().createRecipeCategoryLookup().get()
                                .noneMatch(c -> c.getRecipeType().getUid().getNamespace().equals("jei_mekanism_multiblocks")),
                        "JEMM equipment was not merged into one category");
                require(linkedRecipes().size() >= 8, "Missing JEMM entries: " + linkedRecipes().size());
                show(StructureKind.DYNAMIC_TANK);
            }
            if (tick == 170) { verify(mc); capture(mc, "01-jemm-linked-gt-preview.png"); }
            if (tick == 180) preview(mc).model().setDimensionWidth(5);
            if (tick == 200) {
                require(find(preview(mc), "jemm-settings").isVisible(), "Native controls must stay beside the scene");
                capture(mc, "02-jemm-native-settings.png");
                widthBefore = preview(mc).model().getDimensionWidth();
                clickNativeWidth(mc);
            }
            if (tick == 210) {
                var model = (DynamicTankWidget) preview(mc).model();
                require(model.getDimensionWidth() == widthBefore + 1, "Native width click was lost or applied twice");
                model.setDimensionWidth(7); model.setDimensionHeight(6); model.setDimensionLength(9);
                model.setValveCount(5);
            }
            if (tick == 230) {
                verify(mc);
                require(metric(preview(mc), "fluid_tank").getValueLabel().getMessage().getString().equals(
                        VolumeTextHelper.formatMB(7L * 6 * 9 * MekanismConfig.general.dynamicTankFluidPerTank.get()).getString()),
                        "Tank performance did not follow live dimensions");
                capture(mc, "03-jemm-live-editing.png");
            }
            if (tick == 250) {
                verify(mc);
                require(preview(mc).snapshot().dimensions().equals(new Dimensions(7, 6, 9)), "JEMM dimensions stale");
                require(count(preview(mc).snapshot(), "dynamic_valve") == 5, "JEMM valve count lost");
                capture(mc, "04-jemm-synchronized-preview.png");
            }
            if (tick == 260) {
                viewCaptures = preview(mc).captureCount(); layerScene = preview(mc).scene();
                press(mc, "layer"); press(mc, "layer");
            }
            if (tick == 280) {
                var root = preview(mc);
                require(root.settings().layerMode == LayerMode.SINGLE && root.settings().layer == 1, "Layer control failed");
                require(root.scene().getCore().stream().allMatch(p -> p.getY() == 1), "Hidden layers still rendered");
                require(root.captureCount() == viewCaptures, "Layer view unnecessarily recaptured the native design");
                require(root.scene() == layerScene && root.scene().isUseCache(), "Layer view did not reuse the GPU-cached scene");
                capture(mc, "05-jemm-single-layer.png");
            }
            if (tick >= 300 && tick < 620 && (tick - 300) % 40 == 0)
                show(StructureKind.values()[(tick - 300) / 40]);
            if (tick >= 320 && tick < 640 && (tick - 320) % 40 == 0) {
                verify(mc);
                capture(mc, "jemm-" + StructureKind.values()[(tick - 320) / 40].id() + ".png");
            }
            if (tick == 630) show(StructureKind.FISSION_REACTOR);
            if (tick == 650) {
                var model = (FissionReactorCategoryWidget) preview(mc).model();
                var layout = new FissionReactorCategoryWidget.Layout(3, 18, 3, 18, 4, 18);
                layout.setWidth(7); layout.setLength(5); layout.setHeight(8);
                layout.clearPillars(); layout.setPillar(1, 1, 3); layout.setPillar(5, 3, 1);
                model.setAdvancedLayout(layout); model.setAdvanced(true);
                model.setPortCount(4); model.setLogicAdapterCount(1);
            }
            if (tick == 670) capture(mc, "06-jemm-advanced-fission-settings.png");
            if (tick == 690) {
                verify(mc);
                require(preview(mc).snapshot().dimensions().equals(new Dimensions(7, 8, 5)), "Advanced dimensions lost");
                require(count(preview(mc).snapshot(), "fission_fuel_assembly") == 4, "Advanced fuel positions lost");
                capture(mc, "07-jemm-advanced-fission-preview.png");
            }
            if (tick == 700) {
                press(mc, "project");
                require(mc.screen == null && Hologram.active(), "Project control did not start hologram");
                require(mc.player.containerMenu == mc.player.inventoryMenu, "Projection left a recipe-viewer parent container open");
                var origin = Hologram.origin();
                Hologram.nudge(-origin.getX(), 180 - origin.getY(), -origin.getZ());
                var first = Hologram.ghosts().get(0); var second = Hologram.ghosts().get(1);
                mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    level.setBlockAndUpdate(Hologram.origin().offset(first.local()), first.state());
                    level.setBlockAndUpdate(Hologram.origin().offset(second.local()), Blocks.STONE.defaultBlockState());
                });
            }
            if (tick == 730) {
                require(Hologram.correct() >= 1 && Hologram.wrong() >= 1 && Hologram.missing() > 0, "Placement detection failed");
                require(Hologram.ghosts().stream().filter(g -> BuiltInRegistries.BLOCK.getKey(g.state().getBlock())
                        .getPath().equals("fission_fuel_assembly")).count() == 4, "Projection does not match JEMM fuel layout");
                capture(mc, "08-jemm-world-hologram.png");
                hologramBuilds = HologramRenderer.cacheBuildCount();
            }
            if (tick == 740) {
                require(HologramRenderer.cacheBuildCount() == hologramBuilds, "Unchanged hologram rebuilt its mesh");
                Hologram.rotate(); Hologram.nudge(1, 0, 0); Hologram.layer(-1);
            }
            if (tick == 760) capture(mc, "09-jemm-rotated-layer-hologram.png");
            if (tick == 780) {
                var nova = linkedRecipes().stream().filter(r -> JemmIntegration.model(r) instanceof NovaMultiblockWidget).toList();
                var thermal = linkedRecipes().stream().filter(r -> MtePreview.supports(JemmIntegration.model(r))).toList();
                if (net.minecraftforge.fml.ModList.get().isLoaded("mekanism_more_multiblock")) {
                    require(!nova.isEmpty(), "Nova structures missing from unified category");
                    for (var recipe : nova) {
                        var model = JemmIntegration.model(recipe); model.initialize();
                        verifyMaterialGeometry(JemmBlueprint.capture(model));
                    }
                    System.out.println("MEKPREVIEW_NOVA_STRUCTURES_VERIFIED " + nova.size());
                    NovaSmoke.verify(nova.stream().map(r -> (NovaMultiblockWidget) JemmIntegration.model(r)).toList());
                    MekaPlanJeiPlugin.show(nova.get(0));
                }
                if (net.minecraftforge.fml.ModList.get().isLoaded("morethermalevaporation")) {
                    require(thermal.stream().filter(r -> JemmIntegration.model(r).getClass().getName()
                            .startsWith("morethermalevaporation.")).count() == 5, "Expected all 5 MTE base tiers: "
                            + thermal.stream().map(r -> JemmIntegration.model(r).getClass().getName()).toList());
                    for (var recipe : thermal) {
                        var model = JemmIntegration.model(recipe); model.initialize();
                        verifyMaterialGeometry(JemmBlueprint.capture(model));
                    }
                    System.out.println("MEKPREVIEW_MTE_TIERS_VERIFIED " + thermal.size());
                }
            }
            if (tick == 790 && previewIfRecipe(mc) != null) {
                var root = preview(mc);
                if (root.model() instanceof NovaMultiblockWidget nova)
                    require(nova.calculated() != null, "Nova performance was not calculated");
                capture(mc, "10-nova-unified-preview.png");
            }
            if (tick >= 800 && tick < 900 && (tick - 800) % 20 == 0) {
                var thermal = linkedRecipes().stream().filter(r -> MtePreview.supports(JemmIntegration.model(r))).toList();
                if (!thermal.isEmpty()) MekaPlanJeiPlugin.show(thermal.get((tick - 800) / 20));
            }
            if (tick >= 810 && tick < 910 && (tick - 810) % 20 == 0 && previewIfRecipe(mc) != null
                    && MtePreview.supports(preview(mc).model())) {
                verifyMaterialGeometry(preview(mc).snapshot());
                require(!preview(mc).scene().getCore().isEmpty(), "MTE scene is empty");
                require(results(preview(mc)).visible, "MTE performance hidden on initial view");
                metric(preview(mc), "max_speed");
                capture(mc, "mte-tier-" + ((tick - 810) / 20) + ".png");
            }
            if (tick == 920 && net.minecraftforge.fml.ModList.get().isLoaded("morethermalevaporation")) {
                var recipe = linkedRecipes().stream().filter(r -> JemmIntegration.model(r).getClass().getName()
                        .equals("morethermalevaporation.client.jei.category.MoreEvaporationPlantCategory$UltimateEvaporationPlantWidget"))
                        .findFirst().orElseThrow();
                MekaPlanJeiPlugin.show(recipe);
                var model = JemmIntegration.model(recipe);
                model.initialize();
                previousOutput = metric(preview(mc), "max_speed").getValueLabel().getMessage().getString();
                model.getClass().getMethod("setUseLargeTypes", boolean.class).invoke(model, true);
                model.getClass().getMethod("setUseStructureUpgrade", boolean.class).invoke(model, true);
                model.setDimensionHeight(22);
            }
            if (tick == 950 && net.minecraftforge.fml.ModList.get().isLoaded("morethermalevaporation")) {
                require(preview(mc).snapshot().dimensions().equals(new Dimensions(9, 22, 9)), "MTE large/upgrade dimensions lost");
                verifyMaterialGeometry(preview(mc).snapshot());
                require(!metric(preview(mc), "max_speed").getValueLabel().getMessage().getString().equals(previousOutput),
                        "MTE performance did not follow large/upgrade/height edits");
                capture(mc, "mte-large-9x22x9.png");
            }
            if (tick == 980) {
                show(StructureKind.TURBINE);
                var model = (TurbineWidget) preview(mc).model();
                model.setDimensionWidth(7); model.setDimensionHeight(10); model.setRotorCount(2);
            }
            if (tick == 1000) {
                verifyTurbineOutput(mc);
                previousOutput = metric(preview(mc), "max_production").getValueLabel().getMessage().getString();
                capture(mc, "performance-01-before-size-edit.png");
                clickNativeWidth(mc);
            }
            if (tick == 1020) {
                verifyTurbineOutput(mc);
                require(preview(mc).snapshot().dimensions().width() == 9, "Turbine size edit did not reach scene");
                var output = metric(preview(mc), "max_production").getValueLabel().getMessage().getString();
                require(!output.equals(previousOutput), "Turbine output stale after native dimension click");
                previousOutput = output;
                capture(mc, "performance-02-after-size-edit.png");
                ((TurbineWidget) preview(mc).model()).setRotorCount(1);
            }
            if (tick == 1002 || tick == 1022) verifyTurbineOutput(mc);
            if (tick == 1040) {
                verifyTurbineOutput(mc);
                require(!metric(preview(mc), "max_production").getValueLabel().getMessage().getString().equals(previousOutput),
                        "Turbine output stale after internal composition edit");
                capture(mc, "performance-03-after-rotor-edit.png");
                preview(mc).model().showCostPanel();
            }
            if (tick == 1060) {
                require(!results(preview(mc)).visible, "Materials switch failed");
                verify(mc, false);
                capture(mc, "performance-04-materials.png");
                preview(mc).model().showResultPanel();
            }
            if (tick == 1080) { verifyTurbineOutput(mc); capture(mc, "performance-05-restored.png"); }
            if (tick == 1100) {
                mc.setScreen(null);
                // The recipe viewer may have a creative-inventory parent. End that menu
                // before switching this fixture to survival, as a normal UI close would.
                mc.player.closeContainer();
                mc.mouseHandler.releaseMouse();
                var blocks = new LinkedHashMap<Cell, Part>();
                for (int i = 0; i < 3; i++) blocks.put(new Cell(i, 0, 0), new Part("minecraft:stone", false));
                var blueprint = new Blueprint(StructureKind.DYNAMIC_TANK, new Dimensions(3, 1, 1), blocks, Map.of());
                var settings = new PreviewSettings(StructureKind.DYNAMIC_TANK);
                settings.dimensions = blueprint.dimensions(); settings.source = () -> blueprint;
                Hologram.start(settings);
                var origin = Hologram.origin(); Hologram.nudge(4 - origin.getX(), 183 - origin.getY(), -5 - origin.getZ());
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    // The placement camera stands outside the main preview platform.
                    // Keep survival gravity from moving it below the test ghosts.
                    for (int x = 3; x <= 5; x++) for (int z = -11; z <= -8; z++)
                        player.serverLevel().setBlockAndUpdate(new BlockPos(x, 179, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    player.setGameMode(GameType.SURVIVAL);
                    player.getInventory().clearContent();
                    player.getInventory().setItem(15, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE, 3));
                    player.inventoryMenu.broadcastChanges();
                    player.connection.teleport(4.5, 180, -9.5, 0, -23);
                });
            }
            if (tick == 1130) {
                mc.player.setYRot(0); mc.player.setXRot(-23);
                mc.player.setShiftKeyDown(false);
                Hologram.tick();
                require(Hologram.target() != null, "No target for right-click placement: eye=" + mc.player.getEyePosition()
                        + " look=" + mc.player.getLookAngle() + " origin=" + Hologram.origin());
                System.out.println("MEKPREVIEW_SINGLE_REQUEST target=" + Hologram.target().position()
                        + " inventory=" + mc.player.getInventory().getItem(15) + " shift=" + mc.player.isShiftKeyDown());
                var input = new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(1,
                        mc.options.keyUse, net.minecraft.world.InteractionHand.MAIN_HAND);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(input);
                require(input.isCanceled(), "Right click was not intercepted by build controls");
            }
            if (tick == 1145) mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                System.out.println("MEKPREVIEW_SINGLE_SERVER creative=" + player.getAbilities().instabuild
                        + " inventory=" + player.getInventory().getItem(15)
                        + " block=" + player.serverLevel().getBlockState(new BlockPos(4, 183, -5)));
            });
            if (tick == 1155) {
                require(mc.level.getBlockState(new BlockPos(4, 183, -5)).is(Blocks.STONE), "Single placement did not reach server");
                require(mc.player.getInventory().getItem(15).getCount() == 2, "Single placement did not consume one item: "
                        + mc.player.getInventory().getItem(15));
                BuildControls.request(true);
            }
            if (tick == 1195) {
                for (int i = 0; i < 3; i++) require(mc.level.getBlockState(new BlockPos(4 + i, 183, -5)).is(Blocks.STONE), "Batch placement incomplete");
                require(mc.player.getInventory().getItem(15).isEmpty(), "Batch material cost incorrect");
                capture(mc, "10-server-authoritative-placement.png");
            }
            if (tick == 1220 && Boolean.getBoolean("mekpreview.placementSmoke")) finish(mc, "PASS: isolated right-click network placement and inventory-consuming batch placement.");
            if (tick >= 1220 && !Boolean.getBoolean("mekpreview.placementSmoke")) DesignFeaturesSmoke.tick(mc, tick);
            if (tick >= 1480) ConstructionAssistSmoke.tick(mc, tick);
            if (tick == 1880) finish(mc, "PASS: vanilla inventory block selection and exact-NBT refill, placed material subtraction and visible-layer HUD, red incorrect block/orientation guides, once-only full-plan completion and recoverable configurable auto-hide; Nova recipe-name search and exact native JEI preview link; live whole/visible-layer inventory shortages; native settings and views persisted and restored across equipment; published Nova performance/recipes/upgrades/coils/parallel hardware; GPU-cached preview and hologram, layer switching without native recapture, static hologram cache reuse; live performance beside preview, native turbine size and rotor edits update output within two client ticks, MTE large-tier speed updates, materials switch preserved; unified equipment category including installed Nova/MTE structures, live settings beside 8 GT-style previews, native input exactly once, "
                    + "exact JEMM block states and costs, 7x6x9 tank with 5 valves, advanced 7x8x5 fission with 4 fuel blocks, "
                    + "layers, textured holograms, movement and rotation, actual right-click network placement and inventory-consuming batch placement.");
        } catch (Throwable e) {
            e.printStackTrace();
            finish(mc, "FAIL at tick " + tick + ": " + e);
        }
    }
    private static JemmPreviewWidget previewIfRecipe(Minecraft mc) throws Exception {
        return mc.screen instanceof IRecipesGui ? preview(mc) : null;
    }
    private static void verifyMaterialGeometry(Blueprint blueprint) {
        Map<String, Integer> geometry = new TreeMap<>();
        blueprint.blocks().values().forEach(p -> geometry.merge(p.blockId(), 1, Integer::sum));
        Map<String, Integer> blocks = new TreeMap<>();
        blueprint.materials().forEach((id, count) -> {
            if (BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(id)) instanceof net.minecraft.world.item.BlockItem)
                blocks.put(id, count);
        });
        require(geometry.equals(blocks), "Addon geometry/material mismatch: " + geometry + " vs " + blocks);
    }
    private static long count(Blueprint b, String id) {
        return b.blocks().values().stream().filter(p -> p.blockId().equals("mekanism:" + id)
                || p.blockId().equals("mekanismgenerators:" + id)).count();
    }
    private static List<PlannerRecipe> linkedRecipes() {
        return MekaPlanJeiPlugin.recipes();
    }
    private static void show(StructureKind kind) {
        MekaPlanJeiPlugin.show(linkedRecipes().stream().filter(r -> JemmIntegration.kind(JemmIntegration.model(r)) == kind)
                .filter(r -> JemmIntegration.model(r).getClass().getPackageName().equals("giselle.jei_mekanism_multiblocks.client.jei.category"))
                .findFirst().orElseThrow(() -> new IllegalStateException("No JEMM entry for " + kind)));
    }
    private static Widget find(Widget root, String id) {
        if (id.equals(root.getId())) return root;
        if (root instanceof com.lowdragmc.lowdraglib.gui.widget.WidgetGroup group)
            for (var child : group.widgets) { var found = find(child, id); if (found != null) return found; }
        return null;
    }
    static void press(Minecraft mc, String id) throws Exception {
        var page = page(mc);
        var control = Objects.requireNonNull(find(page.editor(), id), id);
        var pos = control.getPosition();
        double x = page.x() + pos.x + control.getSize().width / 2.0, y = page.y() + pos.y + control.getSize().height / 2.0;
        require(click(mc, x, y), "JEI did not route the click to " + id);
    }
    /** Mouse, key and text input the way Minecraft delivers it: Forge's screen events first (JEI listens there), then the screen. */
    static boolean click(Minecraft mc, double x, double y) {
        var screen = mc.screen;
        boolean handled = MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseButtonPressed.Pre(screen, x, y, 0)) || screen.mouseClicked(x, y, 0);
        // A click may close the screen (projection does); only release on a screen that is still open.
        if (mc.screen == screen && !MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseButtonReleased.Pre(screen, x, y, 0)))
            screen.mouseReleased(x, y, 0);
        return handled;
    }
    static boolean type(Minecraft mc, char character) {
        var screen = mc.screen;
        return MinecraftForge.EVENT_BUS.post(new ScreenEvent.CharacterTyped.Pre(screen, character, 0)) || screen.charTyped(character, 0);
    }
    static boolean key(Minecraft mc, int key) {
        var screen = mc.screen;
        return MinecraftForge.EVENT_BUS.post(new ScreenEvent.KeyPressed.Pre(screen, key, 0, 0)) || screen.keyPressed(key, 0, 0);
    }
    private static void clickNativeWidth(Minecraft mc) throws Exception {
        var root = preview(mc);
        var field = MultiblockWidget.class.getDeclaredField("widthWidget"); field.setAccessible(true);
        var slider = (giselle.jei_mekanism_multiblocks.client.gui.IntSliderWithButtons) field.get(root.model());
        var button = slider.getPlusButton();
        double x = button.getX() + button.getWidth() / 2.0, y = button.getY() + button.getHeight() / 2.0;
        for (var parent = button.getParent(); parent != null; parent = parent.getParent()) {
            x += parent.getX(); y += parent.getY();
        }
        var pos = find(root, "jemm-settings").getPosition(); var page = page(mc);
        x += page.x() + pos.x; y += page.y() + pos.y;
        require(click(mc, x, y), "Native JEMM width control did not receive click");
    }
    static JemmPreviewWidget preview(Minecraft mc) throws Exception {
        return page(mc).editor();
    }
    static PlannerCategory.Visible page(Minecraft mc) {
        require(mc.screen instanceof IRecipesGui, "Expected JEI, got " + mc.screen);
        return Objects.requireNonNull(PlannerCategory.visible(), "No planner page on screen");
    }
    private static void verify(Minecraft mc) throws Exception {
        verify(mc, true);
    }
    private static ListLineWidget results(JemmPreviewWidget root) throws Exception {
        var field = MultiblockWidget.class.getDeclaredField("resultsList"); field.setAccessible(true);
        return (ListLineWidget) field.get(root.model());
    }
    private static ResultWidget metric(JemmPreviewWidget root, String key) throws Exception {
        return results(root).getItems().getChildren().stream().filter(w -> w instanceof ResultWidget)
                .map(w -> (ResultWidget) w).filter(w -> w.getTextLabel().getMessage().getContents() instanceof TranslatableContents text
                        && text.getKey().equals("text.jei_mekanism_multiblocks.result." + key)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing calculated metric: " + key));
    }
    private static void verifyTurbineOutput(Minecraft mc) throws Exception {
        verify(mc);
        var root = preview(mc); var model = (TurbineWidget) root.model();
        var expected = Component.translatable("%s/t", EnergyDisplay.of(model.getMaxProduction(
                model.getLowerVolume(model.getRotorCount()), model.getBladeCount(model.getRotorCount()), model.getVentCount()))
                .getTextComponent()).getString();
        var actual = metric(root, "max_production").getValueLabel().getMessage().getString();
        require(expected.equals(actual), "Live output differs from JEMM: " + actual + " vs " + expected);
        System.out.println("MEKPREVIEW_PERFORMANCE_VERIFIED tick=" + tick + " output=" + actual);
    }
    private static void verify(Minecraft mc, boolean performanceVisible) throws Exception {
        var root = preview(mc);
        require(results(root).visible == performanceVisible, "Performance should be visible beside preview by default");
        require(!results(root).getItems().getChildren().isEmpty(), "Missing performance rows");
        var expected = root.model().createPreview().getBlockStates();
        require(expected.size() == root.snapshot().blocks().size(), "JEMM block count mismatch");
        root.snapshot().blocks().forEach((cell, part) -> require(BlockResolver.state(part, 0)
                .equals(expected.get(new BlockPos(cell.x(), cell.y(), cell.z()))), "JEMM state mismatch at " + cell));
        Map<String, Integer> costs = new TreeMap<>();
        root.model().getCosts().forEach(s -> costs.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum));
        require(costs.equals(root.snapshot().materials()), "JEMM costs mismatch");
        require(!root.scene().getCore().isEmpty(), "Empty LDLib scene");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> System.out.println("MEKPREVIEW_SCREENSHOT " + name));
    }
    private static void finish(Minecraft mc, String result) {
        finished = true;
        System.out.println("MEKPREVIEW_VISUAL_RESULT " + result);
        try { Files.writeString(mc.gameDirectory.toPath().resolve("visual-result.txt"), result); }
        catch (Exception e) { throw new IllegalStateException(e); }
        mc.stop();
    }
}
