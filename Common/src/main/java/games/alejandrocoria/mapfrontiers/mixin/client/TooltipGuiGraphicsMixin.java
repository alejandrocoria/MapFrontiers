package games.alejandrocoria.mapfrontiers.mixin.client;

import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiGraphicsExtractor.class)
public class TooltipGuiGraphicsMixin {
    @ModifyArg(method = "tooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 0)
    private int mapfrontiers$scaleTooltipScreenWidth(int width) {
        return mapfrontiers$scaleTooltipScreenDimension(width);
    }

    @ModifyArg(method = "tooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 1)
    private int mapfrontiers$scaleTooltipScreenHeight(int height) {
        return mapfrontiers$scaleTooltipScreenDimension(height);
    }

    private static int mapfrontiers$scaleTooltipScreenDimension(int dimension) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen instanceof AutoScaledScreen autoScaledScreen && autoScaledScreen.getScaleFactor() != 1.f) {
            return (int) (dimension * autoScaledScreen.getScaleFactor());
        }
        return dimension;
    }
}
