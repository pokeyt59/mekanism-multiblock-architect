package com.syaru.mekpreview.client;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.syaru.mekpreview.structure.Blueprint;
import com.syaru.mekpreview.structure.Cell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import java.util.ArrayList;

/** One reusable LDLib scene with GPU-cached block geometry, without a large FBO. */
public final class PreviewScene extends SceneWidget {
    private TrackedDummyWorld layout;
    private int reloadVersion = HologramRenderer.reloadVersion();
    public PreviewScene(int x, int y, int width, int height, Level level,
                        Blueprint blueprint, PreviewSettings settings) {
        this(x, y, width, height, new TrackedDummyWorld(), blueprint, settings);
    }
    private PreviewScene(int x, int y, int width, int height, TrackedDummyWorld layout,
                         Blueprint blueprint, PreviewSettings settings) {
        super(x, y, width, height, layout, false);
        this.layout = layout;
        setRenderFacing(false);
        setHoverTips(false);
        populate(blueprint, settings);
        useCacheBuffer();
    }
    public void update(Blueprint blueprint, PreviewSettings settings, boolean preserveZoom) {
        float oldZoom = getZoom();
        var previous = layout;
        layout = new TrackedDummyWorld();
        // The old cache compiler keeps its own immutable set while it is cancelled.
        core = new java.util.HashSet<>();
        createScene(layout, false);
        populate(blueprint, settings);
        if (preserveZoom) setZoom(oldZoom);
        previous.clear();
    }
    private void populate(Blueprint blueprint, PreviewSettings settings) {
        var positions = new ArrayList<BlockPos>();
        blueprint.visible(settings.layerMode, settings.layer, settings.hideShell).forEach((cell, part) -> {
            Cell r = cell.rotate(blueprint.dimensions().width(), blueprint.dimensions().depth(), settings.rotation);
            BlockPos pos = new BlockPos(r.x(), r.y(), r.z());
            // Detached display tiles let Mekanism render rods, coils and rotor blades.
            layout.addBlock(pos, PreviewModels.blockInfo(part, settings.rotation, cell.y()));
            positions.add(pos);
        });
        if (!positions.isEmpty()) setRenderedCore(positions);
        else setRenderedCore(java.util.List.of(BlockPos.ZERO));
        var d = blueprint.dimensions();
        boolean swap = settings.rotation % 2 != 0;
        setCenter(new Vector3f((swap ? d.depth() : d.width()) / 2f,
                d.height() / 2f, (swap ? d.width() : d.depth()) / 2f));
        setZoom(d.longest() * 1.8f + 2);
    }
    public void dispose() {
        if (getRenderer() != null) getRenderer().deleteCacheBuffer();
        if (getDummyWorld() != null) getDummyWorld().clear();
        layout.clear();
    }
    @Override public void updateScreen() {
        super.updateScreen();
        if (reloadVersion != HologramRenderer.reloadVersion()) {
            reloadVersion = HologramRenderer.reloadVersion();
            needCompileCache();
        }
    }
}
