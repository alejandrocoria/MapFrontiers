package games.alejandrocoria.mapfrontiers.mixin.client;

import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsMixin {
    @ModifyArg(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor$ScissorStack;containsPoint(II)Z"), index = 0)
    private int mapfrontiers$scaleScissorX(int x) {
        return mapfrontiers$scaleScissorCoordinate(x);
    }

    @ModifyArg(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor$ScissorStack;containsPoint(II)Z"), index = 1)
    private int mapfrontiers$scaleScissorY(int y) {
        return mapfrontiers$scaleScissorCoordinate(y);
    }

    private static int mapfrontiers$scaleScissorCoordinate(int coordinate) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AutoScaledScreen autoScaledScreen && autoScaledScreen.getScaleFactor() != 1.f) {
            return (int) Math.floor(coordinate / autoScaledScreen.getScaleFactor());
        }

        return coordinate;
    }
}
