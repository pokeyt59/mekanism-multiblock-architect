package com.syaru.mekpreview.mixin;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** MTE/Compat still call a tooltip helper removed in JEMM 4.27. Preserve its message content. */
@Pseudo
@Mixin(targets = {
        "morethermalevaporation.client.jei.category.MoreEvaporationPlantCategory$MoreEvaporationPlantWidget",
        "io.github.masyumero.morethermalevaporationcompat.client.jei.category.MoreEvaporationPlantCompatCategory$MoreEvaporationPlantCompatWidget"
}, remap = false)
public abstract class MteTooltipMixin {
    @Unique public int getCornerBlocks() {
        return ((giselle.jei_mekanism_multiblocks.client.jei.MultiblockWidget) (Object) this).getEdgeBlocks();
    }
    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lgiselle/jei_mekanism_multiblocks/client/TooltipHelper;createMessageOnly([Lnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/components/Tooltip;"), require = 0)
    private Tooltip mekpreview$tooltip(Component[] messages) {
        var text = Component.empty();
        for (int i = 0; i < messages.length; i++) { if (i > 0) text.append("\n"); text.append(messages[i]); }
        return Tooltip.create(text);
    }
}
