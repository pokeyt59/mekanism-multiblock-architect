package com.syaru.mekpreview.client;

import com.syaru.mekpreview.MekPreview;
import com.syaru.mekpreview.build.BuildNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = MekPreview.MOD_ID, value = Dist.CLIENT)
public final class BuildControls {
    @SubscribeEvent public static void use(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || mc.screen != null
                || mc.player == null || !Hologram.shown() || Hologram.target() == null) return;
        InventoryAssist.arm();
        // A visual-only server can still use ordinary manual placement and vanilla inventory assistance.
        if (!serverSupported() && !mc.player.isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        request(mc.player.isShiftKeyDown());
    }
    public static void request(boolean batch) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !Hologram.active()) return;
        if (!serverSupported()) {
            mc.player.displayClientMessage(Component.translatable("mekpreview.build.server_required"), true);
            return;
        }
        List<BuildNetwork.Placement> blocks;
        if (batch) {
            blocks = Hologram.ghosts().stream().filter(Hologram.Ghost::visible).map(g ->
                    new BuildNetwork.Placement(Hologram.origin().offset(g.local()), g.state())).toList();
        } else {
            var target = Hologram.target();
            if (target == null) return;
            blocks = List.of(new BuildNetwork.Placement(target.position(), target.expected()));
        }
        BuildNetwork.CHANNEL.sendToServer(new BuildNetwork.Request(batch, blocks));
    }
    public static boolean serverSupported() {
        var mc = Minecraft.getInstance();
        return mc.getConnection() != null && BuildNetwork.CHANNEL.isRemotePresent(mc.getConnection().getConnection());
    }
    private BuildControls() {}
}
