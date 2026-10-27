package com.syaru.mekpreview.client;

import com.syaru.mekpreview.build.MaterialInventory;
import com.syaru.mekpreview.structure.*;
import net.minecraft.client.Minecraft;
import java.util.*;

/** Recompute only on a changed scan, layer, or inventory, never from the render loop. */
public final class ConstructionMaterials {
    private record Inputs(Blueprint plan, long revision, int rotation, LayerMode mode, int layer,
                          boolean shell, Map<String, Long> inventory) {}
    private static Inputs previous;
    private static List<MaterialPlan.Row> rows = List.of();
    private static long checked = Long.MIN_VALUE;
    public static List<MaterialPlan.Row> rows() { return rows; }
    public static void clear() { previous = null; rows = List.of(); checked = Long.MIN_VALUE; MaterialHud.clear(); }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (!Hologram.active() || mc.player == null || mc.level == null) { clear(); return; }
        long now = mc.level.getGameTime();
        if (checked != Long.MIN_VALUE && now - checked < 5) return;
        checked = now;
        var view = Hologram.settings(); var plan = Hologram.blueprint();
        var inventory = MaterialInventory.available(mc.player.getInventory());
        var input = new Inputs(plan, Hologram.placedRevision(plan, view.rotation), view.rotation,
                view.layerMode, view.layer, view.hideShell, inventory);
        if (input.equals(previous)) return;
        previous = input;
        var placed = Hologram.placedMaterials(plan, view.rotation, true, view.layerMode, view.layer, view.hideShell);
        rows = MaterialPlan.compare(plan, true, view.layerMode, view.layer, view.hideShell, inventory, false,
                BlockResolver::itemId, placed).stream().filter(r -> r.remaining() > 0).toList();
    }
    private ConstructionMaterials() {}
}
