package com.syaru.mekpreview.structure;

import java.util.*;

/** Inventory and placed-block comparison; never consumes items or changes a recipe's bill of materials. */
public final class MaterialPlan {
    public record Row(String itemId, long required, long available, long missing, long placed) {
        public Row(String itemId, long required, long available, long missing) { this(itemId, required, available, missing, 0); }
        public long remaining() { return Math.max(0, required - placed); }
    }
    private MaterialPlan() {}
    public static List<Row> compare(Blueprint blueprint, boolean visibleOnly, LayerMode mode, int layer,
                                    boolean hideShell, Map<String, Long> inventory, boolean missingOnly) {
        return compare(blueprint, visibleOnly, mode, layer, hideShell, inventory, missingOnly, java.util.function.Function.identity());
    }
    public static List<Row> compare(Blueprint blueprint, boolean visibleOnly, LayerMode mode, int layer,
                                    boolean hideShell, Map<String, Long> inventory, boolean missingOnly,
                                    java.util.function.Function<String, String> blockItem) {
        return compare(blueprint, visibleOnly, mode, layer, hideShell, inventory, missingOnly, blockItem, Map.of());
    }
    public static List<Row> compare(Blueprint blueprint, boolean visibleOnly, LayerMode mode, int layer,
                                    boolean hideShell, Map<String, Long> inventory, boolean missingOnly,
                                    java.util.function.Function<String, String> blockItem, Map<String, Long> placed) {
        Map<String, Integer> required;
        if (visibleOnly) {
            required = new TreeMap<>();
            blueprint.visible(mode, layer, hideShell).values().forEach(part -> {
                String item = blockItem.apply(part.blockId());
                if (item != null) required.merge(item, 1, Integer::sum);
            });
        } else required = blueprint.materials();
        return required.entrySet().stream().filter(e -> e.getValue() > 0).map(e -> {
            long have = Math.max(0, inventory.getOrDefault(e.getKey(), 0L));
            long installed = Math.max(0, Math.min(e.getValue(), placed.getOrDefault(e.getKey(), 0L)));
            return new Row(e.getKey(), e.getValue(), have, Math.max(0, (long) e.getValue() - installed - have), installed);
        }).filter(r -> !missingOnly || r.missing() > 0)
                .sorted(Comparator.comparing(Row::itemId)).toList();
    }
    public static Map<String, Long> placed(Blueprint blueprint, boolean visibleOnly, LayerMode mode, int layer,
                                           boolean hideShell, Set<Cell> correct, java.util.function.Function<String, String> blockItem) {
        Map<String, Long> counts = new HashMap<>();
        for (var cell : correct) {
            var part = blueprint.blocks().get(cell);
            if (part == null || visibleOnly && (!mode.includes(cell.y(), layer) || hideShell && !part.interior())) continue;
            String item = blockItem.apply(part.blockId());
            if (item != null) counts.merge(item, 1L, Long::sum);
        }
        return Map.copyOf(counts);
    }
}
