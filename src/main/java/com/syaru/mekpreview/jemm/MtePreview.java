package com.syaru.mekpreview.jemm;

import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import giselle.jei_mekanism_multiblocks.client.preview.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import mekanism.common.block.attribute.Attribute;

/** MTE 2.8.2 supplies JEMM costs/settings but predates JEMM's preview API. */
public final class MtePreview {
    public static boolean supports(MultiblockWidget model) {
        for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("morethermalevaporation.client.jei.category.MoreEvaporationPlantCategory$MoreEvaporationPlantWidget")
                    || type.getName().equals("io.github.masyumero.morethermalevaporationcompat.client.jei.category.MoreEvaporationPlantCompatCategory$MoreEvaporationPlantCompatWidget"))
                return true;
        return false;
    }
    public static PreviewLevel create(MultiblockWidget model) {
        var controller = model.getCosts().stream().filter(s -> s.getItem() instanceof BlockItem)
                .map(s -> ((BlockItem) s.getItem()).getBlock())
                .filter(b -> BuiltInRegistries.BLOCK.getKey(b).getPath().endsWith("_thermal_evaporation_controller"))
                .findFirst().orElseThrow(() -> new IllegalStateException("MTE tier controller not found"));
        var id = BuiltInRegistries.BLOCK.getKey(controller);
        String prefix = id.getPath().substring(0, id.getPath().length() - "controller".length());
        BlockState casing = block(id.getNamespace(), prefix + "block").defaultBlockState();
        BlockState valve = block(id.getNamespace(), prefix + "valve").defaultBlockState();
        BlockState face = model.isUseGlass() ? model.getGlassBlock().defaultBlockState() : casing;
        if (large(model)) return large(model, controller.defaultBlockState(), casing, valve, face);
        var builder = new PreviewBuilder(model.getDimension());
        builder.setBlockShell(casing, face);
        builder.setBlock(PreviewSelectors.top(), Blocks.AIR.defaultBlockState());
        builder.setBlock(new BlockPos(model.getDimensionWidth() - 2, 1, model.getDimensionLength() - 1),
                Attribute.setFacing(controller.defaultBlockState(), Direction.SOUTH));
        builder.replaceBlock(PreviewSelectors.shellSidesCCW(), face, valve, (int) invoke(model, "getValveCount"));
        boolean solar = (boolean) invoke(model, "isUseAdvancedSolarGenerator");
        builder.setBlock(PreviewSelectors.topEdges(), model.isUseGlass() && !solar ? face : casing);
        if (solar) builder.setBlock(PreviewSelectors.topCorners(),
                block("mekanismgenerators", "advanced_solar_generator").defaultBlockState());
        else if (model.isUseGlass()) builder.setBlock(PreviewSelectors.topCorners(), casing);
        return new PreviewLevel(builder);
    }
    private static boolean large(MultiblockWidget model) {
        try { return (boolean) model.getClass().getMethod("isUseLargeType").invoke(model); }
        catch (NoSuchMethodException absentOnOldVersion) { return false; }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("MTE large structure API", e); }
    }
    private static PreviewLevel large(MultiblockWidget model, BlockState controller, BlockState casing,
                                      BlockState valve, BlockState face) {
        int height = model.getDimensionHeight();
        var builder = new PreviewBuilder(new Vec3i(9, height, 9));
        var free = new java.util.ArrayList<BlockPos>();
        try {
            var validator = Class.forName("morethermalevaporation.common.content.evaporation.MoreThermalEvaporationValidator");
            var gridMethod = validator.getDeclaredMethod("getAllowedGrid", int.class, int.class);
            gridMethod.setAccessible(true);
            for (int y = 0; y < height; y++) {
                byte[][] grid = (byte[][]) gridMethod.invoke(null, y, height - 1);
                for (int z = 0; z < 9; z++) for (int x = 0; x < 9; x++) {
                    var pos = new BlockPos(x, y, z);
                    if (grid[z][x] == 1) builder.setBlock(pos, casing);
                    else if (grid[z][x] == 2) { builder.setBlock(pos, face); free.add(pos); }
                }
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("MTE validator grid unavailable", e); }
        var controllerPos = new BlockPos(4, 2, 0);
        builder.setBlock(controllerPos, Attribute.setFacing(controller, Direction.NORTH));
        free.remove(controllerPos);
        int valves = (int) invoke(model, "getValveCount");
        for (int i = 0; i < Math.min(valves, free.size()); i++) builder.setBlock(free.get(i), valve);
        if ((boolean) invoke(model, "isUseAdvancedSolarGenerator"))
            for (int x : new int[]{1, 7}) for (int z : new int[]{1, 7})
                builder.setBlock(new BlockPos(x, height - 1, z), block("mekanismgenerators", "advanced_solar_generator").defaultBlockState());
        return new PreviewLevel(builder);
    }
    public static String title(MultiblockWidget model) {
        model.initialize();
        return model.getCosts().stream().filter(s -> s.getItem() instanceof BlockItem
                && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().endsWith("_thermal_evaporation_controller"))
                .findFirst().orElseThrow().getHoverName().getString();
    }
    private static Block block(String namespace, String path) {
        var id = new ResourceLocation(namespace, path);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalStateException("MTE preview block missing: " + id);
        return BuiltInRegistries.BLOCK.get(id);
    }
    private static Object invoke(Object model, String method) {
        try { return model.getClass().getMethod(method).invoke(model); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported MTE preview API", e); }
    }
    private MtePreview() {}
}
