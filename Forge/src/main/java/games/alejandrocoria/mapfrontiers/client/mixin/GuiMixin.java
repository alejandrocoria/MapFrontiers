package games.alejandrocoria.mapfrontiers.client.mixin;

import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 1600)
public class GuiMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("TAIL"))
    public void renderPost(GuiGraphics graphics, float partialTicks, CallbackInfo callbackInfo) {
        ClientEventHandler.postHudRenderEvent(graphics, partialTicks);
    }
}
