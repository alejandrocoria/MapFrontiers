package games.alejandrocoria.mapfrontiers.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Redirect(method = "applyScissor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableScissor(IIII)V"))
    private void mapfrontiers$enableAutoScaledScissor(int x, int y, int width, int height) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AutoScaledScreen autoScaledScreen) || autoScaledScreen.getScaleFactor() == 1.f) {
            RenderSystem.enableScissor(x, y, width, height);
            return;
        }

        float scaleFactor = autoScaledScreen.getScaleFactor();
        int framebufferHeight = Minecraft.getInstance().getWindow().getHeight();
        int left = (int) Math.floor(x / scaleFactor);
        int right = (int) Math.ceil((x + width) / scaleFactor);
        int top = (int) Math.floor((framebufferHeight - y - height) / scaleFactor);
        int bottom = (int) Math.ceil((framebufferHeight - y) / scaleFactor);

        RenderSystem.enableScissor(left, framebufferHeight - bottom,
                Math.max(0, right - left), Math.max(0, bottom - top));
    }
}
