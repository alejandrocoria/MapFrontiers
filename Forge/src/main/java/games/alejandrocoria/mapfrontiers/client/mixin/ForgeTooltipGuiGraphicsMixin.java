package games.alejandrocoria.mapfrontiers.client.mixin;

import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphicsExtractor.class)
public class ForgeTooltipGuiGraphicsMixin {
    @ModifyVariable(method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ClientTooltipPositioner mapfrontiers$scaleTooltipScreenDimensions(ClientTooltipPositioner positioner) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AutoScaledScreen autoScaledScreen) || autoScaledScreen.getScaleFactor() == 1.f) {
            return positioner;
        }

        float scaleFactor = autoScaledScreen.getScaleFactor();
        return (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) -> {
            Vector2ic position = positioner.positionTooltip(
                    (int) (screenWidth * scaleFactor),
                    (int) (screenHeight * scaleFactor),
                    x,
                    y,
                    tooltipWidth,
                    tooltipHeight
            );
            return position;
        };
    }
}
