package com.syaru.mekpreview.client;

import com.syaru.mekpreview.structure.*;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Entirely local: never places blocks or sends interaction packets. */
public final class Hologram {
    public record Ghost(BlockPos local, BlockState state, boolean visible) {}
    public record Target(BlockPos position, BlockState expected, boolean wrong) {}
    public record DrawGhost(Ghost ghost, boolean wrong) {}
    private static PreviewSettings settings;
    private static Blueprint blueprint;
    private static List<Ghost> ghosts = List.of();
    private static Map<BlockPos, Ghost> index = Map.of();
    private static Map<BlockPos, Cell> sourceCells = Map.of();
    private static Set<Cell> correctCells = Set.of();
    private static long progressRevision;
    private static final CompletionTracker completion = new CompletionTracker();
    private static int completionEvents;
    private static Set<BlockPos> interior = Set.of();
    private static List<DrawGhost> drawGhosts = List.of();
    private static long meshRevision;
    private static TrackedDummyWorld displayWorld;
    private static BlockPos origin = BlockPos.ZERO;
    private static ResourceKey<Level> dimension;
    private static boolean anchored, shown;
    private static int correct, wrong, missing;
    private static float opacity = 0.55f;
    private static Target target;
    private static long lastScanTick = Long.MIN_VALUE;
    private Hologram() {}

    public static void start(PreviewSettings edited) {
        settings = edited.copy();
        blueprint = settings.blueprint();
        completion.reset(); correctCells = Set.of(); progressRevision++;
        rebuild();
        dimension = Minecraft.getInstance().level.dimension();
        anchored = false;
        shown = true;
        lastScanTick = Long.MIN_VALUE;
        tick();
    }
    private static void rebuild() {
        if (displayWorld != null) displayWorld.clear();
        displayWorld = new TrackedDummyWorld();
        List<Ghost> result = new ArrayList<>();
        Set<BlockPos> inside = new HashSet<>();
        Map<BlockPos, Cell> sources = new HashMap<>();
        var d = blueprint.dimensions();
        blueprint.blocks().forEach((pos, part) -> {
            Cell rotated = pos.rotate(d.width(), d.depth(), settings.rotation);
            sources.put(new BlockPos(rotated.x(), rotated.y(), rotated.z()), pos);
            result.add(new Ghost(new BlockPos(rotated.x(), rotated.y(), rotated.z()),
                    BlockResolver.state(part, settings.rotation),
                    settings.layerMode.includes(pos.y(), settings.layer) && (!settings.hideShell || part.interior())));
            if (part.interior()) inside.add(new BlockPos(rotated.x(), rotated.y(), rotated.z()));
            displayWorld.addBlock(new BlockPos(rotated.x(), rotated.y(), rotated.z()),
                    PreviewModels.blockInfo(part, settings.rotation, pos.y()));
        });
        ghosts = List.copyOf(result);
        sourceCells = Map.copyOf(sources); correctCells = Set.of(); progressRevision++; completion.reset();
        interior = Set.copyOf(inside);
        reindex();
        lastScanTick = Long.MIN_VALUE;
    }
    private static void reindex() {
        Map<BlockPos, Ghost> result = new HashMap<>();
        ghosts.forEach(g -> result.put(g.local(), g));
        index = Map.copyOf(result);
    }
    private static void updateVisibility() {
        ghosts = ghosts.stream().map(g -> new Ghost(g.local(), g.state(), settings.layerMode.includes(g.local().getY(), settings.layer)
                && (!settings.hideShell || interior.contains(g.local())))).toList();
        reindex();
        lastScanTick = Long.MIN_VALUE;
    }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (settings == null) return;
        if (mc.level == null || mc.player == null || !mc.level.dimension().equals(dimension)) { clear(); return; }
        if (!anchored && mc.screen == null) {
            BlockPos previous = origin;
            var hit = mc.player.pick(48, 0, false);
            BlockPos target = hit instanceof BlockHitResult blockHit && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                    ? blockHit.getBlockPos().relative(blockHit.getDirection())
                    : BlockPos.containing(mc.player.getEyePosition().add(mc.player.getLookAngle().scale(8)));
            int w = settings.rotation % 2 == 0 ? blueprint.dimensions().width() : blueprint.dimensions().depth();
            int d = settings.rotation % 2 == 0 ? blueprint.dimensions().depth() : blueprint.dimensions().width();
            origin = new BlockPos(target.getX() - w / 2, target.getY(), target.getZ() - d / 2);
            if (!previous.equals(origin)) lastScanTick = Long.MIN_VALUE;
        }
        if ((shown || anchored) && (lastScanTick == Long.MIN_VALUE || mc.level.getGameTime() - lastScanTick >= 10)) {
            correct = wrong = missing = 0;
            List<DrawGhost> drawing = new ArrayList<>();
            Set<Cell> installed = new HashSet<>();
            for (Ghost ghost : ghosts) {
                BlockPos pos = origin.offset(ghost.local());
                if (!mc.level.hasChunkAt(pos)) { missing++; continue; }
                BlockState actual = mc.level.getBlockState(pos);
                if (BlockResolver.matches(actual, ghost.state())) { correct++; installed.add(sourceCells.get(ghost.local())); }
                else {
                    boolean incorrect = !actual.isAir() && !actual.canBeReplaced();
                    if (incorrect) wrong++; else missing++;
                    if (ghost.visible()) drawing.add(new DrawGhost(ghost, incorrect));
                }
            }
            if (!drawGhosts.equals(drawing)) { drawGhosts = List.copyOf(drawing); meshRevision++; }
            if (!correctCells.equals(installed)) { correctCells = Set.copyOf(installed); progressRevision++; }
            lastScanTick = mc.level.getGameTime();
            if (completion.update(anchored, ghosts.size(), correct) && shown) {
                completionEvents++;
                if (com.syaru.mekpreview.ClientOptions.NOTIFY_COMPLETED.get())
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("mekpreview.build.complete"), false);
                if (com.syaru.mekpreview.ClientOptions.HIDE_COMPLETED.get()) { shown = false; HologramRenderer.clearCache(); }
            }
        }
        target = null;
        if (shown && mc.screen == null) {
            Vec3 eye = mc.player.getEyePosition();
            Vec3 end = eye.add(mc.player.getLookAngle().scale(48));
            // Traverse only the voxels crossed by the ray, not every block in the structure.
            target = net.minecraft.world.level.BlockGetter.traverseBlocks(eye, end, 0, (ignored, pos) -> {
                Ghost ghost = index.get(pos.subtract(origin));
                if (ghost == null || !ghost.visible() || !mc.level.hasChunkAt(pos)) return null;
                BlockState actual = mc.level.getBlockState(pos);
                return BlockResolver.matches(actual, ghost.state()) ? null : new Target(pos.immutable(), ghost.state(), !actual.isAir() && !actual.canBeReplaced());
            }, ignored -> null);
        }
    }
    public static boolean active() { return settings != null; }
    public static boolean shown() { return active() && shown; }
    public static boolean anchored() { return anchored; }
    public static BlockPos origin() { return origin; }
    public static List<Ghost> ghosts() { return ghosts; }
    public static List<DrawGhost> drawGhosts() { return drawGhosts; }
    public static long meshRevision() { return meshRevision; }
    public static Blueprint blueprint() { return blueprint; }
    public static int completionEvents() { return completionEvents; }
    public static long placedRevision(Blueprint plan, int rotation) { return samePlan(plan, rotation) ? progressRevision : -1; }
    private static boolean samePlan(Blueprint plan, int rotation) {
        return active() && anchored && rotation == settings.rotation && blueprint.equals(plan);
    }
    public static Map<String, Long> placedMaterials(Blueprint plan, int rotation, boolean visibleOnly,
                                                   LayerMode mode, int layer, boolean hideShell) {
        return samePlan(plan, rotation) ? MaterialPlan.placed(plan, visibleOnly, mode, layer, hideShell,
                correctCells, BlockResolver::itemId) : Map.of();
    }
    public static PreviewSettings settings() { return settings; }
    public static int correct() { return correct; }
    public static int wrong() { return wrong; }
    public static int missing() { return missing; }
    public static int total() { return ghosts.size(); }
    public static Target target() { return target; }
    public static float opacity() { return opacity; }
    public static void cycleOpacity() {
        if (active()) { opacity = opacity >= 0.84f ? 0.25f : opacity < 0.45f ? 0.55f : 0.85f; meshRevision++; }
    }
    public static net.minecraft.world.level.block.entity.BlockEntity displayTile(Ghost ghost) {
        return displayWorld == null ? null : displayWorld.getBlockEntity(ghost.local());
    }
    public static void toggle() { if (active()) { shown = !shown; lastScanTick = Long.MIN_VALUE; } }
    public static void anchor() { if (active()) { anchored = !anchored; completion.reset(); lastScanTick = Long.MIN_VALUE; progressRevision++; } }
    public static void rotate() {
        if (active()) { settings.rotation = (settings.rotation + 1) % 4; rebuild(); }
    }
    public static void nudge(int dx, int dy, int dz) {
        if (active()) {
            anchored = true; origin = origin.offset(dx, dy, dz); lastScanTick = Long.MIN_VALUE;
            correctCells = Set.of(); progressRevision++; completion.reset();
        }
    }
    public static void layer(int delta) {
        if (active()) {
            settings.layer = Math.max(0, Math.min(blueprint.dimensions().height() - 1, settings.layer + delta));
            if (settings.layerMode == LayerMode.ALL) settings.layerMode = LayerMode.SINGLE;
            updateVisibility();
        }
    }
    public static void cycleLayerMode() { if (active()) { settings.layerMode = settings.layerMode.next(); updateVisibility(); } }
    public static void clear() {
        settings = null; blueprint = null; ghosts = List.of(); dimension = null;
        index = Map.of(); interior = Set.of(); drawGhosts = List.of(); meshRevision++;
        sourceCells = Map.of(); correctCells = Set.of(); progressRevision++; completion.reset();
        InventoryAssist.reset(); ConstructionMaterials.clear();
        HologramRenderer.clearCache();
        anchored = false; shown = false; correct = wrong = missing = 0;
        target = null;
        if (displayWorld != null) { displayWorld.clear(); displayWorld = null; }
    }
}
