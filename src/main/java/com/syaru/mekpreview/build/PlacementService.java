package com.syaru.mekpreview.build;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.Event;

/** Uses normal item placement and Forge placement events; never overwrites a solid block. */
public final class PlacementService {
    public enum Result { PLACED, ALREADY_PRESENT, MISSING_MATERIAL, BLOCKED, OUT_OF_RANGE }

    public static Result place(ServerLevel level, Player player, BuildNetwork.Placement request,
                               double range, boolean lineOfSight) {
        var pos = request.position();
        var expected = request.expected();
        if (expected.isAir() || !(expected.getBlock().asItem() instanceof BlockItem item)
                || item.getBlock() != expected.getBlock()) return Result.BLOCKED;
        if (player.isSpectator() || !player.getAbilities().mayBuild || !player.isAlive()
                || !level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)
                || !level.getWorldBorder().isWithinBounds(pos)) return Result.BLOCKED;
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > range * range) return Result.OUT_OF_RANGE;
        if (!level.mayInteract(player, pos)) return Result.BLOCKED;
        var actual = level.getBlockState(pos);
        if (matches(actual, expected)) return Result.ALREADY_PRESENT;
        if ((!actual.isAir() && !actual.canBeReplaced()) || level.getBlockEntity(pos) != null) return Result.BLOCKED;
        if (lineOfSight) {
            var obstruction = level.clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(pos),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (obstruction.getType() != HitResult.Type.MISS && !obstruction.getBlockPos().equals(pos))
                return Result.BLOCKED;
        }
        int slot = MaterialInventory.placementSlot(player.getInventory(), item);
        boolean creative = player.getAbilities().instabuild;
        if (slot < 0 && !creative) return Result.MISSING_MATERIAL;
        ItemStack source = slot < 0 ? new ItemStack(item) : player.getInventory().getItem(slot);
        ItemStack placing = source.copy();
        placing.setCount(1);
        // Keep machine item NBT, but only copy physical orientation from the guide.
        CompoundTag physical = new CompoundTag();
        for (var property : expected.getProperties())
            if (property.getName().equals("facing") || property.getName().equals("axis"))
                physical.putString(property.getName(), propertyName(expected, property));
        if (!physical.isEmpty()) placing.addTagElement("BlockStateTag", physical);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (!player.mayUseItemAt(pos, Direction.UP, placing)) return Result.BLOCKED;
        var interaction = ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        if (interaction.isCanceled() || interaction.getUseItem() == Event.Result.DENY) return Result.BLOCKED;
        var result = placing.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, placing, hit));
        if (!result.consumesAction() || !level.getBlockState(pos).is(expected.getBlock())) return Result.BLOCKED;
        if (!creative) source.shrink(1);
        player.getInventory().setChanged();
        return Result.PLACED;
    }
    private static <T extends Comparable<T>> String propertyName(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.state.properties.Property<T> property) {
        return property.getName(state.getValue(property));
    }
    private static boolean matches(net.minecraft.world.level.block.state.BlockState actual,
                                   net.minecraft.world.level.block.state.BlockState expected) {
        if (actual.getBlock() != expected.getBlock()) return false;
        for (var property : expected.getProperties())
            if ((property.getName().equals("facing") || property.getName().equals("axis"))
                    && !actual.getValue(property).equals(expected.getValue(property))) return false;
        return true;
    }
    private PlacementService() {}
}
