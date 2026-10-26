package com.syaru.mekpreview.client;

import com.syaru.mekpreview.structure.Part;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockResolver {
    private BlockResolver() {}
    public static BlockState state(Part part, int rotation) {
        ResourceLocation id = new ResourceLocation(part.blockId());
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Missing block: " + id);
        BlockState state = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            String value = part.properties().get(property.getName());
            if (value != null) state = withValue(state, property, value);
        }
        if (!part.facing().isEmpty()) {
            for (Property<?> property : state.getProperties()) {
                if (property.getName().equals("facing")) state = withValue(state, property, part.facing());
            }
        }
        return state.rotate(switch (Math.floorMod(rotation, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        });
    }
    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String name) {
        return property.getValue(name).map(value -> state.setValue(property, value)).orElse(state);
    }
    public static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)));
    }
    public static String itemId(String blockId) {
        var id = ResourceLocation.tryParse(blockId);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return null;
        var item = BuiltInRegistries.BLOCK.get(id).asItem();
        return item == net.minecraft.world.item.Items.AIR ? null : BuiltInRegistries.ITEM.getKey(item).toString();
    }
    /** Ignore runtime state (active, formed, valve mode), but honor physical orientation. */
    public static boolean matches(BlockState actual, BlockState expected) {
        if (actual.getBlock() != expected.getBlock()) return false;
        for (Property<?> p : expected.getProperties()) {
            if (p.getName().equals("facing") || p.getName().equals("axis")) {
                if (!actual.getValue(p).equals(expected.getValue(p))) return false;
            }
        }
        return true;
    }
}
