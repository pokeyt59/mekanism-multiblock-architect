package com.syaru.mekpreview.jemm;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.syaru.mekpreview.client.*;
import com.syaru.mekpreview.client.design.*;
import com.syaru.mekpreview.structure.*;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** JEMM owns the design and calculations; this widget owns only its presentation. */
public final class JemmPreviewWidget extends WidgetGroup {
    public static final int WIDTH = 366, HEIGHT = 142;
    private final MultiblockWidget model;
    private final PreviewSettings settings;
    private final JemmSettingsBridge nativeControls;
    private final WidgetGroup preview = new WidgetGroup(0, 0, 180, HEIGHT);
    private final ButtonWidget layer;
    private Blueprint snapshot;
    private PreviewScene scene;
    private CompoundTag lastState;
    private boolean dirty;
    private boolean modelDirty, geometryChanged;
    private int captures;
    private final MaterialsPanel materials;
    private final FavoritesPanel favorites;
    private final NovaSearchPanel search;
    private String panel = "";

    public JemmPreviewWidget(MultiblockWidget model, String title) {
        super(0, 0, WIDTH, HEIGHT);
        setClientSideWidget();
        this.model = model;
        nativeControls = new JemmSettingsBridge(model);
        nativeControls.setId("jemm-settings");
        addWidget(nativeControls);
        settings = new PreviewSettings(JemmIntegration.kind(model));
        settings.displayName = title;
        snapshot = JemmBlueprint.capture(model);
        settings.dimensions = snapshot.dimensions();
        settings.layer = settings.dimensions.height() - 1;
        settings.source = () -> snapshot;
        applyView(FavoriteClient.view(model));
        settings.layer = Math.min(settings.layer, settings.dimensions.height() - 1);
        lastState = state();
        dirty = modelDirty = true;

        addWidget(preview);
        preview.addWidget(new ImageWidget(3, 3, 174, 118, new ColorRectTexture(0xFF202020)));
        preview.addWidget(new ImageWidget(5, 5, 145, 11, new TextTexture(title).setWidth(145)
                .setType(TextTexture.TextType.ROLL).setDropShadow(true)));
        layer = button(preview, "layer", 158, 25, 16, 16,
                () -> settings.layerMode == LayerMode.ALL ? "A" : Integer.toString(settings.layer + 1),
                this::cycleLayer);
        button(preview, "mode", 158, 45, 16, 16,
                () -> switch (settings.layerMode) { case ALL -> "A"; case SINGLE -> "1"; case BELOW -> "<"; },
                d -> { settings.layerMode = settings.layerMode.next(); dirty = true; })
                .setHoverTooltips(Component.translatable("mekpreview.control.mode"));
        button(preview, "shell", 158, 65, 16, 16, () -> "S",
                d -> { settings.hideShell = !settings.hideShell; dirty = true; })
                .setHoverTooltips(Component.translatable("mekpreview.control.shell"));
        button(preview, "rotate", 158, 85, 16, 16, () -> "R",
                d -> { settings.rotation = Math.floorMod(settings.rotation + d, 4); dirty = true; })
                .setHoverTooltips(Component.translatable("mekpreview.rotate"));
        button(preview, "project", 3, 123, 174, 17,
                () -> Component.translatable("mekpreview.project").getString(), d -> {
                    // Freeze the exact displayed JEMM design; subsequent edits cannot move the guide.
                    PreviewSettings frozen = settings.copy();
                    Blueprint captured = snapshot;
                    frozen.source = () -> captured;
                    Hologram.start(frozen);
                    // Close the underlying inventory too, not just JEI's screen.
                    // Otherwise a recipe-viewer parent can retain a stale container.
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) mc.player.closeContainer();
                    else mc.setScreen(null);
                }).setHoverTooltips(Component.translatable("mekpreview.jemm.project_hint"));
        materials = new MaterialsPanel(this); materials.setId("materials-panel"); addWidget(materials);
        favorites = new FavoritesPanel(this); favorites.setId("favorites-panel"); addWidget(favorites);
        search = model instanceof NovaMultiblockWidget nova ? new NovaSearchPanel(nova, index -> {
            nova.setRecipeIndex(index); nova.markNeedUpdate(); modelDirty = true; showPanel("");
        }) : null;
        if (search != null) { search.setId("nova-search-panel"); addWidget(search); }
        if (search != null) button(this, "nova-search", 183, 123, 57, 17,
                () -> Component.translatable("mekpreview.search").getString(), d -> showPanel(panel.equals("search") ? "" : "search"))
                .setHoverTooltips(Component.translatable("mekpreview.search.link_hint"));
        button(this, "materials", search == null ? 183 : 243, 123, search == null ? 87 : 57, 17,
                () -> Component.translatable("mekpreview.materials.short").getString(), d -> showPanel(panel.equals("materials") ? "" : "materials"));
        button(this, "favorites", search == null ? 273 : 303, 123, search == null ? 90 : 60, 17,
                () -> "★", d -> showPanel(panel.equals("favorites") ? "" : "favorites"))
                .setHoverTooltips(Component.translatable("mekpreview.favorites"));
        showPanel("");
    }
    public void showPanel(String value) {
        panel = value;
        nativeControls.setVisible(value.isEmpty()); nativeControls.setActive(value.isEmpty());
        materials.setVisible(value.equals("materials")); materials.setActive(value.equals("materials"));
        favorites.setVisible(value.equals("favorites")); favorites.setActive(value.equals("favorites"));
        if (search != null) { search.setVisible(value.equals("search")); search.setActive(value.equals("search")); if (!value.equals("search")) search.blur(); }
        if (!value.equals("favorites")) favorites.blur();
        if (value.equals("materials")) materials.refresh();
        if (value.equals("favorites")) favorites.open();
        if (value.equals("search") && search != null) search.open();
    }
    private FavoritesStore.View view() { return new FavoritesStore.View(settings.layerMode.name(), settings.layer, settings.rotation, settings.hideShell); }
    private void applyView(FavoritesStore.View saved) {
        if (saved == null) return;
        settings.layerMode = LayerMode.valueOf(saved.mode()); settings.layer = Math.max(0, saved.layer());
        settings.rotation = Math.floorMod(saved.rotation(), 4); settings.hideShell = saved.hideShell();
    }
    public FavoritesStore.Favorite saveFavorite(String name) throws java.io.IOException {
        String label = name.isBlank() ? settings.title().getString() + " " + settings.dimensions.width() + "×"
                + settings.dimensions.height() + "×" + settings.dimensions.depth() : name;
        if (label.length() > 120) label = label.substring(0, 120);
        return FavoriteClient.store().add(label, JemmIntegration.equipmentKey(model), state().toString(), view());
    }
    public void loadFavorite(FavoritesStore.Favorite favorite) {
        if (!favorite.equipment().equals(JemmIntegration.equipmentKey(model))) { FavoriteClient.open(favorite); return; }
        try {
            FavoriteClient.restoreModel(model, favorite); applyView(favorite.view());
            modelDirty = dirty = true; showPanel("");
        } catch (Exception e) { FavoriteClient.failure(e); }
    }
    public MaterialsPanel materialsPanel() { return materials; }
    public NovaSearchPanel searchPanel() { return search; }
    public SidePanel.Input focusedInput() {
        var input = search == null ? null : search.focusedInput();
        return input == null ? favorites.focusedInput() : input;
    }
    private ButtonWidget button(WidgetGroup parent, String id, int x, int y, int w, int h,
                                Supplier<String> text, IntConsumer action) {
        ButtonWidget button = new ButtonWidget(x, y, w, h, click -> action.accept(click.button == 1 ? -1 : 1));
        button.setId(id);
        button.setButtonTexture(new ColorRectTexture(0xC0606060), new TextTexture(text).setWidth(w - 2));
        button.setHoverBorderTexture(1, 0xFFFFFFFF);
        parent.addWidget(button);
        return button;
    }
    private void cycleLayer(int direction) {
        if (settings.layerMode == LayerMode.ALL) {
            settings.layerMode = LayerMode.SINGLE;
            settings.layer = direction > 0 ? 0 : settings.dimensions.height() - 1;
        } else {
            settings.layer += direction;
            if (settings.layer < 0 || settings.layer >= settings.dimensions.height()) {
                settings.layerMode = LayerMode.ALL;
                settings.layer = settings.dimensions.height() - 1;
            }
        }
        dirty = true;
    }
    private CompoundTag state() { CompoundTag tag = new CompoundTag(); model.save(tag); return tag; }
    @Override public void initWidget() { super.initWidget(); rebuild(); dirty = geometryChanged = false; }
    @Override public void updateScreen() {
        if (!lastState.equals(state())) modelDirty = true;
        super.updateScreen();
    }
    @Override public void drawInBackground(GuiGraphics g, int x, int y, float partial) {
        if (!lastState.equals(state())) modelDirty = true;
        if (modelDirty) {
            // JEMM updates its costs/results/notifications during render. Flush pending edits
            // before capturing both geometry and the authoritative materials list.
            g.enableScissor(0, 0, 0, 0);
            model.render(g, -1000, -1000, partial);
            g.disableScissor();
            Blueprint updated = JemmBlueprint.capture(model);
            captures++;
            geometryChanged = !snapshot.blocks().equals(updated.blocks()) || !snapshot.dimensions().equals(updated.dimensions());
            snapshot = updated;
            settings.dimensions = snapshot.dimensions();
            settings.layer = Math.min(settings.layer, settings.dimensions.height() - 1);
            dirty |= geometryChanged;
            lastState = state();
            modelDirty = false;
        }
        if (dirty) { rebuild(); FavoriteClient.remember(model, view()); dirty = geometryChanged = false; }
        super.drawInBackground(g, x, y, partial);
    }
    private void rebuild() {
        float yaw = scene == null ? 25 : scene.getRotationYaw(), pitch = scene == null ? -135 : scene.getRotationPitch();
        if (scene == null) {
            scene = new PreviewScene(3, 3, 174, 118, Minecraft.getInstance().level, snapshot, settings);
            scene.setHoverTips(true);
            preview.addWidget(1, scene);
        } else scene.update(snapshot, settings, !geometryChanged);
        scene.setCameraYawAndPitch(yaw, pitch);
        if (layer != null) layer.setHoverTooltips(Component.translatable("mekpreview.control.layer"),
                Component.translatable("mekpreview.layer_number", settings.layer + 1, settings.dimensions.height()));
    }
    @Override public void setGui(ModularUI gui) {
        super.setGui(gui);
        if (gui != null) gui.registerCloseListener(this::dispose);
    }
    public void dispose() { if (scene != null) scene.dispose(); }
    public PreviewScene scene() { return scene; }
    public PreviewSettings settings() { return settings.copy(); }
    public Blueprint snapshot() { return snapshot; }
    public MultiblockWidget model() { return model; }
    public int captureCount() { return captures; }
}
