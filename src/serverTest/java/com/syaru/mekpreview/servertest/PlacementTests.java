package com.syaru.mekpreview.servertest;

import com.syaru.mekpreview.build.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import java.util.function.Consumer;

@GameTestHolder("mekpreview_server_test")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class PlacementTests {
    private static final BlockPos TARGET = new BlockPos(3, 2, 3);
    private static Player player(GameTestHelper h) {
        h.setBlock(TARGET, Blocks.AIR);
        h.setBlock(TARGET.below(), Blocks.STONE);
        var player = h.makeMockSurvivalPlayer();
        var pos = h.absolutePos(TARGET);
        player.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + 2.5, 180, 0);
        player.getAbilities().mayBuild = true;
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        return player;
    }
    private static PlacementService.Result place(GameTestHelper h, Player p) {
        return PlacementService.place(h.getLevel(), p,
                new BuildNetwork.Placement(h.absolutePos(TARGET), Blocks.STONE.defaultBlockState()), 6, false);
    }
    private static void equal(GameTestHelper h, Object expected, Object actual) {
        h.assertTrue(expected.equals(actual), "Expected " + expected + ", got " + actual);
    }
    @GameTest(template = "empty") public static void consumesSurvivalMaterial(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(12, new ItemStack(Items.STONE, 2));
        equal(h, PlacementService.Result.PLACED, place(h, p));
        equal(h, 1, p.getInventory().getItem(12).getCount());
        h.assertBlockPresent(Blocks.STONE, TARGET); h.succeed();
    }
    @GameTest(template = "empty") public static void missingMaterialDoesNotPlace(GameTestHelper h) {
        var p = player(h); equal(h, PlacementService.Result.MISSING_MATERIAL, place(h, p));
        h.assertBlockPresent(Blocks.AIR, TARGET); h.succeed();
    }
    @GameTest(template = "empty") public static void doesNotOverwriteSolidBlocks(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
        h.setBlock(TARGET, Blocks.CHEST);
        equal(h, PlacementService.Result.BLOCKED, place(h, p));
        equal(h, 2, p.getInventory().getItem(0).getCount());
        h.assertBlockPresent(Blocks.CHEST, TARGET); h.succeed();
    }
    @GameTest(template = "empty") public static void respectsForgePlacementCancellation(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
        Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
            if (event.getEntity() == p) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            equal(h, PlacementService.Result.BLOCKED, place(h, p));
            h.assertBlockPresent(Blocks.AIR, TARGET);
            equal(h, 2, p.getInventory().getItem(0).getCount());
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
        h.succeed();
    }
    @GameTest(template = "empty") public static void enforcesReach(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Items.STONE));
        p.moveTo(p.getX() + 100, p.getY(), p.getZ());
        equal(h, PlacementService.Result.OUT_OF_RANGE, place(h, p));
        h.assertBlockPresent(Blocks.AIR, TARGET); h.succeed();
    }
    @GameTest(template = "empty") public static void respectsBuildPermission(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Items.STONE));
        p.getAbilities().mayBuild = false;
        equal(h, PlacementService.Result.BLOCKED, place(h, p));
        h.assertBlockPresent(Blocks.AIR, TARGET); h.succeed();
    }
    @GameTest(template = "empty") public static void preservesOrientationAndItemNbt(GameTestHelper h) {
        var p = player(h); var material = new ItemStack(Items.OAK_LOG, 2);
        material.getOrCreateTag().putString("Preserved", "yes");
        p.getInventory().setItem(2, material); p.getInventory().selected = 2;
        var expected = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        equal(h, PlacementService.Result.PLACED, PlacementService.place(h.getLevel(), p,
                new BuildNetwork.Placement(h.absolutePos(TARGET), expected), 6, false));
        h.assertTrue(h.getBlockState(TARGET).equals(expected), "Orientation was lost");
        equal(h, "yes", material.getTag().getString("Preserved")); equal(h, 1, material.getCount());
        h.assertTrue(!material.getTag().contains("BlockStateTag"), "Placement mutated source item NBT"); h.succeed();
    }
    @GameTest(template = "empty") public static void prefersPlainMaterialOverConfiguredMachine(GameTestHelper h) {
        var p = player(h); var tagged = new ItemStack(Items.STONE, 2); tagged.getOrCreateTag().putInt("Energy", 500);
        p.getInventory().setItem(0, tagged); p.getInventory().setItem(12, new ItemStack(Items.STONE, 2));
        equal(h, PlacementService.Result.PLACED, place(h, p));
        equal(h, 2, tagged.getCount()); equal(h, 1, p.getInventory().getItem(12).getCount()); h.succeed();
    }
    @GameTest(template = "empty") public static void protectsUnselectedTaggedMaterials(GameTestHelper h) {
        var p = player(h); var tagged = new ItemStack(Items.STONE, 2); tagged.getOrCreateTag().putInt("Energy", 500);
        p.getInventory().setItem(12, tagged);
        equal(h, PlacementService.Result.MISSING_MATERIAL, place(h, p));
        equal(h, 2, tagged.getCount()); h.assertBlockPresent(Blocks.AIR, TARGET); h.succeed();
    }
    @GameTest(template = "empty") public static void refillRequiresExactNbtAndLeavesSourcesUntouched(GameTestHelper h) {
        var p = player(h); var wanted = new ItemStack(Items.STONE); wanted.getOrCreateTag().putInt("Energy", 500);
        var different = wanted.copy(); different.getOrCreateTag().putInt("Energy", 100);
        p.getInventory().setItem(10, different); p.getInventory().setItem(11, new ItemStack(Items.STONE));
        equal(h, -1, MaterialInventory.refillSlot(p.getInventory(), wanted, 0));
        p.getInventory().setItem(12, wanted.copy());
        equal(h, 12, MaterialInventory.refillSlot(p.getInventory(), wanted, 0));
        equal(h, 500, p.getInventory().getItem(12).getTag().getInt("Energy"));
        equal(h, 11, MaterialInventory.plainSlot(p.getInventory(), Items.STONE)); h.succeed();
    }
    @GameTest(template = "empty") public static void creativeDoesNotConsume(GameTestHelper h) {
        var p = player(h); p.getAbilities().instabuild = true;
        p.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
        equal(h, PlacementService.Result.PLACED, place(h, p));
        equal(h, 2, p.getInventory().getItem(0).getCount()); h.succeed();
    }
    @GameTest(template = "empty") public static void alreadyPlacedDoesNotConsume(GameTestHelper h) {
        var p = player(h); p.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
        h.setBlock(TARGET, Blocks.STONE);
        equal(h, PlacementService.Result.ALREADY_PRESENT, place(h, p));
        equal(h, 2, p.getInventory().getItem(0).getCount()); h.succeed();
    }
    @GameTest(template = "empty") public static void packetBoundsRejectMalformedRequests(GameTestHelper h) {
        player(h);
        boolean rejected = false;
        try { new BuildNetwork.Request(true, java.util.List.of()); } catch (IllegalArgumentException e) { rejected = true; }
        h.assertTrue(rejected, "Empty packet was accepted");
        equal(h, PlacementService.Result.BLOCKED, PlacementService.place(h.getLevel(), player(h),
                new BuildNetwork.Placement(h.absolutePos(TARGET), Blocks.AIR.defaultBlockState()), 6, false));
        h.succeed();
    }
}
