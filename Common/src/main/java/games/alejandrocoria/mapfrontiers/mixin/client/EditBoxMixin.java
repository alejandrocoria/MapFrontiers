package games.alejandrocoria.mapfrontiers.mixin.client;

import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), require = 0)
    private void mapfrontiers$fitBorderToWidgetBounds(GuiGraphics graphics, int minX, int minY, int maxX, int maxY, int color) {
        if (!mapfrontiers$isMapFrontiersTextBox()) {
            graphics.fill(minX, minY, maxX, maxY, color);
            return;
        }

        EditBox editBox = (EditBox) (Object) this;
        graphics.fill(editBox.getX(), editBox.getY(), editBox.getX() + editBox.getWidth(), editBox.getY() + editBox.getHeight(),
                color);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), require = 0)
    private void mapfrontiers$fitBackgroundInsideBorder(GuiGraphics graphics, int minX, int minY, int maxX, int maxY, int color) {
        if (!mapfrontiers$isMapFrontiersTextBox()) {
            graphics.fill(minX, minY, maxX, maxY, color);
            return;
        }

        EditBox editBox = (EditBox) (Object) this;
        graphics.fill(editBox.getX() + 1, editBox.getY() + 1, editBox.getX() + editBox.getWidth() - 1,
                editBox.getY() + editBox.getHeight() - 1, color);
    }

    @ModifyVariable(method = "renderWidget", at = @At("STORE"), index = 12, require = 0)
    private int mapfrontiers$moveTextDown(int textY) {
        if (mapfrontiers$isMapFrontiersTextBox()) {
            return textY + 1;
        }

        return textY;
    }

    private boolean mapfrontiers$isMapFrontiersTextBox() {
        Object editBox = this;
        return editBox instanceof TextBox || editBox instanceof TextBoxInt || editBox instanceof TextBoxDouble;
    }
}
