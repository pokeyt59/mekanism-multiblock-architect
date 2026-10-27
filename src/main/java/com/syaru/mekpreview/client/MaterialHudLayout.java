package com.syaru.mekpreview.client;

import java.util.List;

/** Pixel-based scrolling keeps every row reachable, including wrapped names. */
public final class MaterialHudLayout {
    public static final int TOP = 90, HEADER = 13, FOOTER = 12, BOTTOM_MARGIN = 54;
    public record View(int height, int contentHeight, int offset, int first, int end, boolean scrollable) {
        public int maxOffset() { return Math.max(0, contentHeight - height); }
    }
    public static View fit(int screenHeight, List<Integer> rowHeights, int requestedOffset) {
        int total = rowHeights.stream().mapToInt(h -> Math.max(1, h)).sum();
        int available = Math.max(0, screenHeight - BOTTOM_MARGIN - TOP - HEADER);
        boolean scrollable = total > available;
        int height = Math.min(total, Math.max(0, available - (scrollable ? FOOTER : 0)));
        int offset = Math.max(0, Math.min(Math.max(0, total - height), requestedOffset));
        int first = rowHeights.size(), end = first, top = 0;
        for (int i = 0; i < rowHeights.size(); i++) {
            int bottom = top + Math.max(1, rowHeights.get(i));
            // Do not draw a sliver of an otherwise fitting row at either edge.
            // Exceptionally tall wrapped rows still scroll in pixels so their full text remains reachable.
            boolean complete = top >= offset && bottom <= offset + height;
            if (height > 0 && bottom > offset && top < offset + height && (complete || bottom - top > height)) {
                first = Math.min(first, i); end = i + 1;
            }
            top = bottom;
        }
        return new View(height, total, offset, first, end, scrollable);
    }
    private MaterialHudLayout() {}
}
