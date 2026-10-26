package com.syaru.mekpreview.structure;

public record Dimensions(int width, int height, int depth) {
    public int longest() { return Math.max(width, Math.max(height, depth)); }
    public boolean contains(Cell cell) {
        return cell.x() >= 0 && cell.y() >= 0 && cell.z() >= 0
                && cell.x() < width && cell.y() < height && cell.z() < depth;
    }
}
