package com.syaru.mekpreview.build;

import com.syaru.mekpreview.MekPreview;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Bounded work per tick, per-player cooldown, no chunk generation or free items. */
@Mod.EventBusSubscriber(modid = MekPreview.MOD_ID)
public final class BuildQueue {
    private static final Map<UUID, Job> JOBS = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    private static final class Job {
        final ServerPlayer player;
        final ResourceKey<Level> dimension;
        final Iterator<BuildNetwork.Placement> blocks;
        final boolean batch;
        int placed, missing, blocked;
        Job(ServerPlayer player, BuildNetwork.Request request) {
            this.player = player;
            dimension = player.level().dimension();
            batch = request.batch();
            Map<net.minecraft.core.BlockPos, BuildNetwork.Placement> unique = new LinkedHashMap<>();
            request.blocks().stream().sorted(Comparator.comparingInt(p -> p.position().getY()))
                    .forEach(p -> unique.putIfAbsent(p.position(), p));
            blocks = unique.values().iterator();
        }
    }
    public static void submit(ServerPlayer player, BuildNetwork.Request request) {
        long time = player.serverLevel().getGameTime();
        Long previous = LAST_REQUEST.get(player.getUUID());
        if (JOBS.containsKey(player.getUUID()) || (previous != null && time - previous < 4)) return;
        LAST_REQUEST.put(player.getUUID(), time);
        JOBS.put(player.getUUID(), new Job(player, request));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var jobs = JOBS.values().iterator();
        while (jobs.hasNext()) {
            Job job = jobs.next();
            var player = job.player;
            if (player.hasDisconnected() || !player.isAlive() || !player.level().dimension().equals(job.dimension)) {
                jobs.remove(); continue;
            }
            for (int i = 0; i < 16 && job.blocks.hasNext(); i++) {
                var result = PlacementService.place(player.serverLevel(), player, job.blocks.next(),
                        32, !job.batch);
                switch (result) {
                    case PLACED -> job.placed++;
                    case MISSING_MATERIAL -> job.missing++;
                    case BLOCKED, OUT_OF_RANGE -> job.blocked++;
                    default -> {}
                }
            }
            player.inventoryMenu.broadcastChanges();
            if (!job.blocks.hasNext()) {
                player.displayClientMessage(Component.translatable("mekpreview.build.result",
                        job.placed, job.missing, job.blocked), true);
                jobs.remove();
            }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { JOBS.clear(); LAST_REQUEST.clear(); }
    @SubscribeEvent public static void loggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        JOBS.remove(event.getEntity().getUUID()); LAST_REQUEST.remove(event.getEntity().getUUID());
    }
    private BuildQueue() {}
}
