package games.alejandrocoria.mapfrontiers.mixin.client;

import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @ModifyArg(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics$ScissorStack;containsPoint(II)Z"), index = 0)
    private int mapfrontiers$scaleScissorX(int x) {
        return mapfrontiers$scaleScissorCoordinate(x);
    }

    @ModifyArg(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics$ScissorStack;containsPoint(II)Z"), index = 1)
    private int mapfrontiers$scaleScissorY(int y) {
        return mapfrontiers$scaleScissorCoordinate(y);
    }

    @ModifyArg(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 0)
    private int mapfrontiers$scaleTooltipScreenWidth(int width) {
        return mapfrontiers$scaleTooltipScreenDimension(width);
    }

    @ModifyArg(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 1)
    private int mapfrontiers$scaleTooltipScreenHeight(int height) {
        return mapfrontiers$scaleTooltipScreenDimension(height);
    }

    private static int mapfrontiers$scaleScissorCoordinate(int coordinate) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AutoScaledScreen autoScaledScreen && autoScaledScreen.getScaleFactor() != 1.f) {
            return (int) Math.floor(coordinate / autoScaledScreen.getScaleFactor());
        }

        return coordinate;
    }

    private static int mapfrontiers$scaleTooltipScreenDimension(int dimension) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AutoScaledScreen autoScaledScreen && autoScaledScreen.getScaleFactor() != 1.f) {
            return (int) (dimension * autoScaledScreen.getScaleFactor());
        }

        return dimension;
    }
}
