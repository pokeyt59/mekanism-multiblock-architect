package com.syaru.mekpreview.test;
import com.syaru.mekpreview.test.fixture.Blueprints;

import com.syaru.mekpreview.client.BlockResolver;
import com.syaru.mekpreview.structure.*;
import mekanism.common.lib.multiblock.IMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@GameTestHolder("mekpreview_test")
public final class FormationTests {
    @GameTestGenerator
    public static Collection<TestFunction> generated() {
        List<TestFunction> tests = new ArrayList<>();
        for (StructureKind kind : StructureKind.values()) {
            Dimensions[] sizes = {kind.defaults(), kind.normalize(new Dimensions(0, 0, 0)),
                    kind.normalize(new Dimensions(100, 100, 100))};
            for (int i = 0; i < sizes.length; i++) {
                Dimensions size = sizes[i];
                int rotation = i;
                String name = kind.id() + "_" + List.of("default", "minimum", "maximum").get(i);
                tests.add(new TestFunction("mekpreview", "mekpreview_test." + name, "mekpreview_test:empty",
                        240, 0, true, h -> verify(h, kind, size, rotation)));
            }
        }
        return tests;
    }
    private static void verify(GameTestHelper h, StructureKind kind, Dimensions size, int rotation) {
        Blueprint b = Blueprints.create(kind, size, true);
        b.materials().keySet().forEach(id -> {
            if (!BuiltInRegistries.ITEM.containsKey(new ResourceLocation(id))) throw new IllegalStateException("Missing item " + id);
        });
        List<BlockPos> multiblocks = new ArrayList<>();
        b.blocks().forEach((cell, part) -> {
            Cell p = cell.rotate(size.width(), size.depth(), rotation);
            BlockPos relative = new BlockPos(p.x() + 1, p.y() + 1, p.z() + 1);
            h.setBlock(relative, BlockResolver.state(part, rotation));
            var tile = h.getBlockEntity(relative);
            if (tile instanceof IMultiblock<?>) multiblocks.add(relative);
        });
        h.succeedWhen(() -> h.assertTrue(multiblocks.stream().anyMatch(pos ->
                h.getBlockEntity(pos) instanceof IMultiblock<?> tile && tile.getMultiblock().isFormed()),
                kind + " should form at " + size + ", rotation " + rotation));
    }
}
