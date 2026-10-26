package com.syaru.mekpreview.structure;
import com.syaru.mekpreview.test.fixture.Blueprints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.HashSet;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class BlueprintTest {
    @Test void authoritativeCostsAreNotRecountedFromGeometry() {
        var costs = new java.util.TreeMap<String, Integer>();
        costs.put("mekanismgenerators:turbine_blade", 6);
        costs.put("mekanismgenerators:turbine_rotor", 3);
        Blueprint b = new Blueprint(StructureKind.TURBINE, new Dimensions(5, 5, 5),
                java.util.Map.of(new Cell(2, 1, 2), new Part("mekanismgenerators:turbine_rotor", true)),
                java.util.Map.of(), costs);
        costs.clear();
        assertEquals(3, b.materials().get("mekanismgenerators:turbine_rotor"));
        assertEquals(6, b.materials().get("mekanismgenerators:turbine_blade"));
        assertThrows(UnsupportedOperationException.class, () -> b.materials().clear());
    }
    @Test void capturedStatePropertiesCannotBeMutatedThroughSourceMap() {
        var properties = new java.util.HashMap<String, String>();
        properties.put("facing", "west");
        Part part = new Part("mekanism:supercharged_coil", "", true, properties);
        properties.put("facing", "east");
        assertEquals("west", part.properties().get("facing"));
        assertThrows(UnsupportedOperationException.class, () -> part.properties().clear());
    }
    @Test void frozenBlueprintCannotBeMutatedThroughSourceGeometry() {
        var blocks = new java.util.LinkedHashMap<Cell, Part>();
        blocks.put(new Cell(1, 1, 1), new Part("mekanism:dynamic_tank", true));
        Blueprint b = new Blueprint(StructureKind.DYNAMIC_TANK, new Dimensions(3, 3, 3), blocks, java.util.Map.of());
        blocks.clear();
        assertEquals(1, b.blocks().size());
        assertThrows(UnsupportedOperationException.class, () -> b.blocks().clear());
    }
    @ParameterizedTest @EnumSource(StructureKind.class)
    void allSizeExtremesStayInBounds(StructureKind kind) {
        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            Dimensions requested = i == 0 ? new Dimensions(-999, -999, -999)
                    : i == 1 ? new Dimensions(999, 999, 999)
                    : new Dimensions(random.nextInt(25), random.nextInt(25), random.nextInt(25));
            Blueprint b = Blueprints.create(kind, requested, i % 2 == 0);
            assertFalse(b.blocks().isEmpty());
            assertTrue(b.blocks().keySet().stream().allMatch(b.dimensions()::contains));
            assertEquals(b.blocks().size() + b.extraMaterials().values().stream().mapToInt(Integer::intValue).sum(),
                    b.materials().values().stream().mapToInt(Integer::intValue).sum());
        }
    }
    @ParameterizedTest @EnumSource(StructureKind.class)
    void layerSlicesReconstructEntireStructure(StructureKind kind) {
        Blueprint b = Blueprints.create(kind, kind.defaults(), true);
        var union = new HashSet<Cell>();
        for (int y = 0; y < b.dimensions().height(); y++) {
            var slice = b.visible(LayerMode.SINGLE, y, false);
            final int layer = y;
            assertTrue(slice.keySet().stream().allMatch(p -> p.y() == layer));
            assertEquals(slice.size(), slice.keySet().stream().filter(p -> !union.contains(p)).count());
            union.addAll(slice.keySet());
        }
        assertEquals(b.blocks().keySet(), union);
        assertTrue(b.visible(LayerMode.SINGLE, 1, true).values().stream().allMatch(Part::interior));
    }
    @ParameterizedTest @EnumSource(StructureKind.class)
    void rotationsNeverCollideOrEscapeBounds(StructureKind kind) {
        Blueprint b = Blueprints.create(kind, new Dimensions(7, 8, 11), true);
        for (int rotation = 0; rotation < 4; rotation++) {
            int r = rotation;
            var positions = b.blocks().keySet().stream()
                    .map(p -> p.rotate(b.dimensions().width(), b.dimensions().depth(), r)).toList();
            assertEquals(positions.size(), new HashSet<>(positions).size());
            Dimensions rotated = rotation % 2 == 0 ? b.dimensions()
                    : new Dimensions(b.dimensions().depth(), b.dimensions().height(), b.dimensions().width());
            assertTrue(positions.stream().allMatch(rotated::contains));
        }
    }
    @Test void fixedEquipmentCannotBeResized() {
        assertEquals(new Dimensions(5, 5, 5), StructureKind.FUSION_REACTOR.normalize(new Dimensions(100, 1, 20)));
        assertEquals(new Dimensions(7, 7, 7), StructureKind.SPS.normalize(new Dimensions(1, 100, 1)));
        assertEquals(new Dimensions(4, 18, 4), StructureKind.EVAPORATION.normalize(new Dimensions(9, 100, 9)));
    }
    @Test void boilerAlwaysHasUnbrokenDisperserPlaneAboveHeatingElement() {
        Blueprint b = Blueprints.create(StructureKind.BOILER, new Dimensions(7, 9, 5), true);
        assertEquals(15, b.materials().get("mekanism:pressure_disperser"));
        b.blocks().forEach((pos, part) -> {
            if (part.blockId().equals("mekanism:pressure_disperser")) assertEquals(6, pos.y());
            if (part.blockId().equals("mekanism:superheating_element")) assertTrue(pos.y() < 6);
        });
    }
    @Test void fissionFuelColumnsHaveControlRodsImmediatelyAbove() {
        Blueprint b = Blueprints.create(StructureKind.FISSION_REACTOR, new Dimensions(8, 12, 5), true);
        b.blocks().forEach((pos, part) -> {
            if (part.blockId().endsWith("control_rod_assembly")) {
                assertEquals(10, pos.y());
                assertTrue(b.blocks().get(new Cell(pos.x(), pos.y() - 1, pos.z())).blockId().endsWith("fission_fuel_assembly"));
            }
        });
    }
    @Test void turbineConstrainsRotorHeightAndIncludesInstalledBlades() {
        for (int w = 5; w <= 17; w += 2) {
            for (int h = 5; h <= 18; h++) {
                Blueprint b = Blueprints.create(StructureKind.TURBINE, new Dimensions(w, h, 17), true);
                int rotors = b.materials().get("mekanismgenerators:turbine_rotor");
                assertEquals(2 * rotors, b.extraMaterials().get("mekanismgenerators:turbine_blade"));
                assertTrue(rotors <= 14);
                int complexY = b.blocks().entrySet().stream()
                        .filter(e -> e.getValue().blockId().endsWith("rotational_complex"))
                        .findFirst().orElseThrow().getKey().y();
                assertTrue((w - 3) / 2 >= (complexY + 1) / 4);
            }
        }
    }
    @Test void spsCoilFacesItsPortAndFusionHasOneTopController() {
        Blueprint sps = Blueprints.create(StructureKind.SPS, StructureKind.SPS.defaults(), true);
        assertEquals("mekanism:supercharged_coil", sps.blocks().get(new Cell(3, 3, 1)).blockId());
        assertEquals("mekanism:sps_port", sps.blocks().get(new Cell(3, 3, 0)).blockId());
        Blueprint fusion = Blueprints.create(StructureKind.FUSION_REACTOR, StructureKind.FUSION_REACTOR.defaults(), false);
        assertEquals(1, fusion.materials().get("mekanismgenerators:fusion_reactor_controller"));
        assertTrue(fusion.blocks().get(new Cell(2, 4, 2)).blockId().endsWith("fusion_reactor_controller"));
        assertFalse(fusion.blocks().containsKey(new Cell(0, 0, 0)));
    }
}
