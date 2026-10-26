package com.syaru.mekpreview.mixin;

import com.lowdragmc.lowdraglib.emi.ModularWrapperWidget;
import com.syaru.mekpreview.client.design.*;
import com.syaru.mekpreview.jemm.JemmPreviewWidget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** LDLib 1.0.50 forwards keys but not typed characters from EMI recipes. */
@Mixin(value = RecipeScreen.class, remap = false, priority = 1100)
public abstract class RecipeScreenInputMixin implements DesignInputScreen {
    @Shadow private List<WidgetGroup> currentPage;
    @Override public SidePanel.Input mekpreview$focusedInput() {
        if (dev.emi.emi.api.EmiApi.isSearchFocused()) return null;
        for (var group : currentPage) for (var widget : group.widgets)
            if (widget instanceof ModularWrapperWidget wrapper && wrapper.modular.getWidget() instanceof JemmPreviewWidget editor) {
                var input = editor.focusedInput();
                if (input != null) return input;
            }
        return null;
    }
    // Explicit 1.20.1 development / production names: the project has no refmap processor.
    @Inject(method = {"charTyped(CI)Z", "m_5534_(CI)Z"}, at = @At("HEAD"), cancellable = true)
    private void mekpreview$type(char character, int modifiers, CallbackInfoReturnable<Boolean> result) {
        var input = mekpreview$focusedInput();
        if (input != null) result.setReturnValue(input.charTyped(character, modifiers));
    }
    @Inject(method = {"keyPressed(III)Z", "m_7933_(III)Z"}, at = @At("HEAD"), cancellable = true)
    private void mekpreview$key(int key, int scan, int modifiers, CallbackInfoReturnable<Boolean> result) {
        var input = mekpreview$focusedInput();
        if (input == null) return;
        if (key == 256) { input.setFocus(false); return; }
        input.keyPressed(key, scan, modifiers);
        result.setReturnValue(true); // Do not trigger EMI/inventory shortcuts while typing.
    }
}
