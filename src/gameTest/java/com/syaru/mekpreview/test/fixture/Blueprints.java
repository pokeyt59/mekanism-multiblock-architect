package com.syaru.mekpreview.test.fixture;

import com.syaru.mekpreview.structure.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Legacy independent layouts retained only as geometry regression fixtures, never shipped. */
public final class Blueprints {
    private final StructureKind kind;
    private final Dimensions size;
    private final Map<Cell, Part> blocks = new LinkedHashMap<>();
    private final Map<String, Integer> extras = new LinkedHashMap<>();
    private final boolean glass;
    private Blueprints(StructureKind kind, Dimensions requested, boolean glass) {
        this.kind = kind;
        this.size = kind.normalize(requested);
        this.glass = glass;
    }
    public static Blueprint create(StructureKind kind, Dimensions size, boolean glass) {
        Blueprints b = new Blueprints(kind, size, glass);
        b.build();
        return new Blueprint(kind, b.size, b.blocks, b.extras);
    }
    private void build() {
        switch (kind) {
            case DYNAMIC_TANK -> {
                shell("mekanism:dynamic_tank", "mekanism:structural_glass");
                ports("mekanism:dynamic_valve");
            }
            case EVAPORATION -> evaporation();
            case INDUCTION_MATRIX -> {
                shell("mekanism:induction_casing", "mekanism:structural_glass");
                ports("mekanism:induction_port");
                put(1, 1, 1, "mekanism:basic_induction_cell", true);
                put(1, 2, 1, "mekanism:basic_induction_provider", true);
            }
            case BOILER -> {
                shell("mekanism:boiler_casing", "mekanism:structural_glass");
                ports("mekanism:boiler_valve");
                put(1, 1, 1, "mekanism:superheating_element", true);
                plane(size.height() - 3, "mekanism:pressure_disperser");
            }
            case SPS -> roundedSps();
            case FISSION_REACTOR -> {
                shell("mekanismgenerators:fission_reactor_casing", "mekanismgenerators:reactor_glass");
                ports("mekanismgenerators:fission_reactor_port");
                put(0, 1, size.depth() / 2, "mekanismgenerators:fission_reactor_port", false);
                put(size.width() - 1, 1, size.depth() / 2, "mekanismgenerators:fission_reactor_port", false);
                for (int x = 1; x < size.width() - 1; x++) {
                    for (int z = 1; z < size.depth() - 1; z++) {
                        if (x % 2 == z % 2) {
                            for (int y = 1; y < size.height() - 2; y++) {
                                put(x, y, z, "mekanismgenerators:fission_fuel_assembly", true);
                            }
                            put(x, size.height() - 2, z, "mekanismgenerators:control_rod_assembly", true);
                        }
                    }
                }
            }
            case FUSION_REACTOR -> fusion();
            case TURBINE -> turbine();
        }
    }
    private void shell(String casing, String window) {
        for (int x = 0; x < size.width(); x++) {
            for (int y = 0; y < size.height(); y++) {
                for (int z = 0; z < size.depth(); z++) {
                    int walls = (edge(x, size.width()) ? 1 : 0)
                            + (edge(y, size.height()) ? 1 : 0) + (edge(z, size.depth()) ? 1 : 0);
                    if (walls > 0) put(x, y, z, walls >= 2 || !glass ? casing : window, false);
                }
            }
        }
    }
    private void ports(String id) {
        put(1, 1, 0, id, "north", false);
        put(size.width() - 2, 1, size.depth() - 1, id, "south", false);
    }
    private void evaporation() {
        shell("mekanism:thermal_evaporation_block", "mekanism:thermal_evaporation_block");
        int top = size.height() - 1;
        blocks.keySet().removeIf(p -> p.y() == top && (
                (p.x() > 0 && p.x() < 3 && p.z() > 0 && p.z() < 3)
                        || (edge(p.x(), 4) && edge(p.z(), 4))));
        put(1, 1, 0, "mekanism:thermal_evaporation_controller", "north", false);
        put(2, 1, 0, "mekanism:thermal_evaporation_valve", "north", false);
        put(1, 1, 3, "mekanism:thermal_evaporation_valve", "south", false);
    }
    private void roundedSps() {
        int[][] grid = {
                {0,0,1,1,1,0,0}, {0,1,2,2,2,1,0}, {1,2,2,2,2,2,1},
                {1,2,2,2,2,2,1}, {1,2,2,2,2,2,1}, {0,1,2,2,2,1,0}, {0,0,1,1,1,0,0}
        };
        rounded(grid, "mekanism:sps_casing", "mekanism:structural_glass");
        put(3, 3, 0, "mekanism:sps_port", "north", false);
        put(3, 3, 1, "mekanism:supercharged_coil", "south", true);
        put(3, 3, 6, "mekanism:sps_port", "south", false);
        put(0, 3, 3, "mekanism:sps_port", "west", false);
    }
    private void fusion() {
        int[][] grid = {{0,0,1,0,0}, {0,1,2,1,0}, {1,2,2,2,1}, {0,1,2,1,0}, {0,0,1,0,0}};
        rounded(grid, "mekanismgenerators:fusion_reactor_frame", "mekanismgenerators:reactor_glass");
        put(2, 4, 2, "mekanismgenerators:fusion_reactor_controller", false);
        put(2, 2, 0, "mekanismgenerators:laser_focus_matrix", "north", false);
        put(0, 2, 2, "mekanismgenerators:fusion_reactor_port", "west", false);
        put(4, 2, 2, "mekanismgenerators:fusion_reactor_port", "east", false);
        put(2, 2, 4, "mekanismgenerators:fusion_reactor_port", "south", false);
    }
    private void rounded(int[][] grid, String casing, String window) {
        int n = grid.length;
        // Each face uses the same symmetric pattern; edges outside the pattern stay empty.
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                int rule = grid[a][b];
                if (rule == 0) continue;
                String id = rule == 1 || !glass ? casing : window;
                put(a, 0, b, id, false); put(a, n - 1, b, id, false);
                put(0, a, b, id, false); put(n - 1, a, b, id, false);
                put(a, b, 0, id, false); put(a, b, n - 1, id, false);
            }
        }
    }
    private void turbine() {
        String ns = "mekanismgenerators:";
        shell(ns + "turbine_casing", "mekanism:structural_glass");
        ports(ns + "turbine_valve");
        int cx = size.width() / 2, cz = size.depth() / 2;
        int radius = (Math.min(size.width(), size.depth()) - 3) / 2;
        int complexY = Math.min(size.height() - 3, Math.min(15, 4 * radius + 2));
        for (int y = 1; y < complexY; y++) put(cx, y, cz, ns + "turbine_rotor", true);
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                put(x, complexY, z, "mekanism:pressure_disperser", true);
            }
        }
        put(cx, complexY, cz, ns + "rotational_complex", true);
        plane(complexY + 1, ns + "saturating_condenser");
        put(cx, complexY + 1, cz, ns + "electromagnetic_coil", true);
        for (int x = 1; x < size.width() - 1; x++) {
            for (int z = 1; z < size.depth() - 1; z++) {
                put(x, size.height() - 1, z, ns + "turbine_vent", false);
            }
        }
        extras.put(ns + "turbine_blade", 2 * (complexY - 1));
    }
    private void plane(int y, String id) {
        for (int x = 1; x < size.width() - 1; x++) {
            for (int z = 1; z < size.depth() - 1; z++) put(x, y, z, id, true);
        }
    }
    private void put(int x, int y, int z, String id, boolean inner) { put(x, y, z, id, "", inner); }
    private void put(int x, int y, int z, String id, String facing, boolean inner) {
        blocks.put(new Cell(x, y, z), new Part(id, facing, inner));
    }
    private static boolean edge(int n, int length) { return n == 0 || n == length - 1; }
}
