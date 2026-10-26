package com.syaru.mekpreview;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client preferences only; safe to register without any client-side classes on a server. */
public final class ClientOptions {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue AUTO_REFILL, HIDE_COMPLETED, NOTIFY_COMPLETED, MATERIAL_HUD;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("construction");
        AUTO_REFILL = builder.comment("Refill an exhausted held block stack during hologram building. Item and NBT must match exactly.")
                .define("autoRefill", true);
        HIDE_COMPLETED = builder.comment("Hide an anchored hologram once all planned blocks and physical orientations match. V can show it again.")
                .define("hideCompleted", true);
        NOTIFY_COMPLETED = builder.comment("Notify once when an anchored plan's blocks match. This does not validate machine formation or configuration.")
                .define("notifyCompleted", true);
        MATERIAL_HUD = builder.comment("Show remaining physical blocks and inventory shortages for the visible hologram layers.")
                .define("materialHud", true);
        builder.pop(); SPEC = builder.build();
    }
    private ClientOptions() {}
}
