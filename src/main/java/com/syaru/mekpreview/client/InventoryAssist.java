package com.syaru.mekpreview.client;

import com.syaru.mekpreview.ClientOptions;
import com.syaru.mekpreview.build.MaterialInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;

/** Uses vanilla inventory clicks and carried-slot packets; never edits inventory contents directly. */
public final class InventoryAssist {
    private static ItemStack reference = ItemStack.EMPTY;
    private static int slot = -1;
    private static long until;
    private InventoryAssist() {}
    private static boolean ready(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.gameMode != null && mc.getConnection() != null
                && mc.screen == null && mc.player.isAlive() && !mc.player.isSpectator()
                && mc.player.containerMenu == mc.player.inventoryMenu && mc.player.containerMenu.getCarried().isEmpty();
    }
    public static void reset() { reference = ItemStack.EMPTY; slot = -1; until = 0; }
    public static boolean selectTarget() {
        var mc = Minecraft.getInstance();
        if (!ready(mc) || !Hologram.shown() || Hologram.target() == null) return false;
        var item = Hologram.target().expected().getBlock().asItem();
        var inventory = mc.player.getInventory();
        int source = MaterialInventory.plainSlot(inventory, item);
        if (source < 0) {
            boolean tagged = inventory.items.stream().anyMatch(s -> !s.isEmpty() && s.is(item));
            mc.player.displayClientMessage(Component.translatable(tagged ? "mekpreview.pick.tagged" : "mekpreview.pick.missing"), true);
            return false;
        }
        reset();
        if (source < 9) {
            inventory.selected = source;
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(source));
        } else swap(mc, source, inventory.selected);
        return true;
    }
    private static void swap(Minecraft mc, int source, int destination) {
        int menuSlot = source < 9 ? source + 36 : source;
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, menuSlot, destination, ClickType.SWAP, mc.player);
    }
    public static void arm() {
        var mc = Minecraft.getInstance();
        if (!ClientOptions.AUTO_REFILL.get() || !ready(mc) || !Hologram.shown() || Hologram.target() == null) return;
        var held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem block) || block.getBlock() != Hologram.target().expected().getBlock()) return;
        reference = held.copy(); slot = mc.player.getInventory().selected; until = mc.level.getGameTime() + 40;
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (!ready(mc) || !Hologram.shown() || !ClientOptions.AUTO_REFILL.get() || reference.isEmpty()
                || slot != mc.player.getInventory().selected || mc.level.getGameTime() > until) { reset(); return; }
        var held = mc.player.getMainHandItem();
        if (!held.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(held, reference)) reset();
            return;
        }
        int source = MaterialInventory.refillSlot(mc.player.getInventory(), reference, slot);
        if (source >= 0) swap(mc, source, slot);
        reset();
    }
}
