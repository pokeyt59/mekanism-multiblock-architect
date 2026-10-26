package com.syaru.mekpreview.structure;

public enum StructureKind {
    DYNAMIC_TANK("dynamic_tank", "mekanism:dynamic_tank", 3, 3, 3, 18, 18, 18, 5, 5, 5, false),
    EVAPORATION("evaporation", "mekanism:thermal_evaporation_controller", 4, 3, 4, 4, 18, 4, 4, 6, 4, false),
    INDUCTION_MATRIX("induction_matrix", "mekanism:induction_casing", 3, 4, 3, 18, 18, 18, 5, 5, 5, false),
    BOILER("boiler", "mekanism:boiler_casing", 3, 5, 3, 18, 18, 18, 5, 6, 5, false),
    SPS("sps", "mekanism:sps_casing", 7, 7, 7, 7, 7, 7, 7, 7, 7, false),
    FISSION_REACTOR("fission_reactor", "mekanismgenerators:fission_reactor_casing", 3, 4, 3, 18, 18, 18, 5, 6, 5, true),
    FUSION_REACTOR("fusion_reactor", "mekanismgenerators:fusion_reactor_frame", 5, 5, 5, 5, 5, 5, 5, 5, 5, true),
    TURBINE("turbine", "mekanismgenerators:turbine_casing", 5, 5, 5, 17, 18, 17, 5, 7, 5, true);

    private final String id;
    private final String icon;
    private final Dimensions min;
    private final Dimensions max;
    private final Dimensions defaults;
    private final boolean generators;
    StructureKind(String id, String icon, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                  int defaultX, int defaultY, int defaultZ, boolean generators) {
        this.id = id;
        this.icon = icon;
        this.min = new Dimensions(minX, minY, minZ);
        this.max = new Dimensions(maxX, maxY, maxZ);
        this.defaults = new Dimensions(defaultX, defaultY, defaultZ);
        this.generators = generators;
    }
    public String id() { return id; }
    public String icon() { return icon; }
    public String translationKey() { return "mekpreview.structure." + id; }
    public Dimensions defaults() { return defaults; }
    public boolean requiresGenerators() { return generators; }
    public Dimensions normalize(Dimensions requested) {
        int x = clamp(requested.width(), min.width(), max.width());
        int y = clamp(requested.height(), min.height(), max.height());
        int z = clamp(requested.depth(), min.depth(), max.depth());
        if (this == TURBINE) {
            if (x % 2 == 0) x--;
            if (z % 2 == 0) z--;
        }
        return new Dimensions(x, y, z);
    }
    public Dimensions change(Dimensions current, int axis, int delta) {
        int step = this == TURBINE && axis != 1 ? 2 : 1;
        return normalize(new Dimensions(current.width() + (axis == 0 ? delta * step : 0),
                current.height() + (axis == 1 ? delta * step : 0),
                current.depth() + (axis == 2 ? delta * step : 0)));
    }
    private static int clamp(int n, int low, int high) { return Math.max(low, Math.min(high, n)); }
}
