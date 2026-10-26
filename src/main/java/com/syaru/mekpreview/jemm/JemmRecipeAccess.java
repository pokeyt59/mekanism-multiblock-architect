package com.syaru.mekpreview.jemm;

import giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget;

/** Optional recipe-viewer bridge. No hard reference to TMRV classes. */
public interface JemmRecipeAccess {
    MultiblockWidget mekpreview$model();
    default String mekpreview$title() { return null; }
    default Object mekpreview$source() { return null; }
}
