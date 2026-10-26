package com.syaru.mekpreview.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import java.util.*;

/** Static block models are uploaded once; only animated tiles and warnings are drawn per frame. */
public final class HologramRenderer {
    private static final RenderType GHOST = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediateWithBuffers(
            Map.of(GHOST, new BufferBuilder(262144), RenderType.lines(), new BufferBuilder(65536),
                    RenderType.debugFilledBox(), new BufferBuilder(65536)), new BufferBuilder(65536));
    private static VertexBuffer mesh;
    private static long revision = Long.MIN_VALUE;
    private static int reloadVersion, compiledReload = -1, builds;
    private static List<Hologram.DrawGhost> animated = List.of(), incorrect = List.of();
    private static AABB bounds;
    private HologramRenderer() {}
    public static int cacheBuildCount() { return builds; }
    public static int reloadVersion() { return reloadVersion; }
    public static void resourceReload() { reloadVersion++; }
    public static void clearCache() {
        if (mesh != null) { mesh.close(); mesh = null; }
        revision = Long.MIN_VALUE; bounds = null;
        animated = incorrect = List.of();
    }
    private static void compile(Minecraft mc) {
        clearCache();
        var vertices = new BufferBuilder(262144);
        vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        var alpha = new GhostVertexConsumer(vertices, Hologram.opacity());
        MultiBufferSource destination = ignored -> alpha;
        var pose = new PoseStack();
        var dynamic = new ArrayList<Hologram.DrawGhost>();
        var wrong = new ArrayList<Hologram.DrawGhost>();
        for (var drawing : Hologram.drawGhosts()) {
            var ghost = drawing.ghost();
            var pos = ghost.local();
            var box = new AABB(pos);
            bounds = bounds == null ? box : bounds.minmax(box);
            if (ghost.state().getRenderShape() == RenderShape.MODEL) {
                pose.pushPose(); pose.translate(pos.getX(), pos.getY(), pos.getZ());
                mc.getBlockRenderer().renderSingleBlock(ghost.state(), pose, destination, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, ModelData.EMPTY, GHOST);
                pose.popPose();
            }
            if (Hologram.displayTile(ghost) != null || ghost.state().getRenderShape() != RenderShape.MODEL) dynamic.add(drawing);
            if (drawing.wrong()) wrong.add(drawing);
        }
        mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
        mesh.bind(); mesh.upload(vertices.end()); VertexBuffer.unbind();
        animated = List.copyOf(dynamic); incorrect = List.copyOf(wrong);
        revision = Hologram.meshRevision(); compiledReload = reloadVersion; builds++;
    }
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !Hologram.shown()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.screen != null) return;
        if (mesh == null || revision != Hologram.meshRevision() || compiledReload != reloadVersion) compile(mc);
        if (bounds == null) return;
        var origin = Hologram.origin();
        var worldBounds = bounds.move(origin).inflate(.003);
        Vec3 camera = event.getCamera().getPosition();
        if (!event.getFrustum().isVisible(worldBounds) || worldBounds.distanceToSqr(camera) > 96 * 96) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(origin.getX() - camera.x, origin.getY() - camera.y, origin.getZ() - camera.z);
        GHOST.setupRenderState();
        mesh.bind();
        mesh.drawWithShader(new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()),
                RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind(); GHOST.clearRenderState();
        MultiBufferSource translucent = type -> new GhostVertexConsumer(BUFFERS.getBuffer(type), Hologram.opacity());
        for (var drawing : animated) {
            var ghost = drawing.ghost(); var pos = ghost.local();
            if (!event.getFrustum().isVisible(new AABB(pos.offset(origin)))) continue;
            pose.pushPose(); pose.translate(pos.getX(), pos.getY(), pos.getZ());
            if (ghost.state().getRenderShape() != RenderShape.MODEL)
                mc.getBlockRenderer().renderSingleBlock(ghost.state(), pose, translucent, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, ModelData.EMPTY, GHOST);
            var tile = Hologram.displayTile(ghost);
            if (tile != null) {
                var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(tile);
                if (renderer != null) renderer.render(tile, event.getPartialTick(), pose, translucent,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
        for (var drawing : incorrect) {
            AABB box = new AABB(drawing.ghost().local()).inflate(.002);
            LevelRenderer.addChainedFilledBoxVertices(pose, BUFFERS.getBuffer(RenderType.debugFilledBox()),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 1, .1f, .1f, .24f);
            LevelRenderer.renderLineBox(pose, BUFFERS.getBuffer(RenderType.lines()), box, 1, .18f, .15f, .85f);
        }
        var target = Hologram.target();
        if (target != null && !target.wrong())
            LevelRenderer.renderLineBox(pose, BUFFERS.getBuffer(RenderType.lines()),
                    new AABB(target.position().subtract(origin)).inflate(.002), 1, 1, .4f, 1);
        pose.popPose();
        BUFFERS.endBatch();
    }
}
