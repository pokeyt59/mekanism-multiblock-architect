package com.syaru.mekpreview.jemm;

import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;
import giselle.jei_mekanism_multiblocks.client.jei.category.ICostConsumer;
import giselle.jei_mekanism_multiblocks.client.preview.IPreviewBuilder;
import giselle.jei_mekanism_multiblocks.client.gui.LabelWidget;
import giselle.jei_mekanism_multiblocks.client.gui.IntSliderWithButtons;
import giselle.jei_mekanism_multiblocks.client.jei.ResultWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;

/** Optional reflection bridge: the authoritative Nova construction blueprint defines every block. */
public final class NovaMultiblockWidget extends MultiblockWidget {
    public static final String ENTRY_CLASS = "com.falcon2235.moremultiblock.client.jei.StructureEntry";
    private final Object source;
    private final Map<BlockPos, BlockState> blocks;
    private final BlockPos size;
    private final Component title;
    private final Map<BlockPos, BlockState> baseBlocks;
    private final NovaPerformance performance;
    private final List<BlockState> coils, processors;
    private final List<BlockPos> coilPositions;
    private final BlockPos processorPosition;
    private int lastCoil = -1, lastParallel = -1;
    private final IntSliderWithButtons recipeControl, speedControl, energyControl, coilControl, parallelControl;
    private NovaPerformance.Result calculated;
    public NovaMultiblockWidget(Object source) {
        this.source = source;
        title = (Component) field(source, "name");
        BlockState controller = (BlockState) field(source, "controllerState");
        Map<BlockPos, BlockState> raw = new LinkedHashMap<>();
        raw.put(BlockPos.ZERO, controller);
        try {
            Class<?> api = Class.forName("com.falcon2235.moremultiblock.multiblock.StructureBlueprint");
            var cells = (List<?>) api.getMethod("forController", BlockPos.class, BlockState.class)
                    .invoke(null, BlockPos.ZERO, controller);
            if (cells == null) throw new IllegalStateException("Nova controller has no construction blueprint");
            for (Object cell : cells) raw.put((BlockPos) cell.getClass().getMethod("pos").invoke(cell),
                    ((Block) cell.getClass().getMethod("block").invoke(cell)).defaultBlockState());
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Nova structure API", e); }
        int minX = raw.keySet().stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int minY = raw.keySet().stream().mapToInt(BlockPos::getY).min().orElseThrow();
        int minZ = raw.keySet().stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        size = new BlockPos(raw.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow() - minX + 1,
                raw.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow() - minY + 1,
                raw.keySet().stream().mapToInt(BlockPos::getZ).max().orElseThrow() - minZ + 1);
        blocks = new LinkedHashMap<>();
        raw.forEach((p, state) -> blocks.put(p.offset(-minX, -minY, -minZ), state));
        baseBlocks = Collections.unmodifiableMap(new LinkedHashMap<>(blocks));
        performance = new NovaPerformance(controller);
        coils = NovaPerformance.coils();
        processors = NovaPerformance.processors();
        Map<Block, Integer> tiers = new HashMap<>();
        for (int i = 0; i < coils.size(); i++) tiers.put(coils.get(i).getBlock(), i);
        coilPositions = baseBlocks.entrySet().stream().filter(e -> tiers.containsKey(e.getValue().getBlock())).map(Map.Entry::getKey).toList();
        Block casing = ((BlockState) field(source, "casingState")).getBlock();
        processorPosition = performance.parallel() ? baseBlocks.entrySet().stream().filter(e -> e.getValue().getBlock() == casing)
                .map(Map.Entry::getKey).findFirst().orElse(null) : null;
        configsList.getItems().addChild(new LabelWidget(0, 0, 98, 12,
                Component.literal(size.getX() + " × " + size.getY() + " × " + size.getZ())));
        recipeControl = control("recipe", 1, Math.max(1, performance.recipes().size()), performance.recipes().size() > 1);
        boolean hasCoils = !coilPositions.isEmpty();
        int initialCoil = coilPositions.stream().mapToInt(pos -> tiers.get(baseBlocks.get(pos).getBlock())).max().orElse(0);
        coilControl = control("coil", 0, Math.max(0, coils.size() - 1), hasCoils);
        coilControl.getSlider().setValue(Math.max(0, initialCoil));
        parallelControl = control("parallel", 0, processors.size(), performance.parallel());
        speedControl = control("speed", 0, (int) field(source, "maxSpeedUpgrades"), (int) field(source, "maxSpeedUpgrades") > 0);
        energyControl = control("energy", 0, (int) field(source, "maxEnergyUpgrades"), (int) field(source, "maxEnergyUpgrades") > 0);
        recipeControl.getSlider().addValueChangeHanlder(value -> {
            if (!performance.recipes().isEmpty() && hasCoils)
                coilControl.getSlider().setValue(Math.max(coil(), performance.recipes().get(value - 1).requiredCoil()));
        });
    }
    private IntSliderWithButtons control(String key, int min, int max, boolean visible) {
        var control = new IntSliderWithButtons(0, 0, 0, 0, "mekpreview.nova." + key, min, min, max);
        control.getSlider().addValueChangeHanlder(value -> markNeedUpdate());
        if (visible) configsList.getItems().addChild(control);
        return control;
    }
    public int recipeIndex() { return recipeControl.getSlider().getValue() - 1; }
    public void setRecipeIndex(int value) { recipeControl.getSlider().setValue(value + 1); }
    public int coil() { return coilControl.getSlider().getValue(); }
    public void setCoil(int value) { coilControl.getSlider().setValue(value); }
    public void setSpeedUpgrades(int value) { speedControl.getSlider().setValue(value); }
    public void setEnergyUpgrades(int value) { energyControl.getSlider().setValue(value); }
    public void setParallelTier(int value) { parallelControl.getSlider().setValue(value); }
    public NovaPerformance performance() { return performance; }
    public NovaPerformance.Result calculated() { return calculated; }
    private int parallelCount() {
        int index = parallelControl.getSlider().getValue();
        return index == 0 ? 1 : NovaPerformance.parallelCount(processors.get(index - 1));
    }
    private void refreshHardware() {
        int parallel = parallelControl.getSlider().getValue();
        if (lastCoil == coil() && lastParallel == parallel) return;
        blocks.clear(); blocks.putAll(baseBlocks);
        coilPositions.forEach(pos -> blocks.put(pos, coils.get(coil())));
        if (parallel > 0 && processorPosition != null) blocks.put(processorPosition, processors.get(parallel - 1));
        lastCoil = coil(); lastParallel = parallel;
    }
    public String title() { return title.getString(); }
    public String equipmentKey() { return field(source, "id").toString(); }
    private boolean missingSavedRecipe;
    public boolean missingSavedRecipe() { return missingSavedRecipe; }
    @Override public boolean canCreatePreview() { return true; }
    @Override protected void fillPreview(IPreviewBuilder builder) { refreshHardware(); blocks.forEach(builder::setBlock); }
    @Override protected void collectCost(ICostConsumer consumer) {
        refreshHardware();
        Map<Block, Integer> costs = new LinkedHashMap<>();
        blocks.values().forEach(state -> costs.merge(state.getBlock(), 1, Integer::sum));
        costs.forEach((block, count) -> consumer.accept(new ItemStack(block, count)));
    }
    @Override protected void collectResult(Consumer<AbstractWidget> consumer) {
        calculated = performance.calculate(recipeIndex(), coil(), parallelCount(), speedControl.getSlider().getValue(), energyControl.getSlider().getValue());
        if (performance.recipes().isEmpty()) {
            consumer.accept(new LabelWidget(0, 0, 0, 0, Component.translatable("mekpreview.nova.no_recipes")));
            return;
        }
        var recipe = performance.recipes().get(recipeIndex());
        recipeControl.setTooltipMessage(recipe.name(), Component.literal((recipeIndex() + 1) + " / " + performance.recipes().size()));
        if (!coilPositions.isEmpty()) coilControl.setTooltipMessage(coils.get(coil()).getBlock().getName());
        int processor = parallelControl.getSlider().getValue();
        if (processor > 0) parallelControl.setTooltipMessage(processors.get(processor - 1).getBlock().getName());
        var name = new ResultWidget(Component.translatable("mekpreview.nova.recipe_name"), recipe.name());
        var detail = new ArrayList<Component>();
        for (var input : calculated.inputs()) detail.add(Component.translatable("mekpreview.nova.input", input.name(), format(input.amount()), input.volume() ? "mB" : ""));
        if (calculated.note() != null) detail.add(calculated.note());
        name.setTooltipMessage(detail.toArray(Component[]::new));
        consumer.accept(name);
        if (!calculated.runnable()) {
            consumer.accept(new LabelWidget(0, 0, 0, 0, Component.translatable(coil() < recipe.requiredCoil()
                    ? "mekpreview.nova.coil_required" : "mekpreview.nova.capacity_required")));
            return;
        }
        if (!calculated.generator()) row(consumer, "time", Component.literal(calculated.ticks() + " t / " + format(calculated.ticks() / 20.0) + " s"));
        if (!performance.kind().equals("PBF")) row(consumer, calculated.generator() ? "generation" : "usage",
                Component.literal(energy(calculated.joulesPerTick()) + "/t"));
        if (performance.parallel()) {
            row(consumer, "parallel_count", Component.literal(Integer.toString(parallelCount())));
            row(consumer, "batch", Component.literal(Integer.toString(calculated.batch())));
        }
        for (var output : calculated.outputs()) {
            boolean chance = output.probability() < 1;
            double rate = output.amount() * output.probability() * 20 / calculated.ticks();
            var widget = new ResultWidget(output.name(), Component.literal(format(rate) + (output.volume() ? " mB/s" : "/s") + (chance ? " *" : "")));
            widget.setTooltipMessage(Component.translatable(chance ? "mekpreview.nova.expected" : "mekpreview.nova.theoretical"),
                    Component.literal(format(output.amount()) + (output.volume() ? " mB" : "") + " / " + calculated.ticks() + " t"),
                    Component.literal(format(output.probability() * 100) + "%"));
            consumer.accept(widget);
        }
        if (calculated.generator()) for (var input : calculated.inputs())
            consumer.accept(new ResultWidget(input.name(), Component.literal(format(input.amount() * 20 / calculated.ticks()) + (input.volume() ? " mB/s" : "/s"))));
        if (!calculated.module().isEmpty()) row(consumer, "module", calculated.module().getHoverName());
        if (calculated.mana() > 0) row(consumer, "mana", Component.literal(format(calculated.mana()) + "/op"));
    }
    private static String format(double value) {
        if (value != 0 && Math.abs(value) < 0.0001) return String.format(Locale.ROOT, "%.3g", value);
        return String.format(Locale.ROOT, value >= 1000 || value == Math.rint(value) ? "%,.0f" : "%,.4f", value)
                .replaceAll("(\\.\\d*?)0+$", "$1").replaceAll("\\.$", "");
    }
    private static String energy(long value) {
        if (value >= 1_000_000_000) return format(value / 1_000_000_000.0) + " GJ";
        if (value >= 1_000_000) return format(value / 1_000_000.0) + " MJ";
        if (value >= 1_000) return format(value / 1_000.0) + " kJ";
        return value + " J";
    }
    private static void row(Consumer<AbstractWidget> consumer, String key, Component value) {
        consumer.accept(new ResultWidget(Component.translatable("mekpreview.nova." + key), value));
    }
    @Override public void save(net.minecraft.nbt.CompoundTag tag) {
        super.save(tag);
        if (recipeControl == null) return;
        tag.putInt("NovaRecipe", recipeIndex()); tag.putInt("NovaCoil", coil());
        tag.putString("NovaRecipeKey", performance.recipeKey(recipeIndex()));
        tag.putInt("NovaParallel", parallelControl.getSlider().getValue());
        tag.putInt("NovaSpeed", speedControl.getSlider().getValue()); tag.putInt("NovaEnergy", energyControl.getSlider().getValue());
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        if (!tag.contains("NovaRecipe")) return;
        int savedRecipe = tag.getInt("NovaRecipe");
        if (tag.contains("NovaRecipeKey") && (savedRecipe < 0 || savedRecipe >= performance.recipes().size()
                || !performance.recipeKey(savedRecipe).equals(tag.getString("NovaRecipeKey"))))
            savedRecipe = performance.findRecipeKey(tag.getString("NovaRecipeKey"));
        missingSavedRecipe = savedRecipe < 0 && !performance.recipes().isEmpty();
        if (savedRecipe >= 0) setRecipeIndex(savedRecipe);
        setCoil(tag.getInt("NovaCoil")); setParallelTier(tag.getInt("NovaParallel"));
        setSpeedUpgrades(tag.getInt("NovaSpeed")); setEnergyUpgrades(tag.getInt("NovaEnergy"));
    }
    @Override public Block getGlassBlock() { return null; }
    @Override public int getDimensionWidthMin() { return size == null ? 3 : size.getX(); }
    @Override public int getDimensionWidthMax() { return getDimensionWidthMin(); }
    @Override public int getDimensionHeightMin() { return size == null ? 3 : size.getY(); }
    @Override public int getDimensionHeightMax() { return getDimensionHeightMin(); }
    @Override public int getDimensionLengthMin() { return size == null ? 3 : size.getZ(); }
    @Override public int getDimensionLengthMax() { return getDimensionLengthMin(); }
    @Override public int getDimensionWidth() { return getDimensionWidthMin(); }
    @Override public int getDimensionHeight() { return getDimensionHeightMin(); }
    @Override public int getDimensionLength() { return getDimensionLengthMin(); }
    private static Object field(Object source, String name) {
        try { return source.getClass().getField(name).get(source); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Nova structure entry: " + name, e); }
    }
}
