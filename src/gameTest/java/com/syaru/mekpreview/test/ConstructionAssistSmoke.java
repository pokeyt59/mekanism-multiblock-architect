package com.syaru.mekpreview.test;

import com.syaru.mekpreview.ClientOptions;
import com.syaru.mekpreview.client.*;
import com.syaru.mekpreview.structure.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Real vanilla inventory swap/carried packets, server consumption, scans and completion transitions. */
final class ConstructionAssistSmoke {
    private static Blueprint plan;
    private static boolean refill, hide, notify;
    private static int events;
    private static com.mojang.blaze3d.platform.InputConstants.Key pickKey;
    private static ItemStack tagged(int count, int energy) {
        var stack = new ItemStack(Items.STONE, count); stack.getOrCreateTag().putInt("Energy", energy); return stack;
    }
    static void tick(Minecraft mc, int tick) throws Exception {
        if (tick == 1480) {
            refill = ClientOptions.AUTO_REFILL.get(); hide = ClientOptions.HIDE_COMPLETED.get(); notify = ClientOptions.NOTIFY_COMPLETED.get();
            ClientOptions.AUTO_REFILL.set(true); ClientOptions.HIDE_COMPLETED.set(false); ClientOptions.NOTIFY_COMPLETED.set(true);
            pickKey = ClientEvents.PICK.getKey();
            ClientEvents.PICK.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_F8));
            net.minecraft.client.KeyMapping.resetMapping();
            mc.player.closeContainer(); mc.setScreen(null); mc.mouseHandler.releaseMouse(); Hologram.clear();
            plan = new Blueprint(StructureKind.DYNAMIC_TANK, new Dimensions(3, 2, 1), Map.of(
                    new Cell(0, 0, 0), new Part("minecraft:stone", false),
                    new Cell(1, 0, 0), new Part("minecraft:oak_log", "", true, Map.of("axis", "x")),
                    new Cell(2, 1, 0), new Part("minecraft:stone", false)), Map.of());
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                for (int x = 4; x <= 6; x++) for (int y = 183; y <= 184; y++) player.serverLevel().setBlockAndUpdate(new BlockPos(x, y, -5), Blocks.AIR.defaultBlockState());
                player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.getInventory().selected = 0;
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
                player.getInventory().setItem(10, tagged(2, 500));
                player.getInventory().setItem(12, new ItemStack(Items.STONE)); player.getInventory().setItem(13, new ItemStack(Items.STONE, 3));
                player.inventoryMenu.broadcastChanges(); player.connection.teleport(4.5, 180, -9.5, 0, -23);
            });
        }
        if (tick == 1520) {
            var settings = new PreviewSettings(StructureKind.DYNAMIC_TANK); settings.source = () -> plan; settings.dimensions = plan.dimensions(); settings.layer = 1;
            Hologram.start(settings); var origin = Hologram.origin(); Hologram.nudge(4 - origin.getX(), 183 - origin.getY(), -5 - origin.getZ());
            mc.player.setYRot(0); mc.player.setXRot(-23); Hologram.tick(); events = Hologram.completionEvents();
            require(Hologram.target() != null, "No hologram target for block-selection key");
            net.minecraft.client.KeyMapping.click(ClientEvents.PICK.getKey());
        }
        if (tick == 1540) {
            require(mc.player.getMainHandItem().is(Items.STONE) && mc.player.getMainHandItem().getCount() == 1 && !mc.player.getMainHandItem().hasTag(), "Selection chose configured material or wrong slot");
            require(mc.player.getInventory().getItem(12).is(Items.DIAMOND_PICKAXE), "Vanilla inventory swap lost the displaced tool");
            use(mc);
        }
        if (tick == 1570) {
            require(mc.level.getBlockState(new BlockPos(4, 183, -5)).is(Blocks.STONE), "Selected material was not placed on server");
            require(mc.player.getMainHandItem().getCount() == 3 && !mc.player.getMainHandItem().hasTag(), "Exhausted stack did not refill with plain matching material");
            require(mc.player.getInventory().getItem(13).isEmpty() && mc.player.getInventory().getItem(10).getCount() == 2, "Refill duplicated items or consumed tagged reserve");
            require(Hologram.correct() == 1 && Hologram.drawGhosts().stream().noneMatch(g -> g.ghost().local().equals(BlockPos.ZERO)), "Correct ghost was not hidden");
            var row = MaterialPlan.compare(plan, false, LayerMode.ALL, 1, false, Map.of(), false, BlockResolver::itemId,
                    Hologram.placedMaterials(plan, 0, false, LayerMode.ALL, 1, false)).stream().filter(r -> r.itemId().equals("minecraft:stone")).findFirst().orElseThrow();
            require(row.required() == 2 && row.placed() == 1 && row.remaining() == 1 && row.missing() == 1, "Placed blocks did not reduce material demand");
            require(Hologram.placedMaterials(plan, 1, false, LayerMode.ALL, 1, false).isEmpty(), "Different orientation reused placement counts");
            ClientSmoke.capture(mc, "assist-01-selected-refilled-materials.png");
            mc.getSingleplayerServer().execute(() -> {
                var p = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                p.serverLevel().setBlockAndUpdate(new BlockPos(5, 183, -5), Blocks.STONE.defaultBlockState());
                p.getInventory().setItem(2, new ItemStack(Items.OAK_LOG)); p.inventoryMenu.broadcastChanges();
                p.connection.teleport(5.5, 180, -9.5, 0, -23);
            });
        }
        if (tick == 1600) {
            mc.player.setYRot(0); mc.player.setXRot(-23); Hologram.tick();
            require(Hologram.wrong() == 1 && Hologram.drawGhosts().stream().anyMatch(Hologram.DrawGhost::wrong), "Wrong block not marked red");
            require(InventoryAssist.selectTarget(), "Hotbar log selection failed");
            Hologram.layer(-1); ClientSmoke.capture(mc, "assist-02-wrong-block-layer.png");
        }
        if (tick == 1620) {
            require(mc.player.getInventory().selected == 2 && mc.player.getMainHandItem().is(Items.OAK_LOG), "Hotbar selection did not switch held slot");
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(5, 183, -5),
                    Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)));
        }
        if (tick == 1640) {
            require(Hologram.wrong() == 1, "Wrong physical orientation was accepted");
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(5, 183, -5),
                    Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
        }
        if (tick == 1660) {
            require(Hologram.correct() == 2 && Hologram.shown() && Hologram.completionEvents() == events, "Visible layer completion falsely completed full structure");
            require(ConstructionMaterials.rows().isEmpty(), "Completed layer still demanded its placed blocks");
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(6, 184, -5), Blocks.STONE.defaultBlockState()));
        }
        if (tick == 1680) {
            require(Hologram.correct() == 3 && Hologram.shown() && Hologram.completionEvents() == events + 1, "Disabled auto-hide or completion notice failed");
            ClientOptions.HIDE_COMPLETED.set(true);
        }
        if (tick == 1700) {
            require(Hologram.shown() && Hologram.completionEvents() == events + 1, "Completed structure repeatedly hid/notified");
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(6, 184, -5), Blocks.AIR.defaultBlockState()));
        }
        if (tick == 1720) {
            require(Hologram.correct() == 2, "Removed block remained counted as installed");
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(6, 184, -5), Blocks.STONE.defaultBlockState()));
        }
        if (tick == 1740) {
            require(!Hologram.shown() && Hologram.active() && Hologram.completionEvents() == events + 2, "Completed hologram was not hidden or could not be recovered");
            Hologram.toggle(); Hologram.tick();
            require(Hologram.shown(), "Manual show immediately re-hid completed hologram");
            ClientSmoke.capture(mc, "assist-03-completion.png");
            ClientOptions.HIDE_COMPLETED.set(false);
            Hologram.cycleLayerMode();
            mc.getSingleplayerServer().execute(() -> {
                var p = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                p.serverLevel().setBlockAndUpdate(new BlockPos(4, 183, -5), Blocks.AIR.defaultBlockState());
                p.getInventory().clearContent(); p.getInventory().selected = 0;
                p.getInventory().setItem(0, tagged(1, 500)); p.getInventory().setItem(10, tagged(2, 100)); p.getInventory().setItem(12, tagged(4, 500));
                p.inventoryMenu.broadcastChanges(); p.connection.teleport(4.5, 180, -9.5, 0, -23);
            });
        }
        if (tick == 1770) {
            mc.player.getInventory().selected = 0;
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));
            mc.player.setYRot(0); mc.player.setXRot(-23); Hologram.tick();
            require(Hologram.target() != null && !InventoryAssist.selectTarget(), "Automatic pick selected an NBT-bearing item");
            ClientOptions.AUTO_REFILL.set(false); InventoryAssist.arm(); InventoryAssist.tick();
            require(mc.player.getMainHandItem().getCount() == 1, "Disabled refill changed inventory");
            ClientOptions.AUTO_REFILL.set(true); use(mc);
        }
        if (tick == 1800) {
            require(mc.player.getMainHandItem().getCount() == 4 && mc.player.getMainHandItem().getTag().getInt("Energy") == 500,
                    "Tagged refill mixed NBT or failed vanilla swap");
            require(mc.player.getInventory().getItem(10).getCount() == 2 && mc.player.getInventory().getItem(10).getTag().getInt("Energy") == 100,
                    "Differently configured reserve was modified");
            require(mc.player.getInventory().getItem(12).isEmpty(), "Tagged refill duplicated reserve");
            ClientOptions.AUTO_REFILL.set(false);
            mc.getSingleplayerServer().execute(() -> {
                var p = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                p.serverLevel().setBlockAndUpdate(new BlockPos(4, 183, -5), Blocks.AIR.defaultBlockState());
                p.getInventory().setItem(0, tagged(1, 500)); p.getInventory().setItem(12, tagged(2, 500)); p.inventoryMenu.broadcastChanges();
            });
        }
        if (tick == 1820) { Hologram.tick(); use(mc); }
        if (tick == 1840) {
            require(mc.player.getMainHandItem().isEmpty() && mc.player.getInventory().getItem(12).getCount() == 2,
                    "Refill still ran while disabled");
            Hologram.clear(); ClientOptions.AUTO_REFILL.set(refill); ClientOptions.HIDE_COMPLETED.set(hide); ClientOptions.NOTIFY_COMPLETED.set(notify);
            ClientEvents.PICK.setKey(pickKey); net.minecraft.client.KeyMapping.resetMapping();
            System.out.println("MEKPREVIEW_CONSTRUCTION_ASSIST_VERIFIED vanilla inventory selection/refill packets, NBT isolation, placed-material scopes, wrong orientations, full-plan completion and recoverable auto-hide");
        }
    }
    private static void use(Minecraft mc) {
        var event = new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(1, mc.options.keyUse, net.minecraft.world.InteractionHand.MAIN_HAND);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        require(event.isCanceled(), "Server-assisted right click was not consumed");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
