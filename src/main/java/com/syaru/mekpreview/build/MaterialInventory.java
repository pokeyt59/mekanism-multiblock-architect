package com.syaru.mekpreview.build;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import java.util.*;

/** Automatic selection never guesses which configured machine the player wants to consume. */
public final class MaterialInventory {
    public static int plainSlot(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.items.size(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(item) && !stack.hasTag()) return i;
        }
        return -1;
    }
    public static int placementSlot(Inventory inventory, Item item) {
        int plain = plainSlot(inventory, item);
        if (plain >= 0) return plain;
        var held = inventory.getSelected();
        return !held.isEmpty() && held.is(item) ? inventory.selected : -1;
    }
    public static int refillSlot(Inventory inventory, ItemStack reference, int destination) {
        if (reference.isEmpty()) return -1;
        for (int i = 0; i < inventory.items.size(); i++) {
            var candidate = inventory.getItem(i);
            if (i != destination && !candidate.isEmpty() && ItemStack.isSameItemSameTags(reference, candidate)) return i;
        }
        return -1;
    }
    public static Map<String, Long> available(Inventory inventory) {
        Map<String, Long> result = new HashMap<>();
        for (int i = 0; i < inventory.items.size(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty() && (!stack.hasTag() || i == inventory.selected))
                result.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), (long) stack.getCount(), Long::sum);
        }
        return Map.copyOf(result);
    }
    private MaterialInventory() {}
}
