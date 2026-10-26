package com.syaru.mekpreview.client;

import com.syaru.mekpreview.structure.*;
import java.util.function.Supplier;

/** Display options; structure content is supplied by the live JEMM recipe. */
public final class PreviewSettings {
    public final StructureKind kind;
    public Dimensions dimensions;
    public LayerMode layerMode = LayerMode.ALL;
    public int layer;
    public boolean hideShell;
    public int rotation;
    public String displayName;
    public Supplier<Blueprint> source;
    public PreviewSettings(StructureKind kind) {
        this.kind = kind;
        dimensions = kind.defaults();
        layer = dimensions.height() - 1;
    }
    public PreviewSettings copy() {
        PreviewSettings copy = new PreviewSettings(kind);
        copy.dimensions = dimensions;
        copy.layerMode = layerMode;
        copy.layer = layer;
        copy.hideShell = hideShell;
        copy.rotation = rotation;
        copy.displayName = displayName;
        copy.source = source;
        return copy;
    }
    public Blueprint blueprint() {
        if (source == null) throw new IllegalStateException("No JEMM design is attached");
        return source.get();
    }
    public net.minecraft.network.chat.Component title() {
        return displayName == null ? net.minecraft.network.chat.Component.translatable(kind.translationKey())
                : net.minecraft.network.chat.Component.literal(displayName);
    }
}
