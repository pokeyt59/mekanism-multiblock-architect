package com.syaru.mekpreview.structure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public record Blueprint(StructureKind kind, Dimensions dimensions, Map<Cell, Part> blocks,
                        Map<String, Integer> extraMaterials, Map<String, Integer> declaredMaterials) {
    public Blueprint(StructureKind kind, Dimensions dimensions, Map<Cell, Part> blocks, Map<String, Integer> extras) {
        this(kind, dimensions, blocks, extras, Map.of());
    }
    public Blueprint {
        blocks = Collections.unmodifiableMap(new LinkedHashMap<>(blocks));
        extraMaterials = Map.copyOf(extraMaterials);
        declaredMaterials = Collections.unmodifiableMap(new TreeMap<>(declaredMaterials));
        if (blocks.keySet().stream().anyMatch(p -> !dimensions.contains(p))) {
            throw new IllegalArgumentException("Block outside blueprint dimensions");
        }
    }
    public Map<String, Integer> materials() {
        if (!declaredMaterials.isEmpty()) return declaredMaterials;
        Map<String, Integer> counts = new TreeMap<>();
        blocks.values().forEach(part -> counts.merge(part.blockId(), 1, Integer::sum));
        extraMaterials.forEach((id, count) -> counts.merge(id, count, Integer::sum));
        return counts;
    }
    public Map<Cell, Part> visible(LayerMode mode, int layer, boolean hideShell) {
        Map<Cell, Part> result = new LinkedHashMap<>();
        blocks.forEach((pos, part) -> {
            if (mode.includes(pos.y(), layer) && (!hideShell || part.interior())) result.put(pos, part);
        });
        return result;
    }
}
