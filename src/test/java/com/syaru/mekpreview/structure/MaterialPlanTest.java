package com.syaru.mekpreview.structure;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialPlanTest {
    private Blueprint design() {
        return new Blueprint(StructureKind.DYNAMIC_TANK, new Dimensions(3, 3, 3), Map.of(
                new Cell(0, 0, 0), new Part("test:casing", false),
                new Cell(0, 1, 0), new Part("test:casing", false),
                new Cell(1, 1, 1), new Part("test:core", true)), Map.of(), Map.of("test:casing", 2, "test:core", 1, "test:upgrade", 4));
    }
    @Test void fullPlanUsesDeclaredMaterials() {
        var rows = MaterialPlan.compare(design(), false, LayerMode.SINGLE, 0, true, Map.of("test:casing", 1L), false);
        assertEquals(new MaterialPlan.Row("test:casing", 2, 1, 1), rows.get(0));
        assertEquals(4, rows.stream().filter(r -> r.itemId().equals("test:upgrade")).findFirst().orElseThrow().missing());
    }
    @Test void currentLayerExcludesOtherLayersAndNonBlockUpgrades() {
        var rows = MaterialPlan.compare(design(), true, LayerMode.SINGLE, 0, false, Map.of(), false);
        assertEquals(List.of(new MaterialPlan.Row("test:casing", 1, 0, 1)), rows);
    }
    @Test void visibleScopeRespectsBelowAndHiddenShell() {
        var rows = MaterialPlan.compare(design(), true, LayerMode.BELOW, 1, true, Map.of(), false);
        assertEquals(List.of(new MaterialPlan.Row("test:core", 1, 0, 1)), rows);
    }
    @Test void surplusNeverCreatesNegativeMissingAndCoveredRowsCanBeHidden() {
        assertTrue(MaterialPlan.compare(design(), true, LayerMode.ALL, 2, false,
                Map.of("test:casing", 64L, "test:core", 20L), true).isEmpty());
    }
    @Test void negativeInventoryIsNotTreatedAsExtraDemand() {
        var rows = MaterialPlan.compare(design(), true, LayerMode.SINGLE, 0, false, Map.of("test:casing", -5L), false);
        assertEquals(1, rows.get(0).missing()); assertEquals(0, rows.get(0).available());
    }
    @Test void visibleBlockIdsAreMappedToActualInventoryItemIds() {
        var rows = MaterialPlan.compare(design(), true, LayerMode.SINGLE, 0, false, Map.of("test:actual_item", 1L), false,
                id -> id.equals("test:casing") ? "test:actual_item" : id);
        assertEquals(List.of(new MaterialPlan.Row("test:actual_item", 1, 1, 0)), rows);
    }
    @Test void subtractsPlacedAndInventoryWithoutChangingDesignTotals() {
        var rows = MaterialPlan.compare(design(), false, LayerMode.ALL, 2, false, Map.of("test:casing", 1L), false,
                java.util.function.Function.identity(), Map.of("test:casing", 1L, "test:core", 1L));
        var casing = rows.get(0);
        assertEquals(2, casing.required()); assertEquals(1, casing.placed()); assertEquals(1, casing.remaining()); assertEquals(0, casing.missing());
        assertEquals(4, rows.stream().filter(r -> r.itemId().equals("test:upgrade")).findFirst().orElseThrow().missing());
    }
    @Test void clampsBogusPlacedCounts() {
        var rows = MaterialPlan.compare(design(), true, LayerMode.SINGLE, 0, false, Map.of(), false,
                java.util.function.Function.identity(), Map.of("test:casing", 100L));
        assertEquals(1, rows.get(0).placed()); assertEquals(0, rows.get(0).remaining()); assertEquals(0, rows.get(0).missing());
    }
    @Test void installedCountsRespectLayerShellAndItemMapping() {
        var correct = Set.of(new Cell(0, 0, 0), new Cell(0, 1, 0), new Cell(1, 1, 1));
        assertEquals(Map.of("test:casing", 1L), MaterialPlan.placed(design(), true, LayerMode.SINGLE, 0, false, correct, java.util.function.Function.identity()));
        assertEquals(Map.of("item:core", 1L), MaterialPlan.placed(design(), true, LayerMode.BELOW, 1, true, correct,
                id -> id.equals("test:core") ? "item:core" : null));
        assertEquals(Map.of("test:casing", 2L, "test:core", 1L), MaterialPlan.placed(design(), false, LayerMode.SINGLE, 0, true, correct, java.util.function.Function.identity()));
    }
}
