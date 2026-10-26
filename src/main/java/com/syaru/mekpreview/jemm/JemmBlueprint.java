package com.syaru.mekpreview.jemm;

import com.syaru.mekpreview.structure.*;
import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.*;

/** Copies JEMM's generated states and cost list; performs no layout or performance calculation. */
public final class JemmBlueprint {
    private JemmBlueprint() {}
    public static Blueprint capture(MultiblockWidget source) {
        var preview = Objects.requireNonNull(MtePreview.supports(source) ? MtePreview.create(source) : source.createPreview(),
                "JEMM does not provide a preview for this recipe");
        var size = preview.getDimension();
        Dimensions dimensions = new Dimensions(size.getX(), size.getY(), size.getZ());
        Map<Cell, Part> blocks = new LinkedHashMap<>();
        preview.getBlockStates().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var pos = entry.getKey();
            var state = entry.getValue();
            if (state.isAir()) return;
            Map<String, String> properties = new TreeMap<>();
            for (var property : state.getProperties()) properties.put(property.getName(), propertyName(state, property));
            boolean inner = pos.getX() > 0 && pos.getY() > 0 && pos.getZ() > 0
                    && pos.getX() < size.getX() - 1 && pos.getY() < size.getY() - 1 && pos.getZ() < size.getZ() - 1;
            blocks.put(new Cell(pos.getX(), pos.getY(), pos.getZ()),
                    new Part(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), "", inner, properties));
        });
        Map<String, Integer> costs = new TreeMap<>();
        source.getCosts().stream().filter(stack -> !stack.isEmpty()).forEach(stack ->
                costs.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum));
        return new Blueprint(JemmIntegration.kind(source), dimensions, blocks, Map.of(), costs);
    }
    private static <T extends Comparable<T>> String propertyName(net.minecraft.world.level.block.state.BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
