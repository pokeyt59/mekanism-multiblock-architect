package com.syaru.mekpreview.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Apply alpha to baked block vertices without changing global vanilla render types. */
final class GhostVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alpha;
    GhostVertexConsumer(VertexConsumer delegate, float alpha) { this.delegate = delegate; this.alpha = alpha; }
    @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
    @Override public VertexConsumer color(int r, int g, int b, int a) {
        delegate.color(r, g, b, (int) (a * alpha)); return this;
    }
    @Override public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
    @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
    @Override public void endVertex() { delegate.endVertex(); }
    @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, (int) (a * alpha)); }
    @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
}
