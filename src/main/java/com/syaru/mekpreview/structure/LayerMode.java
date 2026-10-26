package com.syaru.mekpreview.structure;

public enum LayerMode {
    ALL, SINGLE, BELOW;
    public boolean includes(int y, int layer) {
        return this == ALL || (this == SINGLE ? y == layer : y <= layer);
    }
    public LayerMode next() { return values()[(ordinal() + 1) % values().length]; }
    public String translationKey() { return "mekpreview.layer." + name().toLowerCase(java.util.Locale.ROOT); }
}
