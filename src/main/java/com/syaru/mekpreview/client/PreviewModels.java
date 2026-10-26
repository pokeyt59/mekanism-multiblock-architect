package com.syaru.mekpreview.client;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.syaru.mekpreview.structure.Part;
import mekanism.api.NBTConstants;

final class PreviewModels {
    private PreviewModels() {}
    static BlockInfo blockInfo(Part part, int rotation, int y) {
        boolean needsDisplayTile = switch (part.blockId()) {
            case "mekanismgenerators:turbine_rotor", "mekanismgenerators:fission_fuel_assembly",
                    "mekanismgenerators:control_rod_assembly", "mekanism:supercharged_coil" -> true;
            default -> false;
        };
        // Casing renderers expect formed machine contents; only render physical part tiles.
        if (!needsDisplayTile) return new BlockInfo(BlockResolver.state(part, rotation), false);
        return new BlockInfo(BlockResolver.state(part, rotation), tile -> {
            if (part.blockId().equals("mekanismgenerators:turbine_rotor")) {
                // Initialize only the detached display tile; no world interaction or ticking.
                var tag = tile.saveWithoutMetadata();
                tag.putInt(NBTConstants.BLADES, 2);
                tag.putInt(NBTConstants.POSITION, y - 1);
                tile.load(tag);
            }
        });
    }
}
