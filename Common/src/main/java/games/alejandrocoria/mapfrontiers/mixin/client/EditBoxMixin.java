package games.alejandrocoria.mapfrontiers.mixin.client;

import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @ModifyVariable(method = "renderWidget", at = @At("STORE"), index = 11)
    private int mapfrontiers$moveTextDown(int textY) {
        Object editBox = this;
        if (editBox instanceof TextBox || editBox instanceof TextBoxInt || editBox instanceof TextBoxDouble) {
            return textY + 1;
        }

        return textY;
    }
}
