package com.syaru.mekpreview.structure;

/** Facing is optional; it is rotated together with the world projection. */
public record Part(String blockId, String facing, boolean interior, java.util.Map<String, String> properties) {
    public Part { properties = java.util.Map.copyOf(properties); }
    public Part(String id, String facing, boolean interior) { this(id, facing, interior, java.util.Map.of()); }
    public Part(String id, boolean interior) { this(id, "", interior); }
}
