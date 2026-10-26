package com.syaru.mekpreview.build;

import com.syaru.mekpreview.MekPreview;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

/** Optional, server-authoritative building support. No client or recipe-viewer class references. */
public final class BuildNetwork {
    public static final int MAX_BLOCKS = 8192;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MekPreview.MOD_ID, "build"), () -> "2",
            BuildNetwork::compatible, BuildNetwork::compatible);
    private static boolean compatible(String version) {
        // Older servers selected arbitrary tagged stacks. Require the new safety policy on both ends.
        return version.equals("2") || version.equals(NetworkRegistry.ABSENT)
                || version.equals(NetworkRegistry.ACCEPTVANILLA);
    }
    public record Placement(BlockPos position, BlockState expected) {
        public Placement { position = position.immutable(); }
    }
    public record Request(boolean batch, List<Placement> blocks) {
        public Request {
            blocks = List.copyOf(blocks);
            if (blocks.isEmpty() || blocks.size() > MAX_BLOCKS || (!batch && blocks.size() != 1))
                throw new IllegalArgumentException("Invalid build request size");
        }
        static Request decode(FriendlyByteBuf buf) {
            boolean batch = buf.readBoolean();
            int size = buf.readVarInt();
            if (size < 1 || size > MAX_BLOCKS || (!batch && size != 1))
                throw new IllegalArgumentException("Invalid build request size");
            List<Placement> blocks = new ArrayList<>(size);
            for (int i = 0; i < size; i++) blocks.add(new Placement(buf.readBlockPos(), Block.stateById(buf.readVarInt())));
            return new Request(batch, blocks);
        }
        void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(batch); buf.writeVarInt(blocks.size());
            for (var block : blocks) { buf.writeBlockPos(block.position()); buf.writeVarInt(Block.getId(block.expected())); }
        }
    }
    public static void init() {
        CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Request::encode).decoder(Request::decode)
                .consumerMainThread((request, context) -> {
                    var player = context.get().getSender();
                    if (player != null) BuildQueue.submit(player, request);
                    context.get().setPacketHandled(true);
                }).add();
    }
    private BuildNetwork() {}
}
