package games.alejandrocoria.mapfrontiers.mixin.client;

import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @ModifyArgs(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), require = 0)
    private void mapfrontiers$fitBorderToWidgetBounds(Args args) {
        if (!mapfrontiers$isMapFrontiersTextBox()) {
            return;
        }

        EditBox editBox = (EditBox) (Object) this;
        args.set(0, editBox.getX());
        args.set(1, editBox.getY());
        args.set(2, editBox.getX() + editBox.getWidth());
        args.set(3, editBox.getY() + editBox.getHeight());
    }

    @ModifyArgs(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), require = 0)
    private void mapfrontiers$fitBackgroundInsideBorder(Args args) {
        if (!mapfrontiers$isMapFrontiersTextBox()) {
            return;
        }

        EditBox editBox = (EditBox) (Object) this;
        args.set(0, editBox.getX() + 1);
        args.set(1, editBox.getY() + 1);
        args.set(2, editBox.getX() + editBox.getWidth() - 1);
        args.set(3, editBox.getY() + editBox.getHeight() - 1);
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
