package com.syaru.mekpreview.structure;

/** Structure-local coordinates, independent of a loaded Minecraft world. */
public record Cell(int x, int y, int z) {
    public Cell rotate(int width, int depth, int quarterTurns) {
        return switch (Math.floorMod(quarterTurns, 4)) {
            case 1 -> new Cell(depth - 1 - z, y, x);
            case 2 -> new Cell(width - 1 - x, y, depth - 1 - z);
            case 3 -> new Cell(z, y, width - 1 - x);
            default -> this;
        };
    }
}
