package com.syaru.mekpreview.jemm;

import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import java.lang.reflect.*;
import java.util.*;

/** Optional Nova bridge. Uses a detached native controller and its own timing/energy methods. */
public final class NovaPerformance {
    private static final String ROOT = "com.falcon2235.moremultiblock.";
    public record Flow(Component name, double amount, boolean volume, double probability) {}
    public record Recipe(Object nativeRecipe, List<Flow> outputs, List<Flow> inputs,
                         int requiredCoil, ItemStack module, Component note, int mana) {
        public Recipe { outputs = List.copyOf(outputs); inputs = List.copyOf(inputs); module = module.copy(); }
        public Component name() {
            return !outputs.isEmpty() ? outputs.get(0).name() : !inputs.isEmpty() ? inputs.get(0).name() : Component.literal("—");
        }
    }
    public record Result(int ticks, long joulesPerTick, boolean generator, List<Flow> outputs,
                         List<Flow> inputs, ItemStack module, Component note, int mana, boolean runnable, int batch) {
        public Result { outputs = List.copyOf(outputs); inputs = List.copyOf(inputs); module = module.copy(); }
    }
    private final BlockEntity controller;
    private final Object type;
    private final String kind;
    private final boolean parallel, primitive;
    private final List<Recipe> recipes;
    private final List<String> recipeKeys, recipeSources;

    public NovaPerformance(BlockState state) {
        controller = Objects.requireNonNull(((EntityBlock) state.getBlock()).newBlockEntity(BlockPos.ZERO, state));
        primitive = controller.getClass().getSimpleName().equals("PbfBlockEntity");
        parallel = controller.getClass().getSimpleName().equals("ControllerBlockEntity");
        type = primitive ? null : call(controller, "machineType");
        kind = primitive ? "PBF" : ((Enum<?>) type).name();
        recipes = loadRecipes();
        recipeSources = recipes.stream().map(this::recipeIdentity).toList();
        recipeKeys = recipeSources.stream().map(text -> {
            try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
            catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        }).toList();
    }
    public String kind() { return kind; }
    public boolean parallel() { return parallel; }
    public List<Recipe> recipes() { return recipes; }
    public String recipeKey(int index) { return recipes.isEmpty() ? "" : recipeKeys.get(index); }
    public int findRecipeKey(String key) { return recipeKeys.indexOf(key); }
    public int findRecipe(Object source, ResourceLocation id) {
        for (int i = 0; i < recipes.size(); i++) {
            Object candidate = recipes.get(i).nativeRecipe();
            if (source != null && candidate == source) return i;
            if (candidate instanceof net.minecraft.world.item.crafting.Recipe<?> r && id != null && id.equals(r.getId())) return i;
            if (candidate instanceof net.minecraft.world.item.crafting.Recipe<?> r
                    && source instanceof net.minecraft.world.item.crafting.Recipe<?> other && r.getId().equals(other.getId())) return i;
        }
        return -1;
    }
    public List<String> searchDocuments() {
        return java.util.stream.IntStream.range(0, recipes.size()).mapToObj(i -> {
            Recipe recipe = recipes.get(i);
            var text = new StringBuilder(recipeSources.get(i)).append(' ').append(recipe.name().getString());
            recipe.outputs().forEach(f -> text.append(' ').append(f.name().getString()));
            recipe.inputs().forEach(f -> text.append(' ').append(f.name().getString()));
            if (!recipe.module().isEmpty()) text.append(' ').append(recipe.module().getHoverName().getString());
            return text.toString();
        }).toList();
    }
    private String recipeIdentity(Recipe recipe) {
        if (recipe.nativeRecipe() instanceof net.minecraft.world.item.crafting.Recipe<?> nativeRecipe)
            return kind + ":" + nativeRecipe.getId();
        if (primitive) return "nova:primitive_blast_furnace";
        StringBuilder text = new StringBuilder(kind);
        for (String key : List.of("itemInput", "itemInput2", "itemInput3", "itemInput4", "itemInput5",
                "gasInput", "gasInput2", "fluidInput", "itemOutput", "itemOutput2", "itemOutput3", "itemOutput4",
                "gasOutput", "fluidOutput", "requiredUpgrade", "chanceOutput", "chancePercent", "coilTier", "manaCost", "ticks", "energyPerTick")) {
            Object value = field(recipe.nativeRecipe(), key);
            var tag = new net.minecraft.nbt.CompoundTag();
            if (value instanceof ItemStack item) text.append('|').append(item.save(tag));
            else if (value instanceof GasStack gas) text.append('|').append(gas.write(tag));
            else if (value instanceof FluidStack fluid) text.append('|').append(fluid.writeToNBT(tag));
            else text.append('|').append(value);
        }
        return text.toString();
    }
    public static List<BlockState> coils() {
        return registryBlocks("COIL_TIERS");
    }
    public static List<BlockState> processors() { return registryBlocks("PARALLEL_BLOCKS"); }
    public static int coilTier(BlockState state) { return ((Number) call(type("MMMRegistry"), "coilTierOf", state)).intValue(); }
    public static int parallelCount(BlockState state) { return ((Number) field(state.getBlock(), "parallel")).intValue(); }
    private static List<BlockState> registryBlocks(String name) {
        return ((List<?>) field(type("MMMRegistry"), name)).stream()
                .map(entry -> ((net.minecraft.world.level.block.Block) call(entry, "get")).defaultBlockState()).toList();
    }

    private List<Recipe> loadRecipes() {
        if (primitive) {
            var steel = new ItemStack((net.minecraft.world.item.Item) call(controller, "steelIngot"));
            return List.of(new Recipe(null, List.of(item(steel)), List.of(item(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)),
                    new Flow(Component.translatable("mekpreview.nova.coal"), number(controller, "FUEL_PER_OPERATION"), false, 1)),
                    0, ItemStack.EMPTY, null, 0));
        }
        if (!parallel) return ((List<?>) call(type("machine.ChemRecipes"), "get", type)).stream().map(this::chemical).toList();
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        List<Recipe> result = new ArrayList<>();
        if (kind.equals("SMELTING")) {
            for (var recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
                ItemStack[] inputs = recipe.getIngredients().get(0).getItems();
                if (inputs.length > 0) result.add(new Recipe(recipe, List.of(item(recipe.getResultItem(level.registryAccess()))),
                        List.of(item(inputs[0])), 0, ItemStack.EMPTY, null, 0));
            }
        } else {
            @SuppressWarnings("unchecked") var recipeType = (RecipeType<ItemStackToItemStackRecipe>) call(type, "recipeType");
            for (var recipe : level.getRecipeManager().getAllRecipesFor(recipeType)) {
                var inputs = recipe.getInput().getRepresentations();
                if (inputs.isEmpty()) continue;
                var input = inputs.get(0);
                var needed = new Flow(input.getHoverName(), recipe.getInput().getNeededAmount(input), false, 1);
                result.add(new Recipe(recipe, List.of(item(recipe.getOutput(input))), List.of(needed), 0, ItemStack.EMPTY, null, 0));
            }
        }
        result.removeIf(r -> r.outputs().stream().allMatch(f -> f.amount() <= 0));
        return List.copyOf(result);
    }
    private Recipe chemical(Object recipe) {
        List<Flow> outputs = new ArrayList<>(), inputs = new ArrayList<>();
        for (String key : List.of("itemOutput", "itemOutput2", "itemOutput3", "itemOutput4")) addItem(outputs, field(recipe, key), 1);
        addItem(outputs, field(recipe, "chanceOutput"), number(recipe, "chancePercent") / 100.0);
        addVolume(outputs, field(recipe, "gasOutput")); addVolume(outputs, field(recipe, "fluidOutput"));
        for (String key : List.of("itemInput", "itemInput2", "itemInput3", "itemInput4", "itemInput5")) addItem(inputs, field(recipe, key), 1);
        addVolume(inputs, field(recipe, "gasInput")); addVolume(inputs, field(recipe, "gasInput2")); addVolume(inputs, field(recipe, "fluidInput"));
        if (kind.equals("VOID_MINER") && !outputs.isEmpty()) {
            int total = ((Number) call(type("machine.VoidOreTable"), "totalWeight")).intValue();
            ItemStack output = (ItemStack) field(recipe, "itemOutput");
            for (var entry : (List<?>) call(type("machine.VoidOreTable"), "entries")) {
                if (ItemStack.isSameItemSameTags((ItemStack) call(entry, "stack"), output)) {
                    Flow old = outputs.get(0);
                    outputs.set(0, new Flow(old.name(), old.amount(), false, ((Number) call(entry, "weight")).doubleValue() / total));
                    break;
                }
            }
        }
        return new Recipe(recipe, outputs, inputs, (int) number(recipe, "coilTier"), (ItemStack) field(recipe, "requiredUpgrade"),
                (Component) field(recipe, "note"), (int) number(recipe, "manaCost"));
    }
    public Result calculate(int index, int coil, int parallelCount, int speed, int energy) {
        if (recipes.isEmpty()) return new Result(0, 0, false, List.of(), List.of(), ItemStack.EMPTY, null, 0, false, 0);
        Recipe recipe = recipes.get(Math.max(0, Math.min(index, recipes.size() - 1)));
        if (primitive) return new Result((int) number(controller, "TICKS_REQUIRED"), 0, false, recipe.outputs(), recipe.inputs(),
                ItemStack.EMPTY, null, 0, true, 1);
        var upgrades = (ItemStackHandler) call(controller, "getUpgrades");
        upgrades.setStackInSlot(0, upgrade("speed", speed)); upgrades.setStackInSlot(1, upgrade("energy", energy));
        call(controller, "recomputeUpgrades");
        if (parallel) {
            int batch = Math.min(parallelCount, batchLimit(recipe));
            List<Flow> outputs = scale(recipe.outputs(), batch), inputs = scale(recipe.inputs(), batch);
            return new Result((int) number(controller, "ticksRequired"), (long) number(controller, "energyPerTick") * batch,
                    false, outputs, inputs, recipe.module(), recipe.note(), recipe.mana(), batch > 0, batch);
        }
        setField(controller, "coilTier", coil);
        if (kind.equals("COMBUSTION_GENERATOR") || kind.equals("ANNIHILATION_GENERATOR")) {
            String key = kind.equals("ANNIHILATION_GENERATOR") ? "annihilationJPerTick"
                    : index == 1 ? "combustionGasolineJPerTick" : "combustionJPerTick";
            long generated = ((Number) call(type("MMMConfig"), key)).longValue();
            return new Result(20, generated, true, List.of(), recipe.inputs(), recipe.module(), recipe.note(), 0,
                    generated <= number(controller, "CAPACITY"), 1);
        }
        if (kind.equals("VOID_MINER")) {
            int ticks = Math.max(1, (int) Math.round(((Number) call(type("machine.VoidOreTable"), "rollIntervalTicks")).doubleValue()
                    * number(controller, "upgradeTimeFactor")));
            long cost = Math.max(1, (long) Math.ceil(((Number) call(type("machine.VoidOreTable"), "energyPerTick")).doubleValue()
                    * number(controller, "upgradeEnergyFactor")));
            return new Result(ticks, cost, false, recipe.outputs(), recipe.inputs(), recipe.module(), recipe.note(), 0,
                    cost <= number(controller, "CAPACITY") && (boolean) call(controller, "fitsItemStack", field(recipe.nativeRecipe(), "itemOutput")), 1);
        }
        long cost = ((Number) call(controller, "effectiveEnergyPerTick", recipe.nativeRecipe())).longValue();
        boolean fits = cost <= number(controller, "CAPACITY") && (boolean) call(controller, "fitsItemOutputs", recipe.nativeRecipe())
                && (boolean) call(controller, "fitsGasOutput", field(recipe.nativeRecipe(), "gasOutput"))
                && (boolean) call(controller, "fitsFluidOutput", field(recipe.nativeRecipe(), "fluidOutput"));
        return new Result(((Number) call(controller, "effectiveTicks", recipe.nativeRecipe())).intValue(),
                cost, false, recipe.outputs(), recipe.inputs(), recipe.module(), recipe.note(), recipe.mana(), coil >= recipe.requiredCoil() && fits, 1);
    }
    private int batchLimit(Recipe recipe) {
        ItemStack input, output;
        if (recipe.nativeRecipe() instanceof SmeltingRecipe smelting) {
            input = smelting.getIngredients().get(0).getItems()[0];
            output = smelting.getResultItem(Minecraft.getInstance().level.registryAccess());
        } else {
            var processing = (ItemStackToItemStackRecipe) recipe.nativeRecipe();
            input = processing.getInput().getRepresentations().get(0); output = processing.getOutput(input);
        }
        int inputLimit = (int) (number(controller, "INPUT_SLOTS") * input.getMaxStackSize() / recipe.inputs().get(0).amount());
        int outputLimit = (int) (number(controller, "OUTPUT_SLOTS") * output.getMaxStackSize() / recipe.outputs().get(0).amount());
        return Math.max(0, Math.min(inputLimit, outputLimit));
    }
    private static List<Flow> scale(List<Flow> flows, int factor) {
        return flows.stream().map(f -> new Flow(f.name(), f.amount() * factor, f.volume(), f.probability())).toList();
    }
    private static ItemStack upgrade(String id, int count) {
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("mekanism", "upgrade_" + id)), count);
    }
    private static Flow item(ItemStack stack) { return new Flow(stack.getHoverName(), stack.getCount(), false, 1); }
    private static void addItem(List<Flow> list, Object value, double probability) {
        var stack = (ItemStack) value;
        if (!stack.isEmpty()) list.add(new Flow(stack.getHoverName(), stack.getCount(), false, probability));
    }
    private static void addVolume(List<Flow> list, Object value) {
        if (value instanceof GasStack gas && !gas.isEmpty()) list.add(new Flow(gas.getTextComponent(), gas.getAmount(), true, 1));
        if (value instanceof FluidStack fluid && !fluid.isEmpty()) list.add(new Flow(fluid.getDisplayName(), fluid.getAmount(), true, 1));
    }
    static Object field(Object object, String name) {
        try { return findField(object instanceof Class<?> c ? c : object.getClass(), name).get(object instanceof Class<?> ? null : object); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Nova field: " + name, e); }
    }
    private static double number(Object object, String name) { return ((Number) field(object, name)).doubleValue(); }
    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }
    private static void setField(Object object, String name, Object value) {
        try { findField(object.getClass(), name).set(object, value); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Nova field: " + name, e); }
    }
    static Object call(Object object, String name, Object... args) {
        Class<?> type = object instanceof Class<?> c ? c : object.getClass();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) for (Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            try { m.setAccessible(true); return m.invoke(object instanceof Class<?> ? null : object, args); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException("Nova calculation failed: " + name, e); }
        }
        throw new IllegalStateException("Unsupported Nova method: " + name);
    }
    private static Class<?> type(String suffix) {
        try { return Class.forName(ROOT + suffix); }
        catch (ClassNotFoundException e) { throw new IllegalStateException("Nova API unavailable", e); }
    }
}
