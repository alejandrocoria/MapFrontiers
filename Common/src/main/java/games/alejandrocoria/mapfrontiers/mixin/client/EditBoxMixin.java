package games.alejandrocoria.mapfrontiers.mixin.client;

import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @Shadow
    private int textY;

    @Inject(method = "updateTextPosition", at = @At("TAIL"))
    private void mapfrontiers$moveTextDown(CallbackInfo ci) {
        Object editBox = this;
        if (editBox instanceof TextBox || editBox instanceof TextBoxInt || editBox instanceof TextBoxDouble) {
            ++textY;
        }
    }
}
